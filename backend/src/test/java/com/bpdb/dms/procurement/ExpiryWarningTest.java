package com.bpdb.dms.procurement;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.ExpiryStatus;
import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.entity.ExpiryType;
import com.bpdb.dms.procurement.entity.ExpiryPolicy;
import com.bpdb.dms.procurement.repository.ExpiryPolicyRepository;
import com.bpdb.dms.procurement.service.ProcurementExpiryAlertService;
import com.bpdb.dms.repository.ExpiryTrackingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Expiry warnings (REQ-E2, REQ-E3).
 *
 * <p>Recording an expiry date achieves nothing on its own; the requirement is that someone
 * is told, at intervals the client can configure, and told once per interval rather than
 * every hour until the date passes.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpiryWarningTest {

    @Autowired
    private ProcurementExpiryAlertService alertService;

    @Autowired
    private ExpiryTrackingRepository expiryRepository;

    @Autowired
    private ExpiryPolicyRepository policyRepository;

    @Autowired
    private com.bpdb.dms.repository.DocumentRepository documentRepository;

    @Autowired
    private com.bpdb.dms.repository.UserRepository userRepository;

    @Autowired
    private com.bpdb.dms.repository.RoleRepository roleRepository;

    private static final String ENTITY = "LETTER_OF_CREDIT";

    private void policyOf(String warningDays) {
        ExpiryPolicy policy = policyRepository.findByEntityTypeAndIsActiveTrue(ENTITY)
                .orElseGet(ExpiryPolicy::new);
        policy.setEntityType(ENTITY);
        policy.setDocLabel("Letter of Credit (LC)");
        policy.setExpiryFieldLabel("LC Expiry Date");
        policy.setWarningDays(warningDays);
        policy.setNotifyResponsibleOfficer(Boolean.FALSE);
        policy.setNotifyRoles("ADMIN");
        policy.setIsActive(Boolean.TRUE);
        policyRepository.save(policy);
    }

    /**
     * A tracker always guards a document — expiry_tracking.document_id is not nullable,
     * because an expiry with nothing behind it is not something anyone can act on.
     */
    private ExpiryTracking trackerExpiringIn(long days) {
        ExpiryTracking tracker = new ExpiryTracking();
        tracker.setEntityType(ENTITY);
        tracker.setEntityId(1L);
        tracker.setDocument(guardedDocument());
        tracker.setExpiryType(ExpiryType.LETTER_OF_CREDIT);
        tracker.setExpiryDate(LocalDate.now().plusDays(days).atStartOfDay());
        tracker.setStatus(ExpiryStatus.ACTIVE);
        return expiryRepository.save(tracker);
    }

    private com.bpdb.dms.entity.Document guardedDocument() {
        com.bpdb.dms.entity.User owner = new com.bpdb.dms.entity.User();
        owner.setUsername("expiry-test-" + System.nanoTime());
        owner.setEmail(owner.getUsername() + "@example.com");
        owner.setPassword("password");
        owner.setIsActive(true);
        owner.setRole(com.bpdb.dms.support.TestRoles.officer(roleRepository));
        owner = userRepository.save(owner);

        com.bpdb.dms.entity.Document document = new com.bpdb.dms.entity.Document();
        document.setFileName("lc.pdf");
        document.setFilePath("uploads/lc.pdf");
        document.setFileSize(1024L);
        document.setDocumentType("OTHER");
        document.setUploadedBy(owner);
        document.setIsActive(true);
        return documentRepository.save(document);
    }

    @Test
    void nothingIsSentWhileTheDateIsStillFarOff() {
        policyOf("90,60,30,15,7");
        trackerExpiringIn(200);

        assertEquals(0, alertService.run(LocalDate.now()),
                "200 days out is beyond every configured threshold");
    }

    @Test
    void aWarningGoesOutOnceTheNearestThresholdIsReached() {
        policyOf("90,60,30,15,7");
        ExpiryTracking tracker = trackerExpiringIn(29);

        assertEquals(1, alertService.run(LocalDate.now()));
        assertTrue(expiryRepository.findById(tracker.getId()).orElseThrow()
                .getSentWarnings().contains("30"));
    }

    @Test
    void theSameThresholdIsNotSentTwice() {
        // The job runs hourly. A warning repeated every hour is a warning nobody reads.
        policyOf("90,60,30,15,7");
        trackerExpiringIn(29);

        assertEquals(1, alertService.run(LocalDate.now()));
        assertEquals(0, alertService.run(LocalDate.now()));
        assertEquals(0, alertService.run(LocalDate.now()));
    }

    @Test
    void crossingSeveralThresholdsAtOnceRaisesOneWarningNotFive() {
        // A tracker registered late is already inside every threshold; that is one thing
        // to tell somebody, not five
        policyOf("90,60,30,15,7");
        ExpiryTracking tracker = trackerExpiringIn(3);

        assertEquals(1, alertService.run(LocalDate.now()));

        String sent = expiryRepository.findById(tracker.getId()).orElseThrow().getSentWarnings();
        assertTrue(sent.contains("90") && sent.contains("7"),
                "the thresholds that were skipped must not fire later: " + sent);
        assertEquals(0, alertService.run(LocalDate.now()));
    }

    @Test
    void aFurtherWarningFollowsAsTheDateGetsCloser() {
        policyOf("90,30,7");
        ExpiryTracking tracker = trackerExpiringIn(29);
        assertEquals(1, alertService.run(LocalDate.now()));

        // six days later the 7-day threshold is reached and is worth saying again
        assertEquals(1, alertService.run(LocalDate.now().plusDays(23)));
        assertTrue(expiryRepository.findById(tracker.getId()).orElseThrow()
                .getSentWarnings().contains("7"));
    }

    @Test
    void anExpiredInstrumentIsReportedOnceMore() {
        policyOf("30");
        ExpiryTracking tracker = trackerExpiringIn(-1);

        assertEquals(1, alertService.run(LocalDate.now()));
        assertTrue(expiryRepository.findById(tracker.getId()).orElseThrow()
                .getSentWarnings().contains("expired"));
        assertEquals(0, alertService.run(LocalDate.now()), "but only once");
    }

    @Test
    void theIntervalsComeFromTheConfiguredPolicyNotFromCode() {
        // Q-15: the defaults are 90/60/30/15/7 but they must stay configurable
        policyOf("45");
        trackerExpiringIn(44);

        assertEquals(1, alertService.run(LocalDate.now()),
                "a policy of 45 days should fire at 44, which no hard-coded default covers");
    }

    @Test
    void aSupersededTrackerIsLeftAlone() {
        // An LC amendment supersedes the old tracker (REQ-E5); warning about a date that
        // has been replaced would be noise
        policyOf("90,30");
        ExpiryTracking tracker = trackerExpiringIn(10);
        tracker.setStatus(ExpiryStatus.RENEWED);
        tracker.setRenewalDate(LocalDateTime.now());
        expiryRepository.save(tracker);

        assertEquals(0, alertService.run(LocalDate.now()));
    }
}
