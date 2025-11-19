package com.bpdb.dms.integration;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Stationery Tracking per Employee feature
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class StationeryTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User employeeUser;
    private Document stationeryRecord;

    @BeforeEach
    void setUp() {
        // Create test user (admin/officer)
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Create employee user
        employeeUser = new User();
        employeeUser.setUsername("employee");
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPassword("password");
        employeeUser.setIsActive(true);
        employeeUser = userRepository.save(employeeUser);

        // Create stationery record
        stationeryRecord = new Document();
        stationeryRecord.setFileName("stationery.pdf");
        stationeryRecord.setFilePath("/uploads/stationery.pdf");
        stationeryRecord.setDocumentType("STATIONERY_RECORD");
        stationeryRecord.setUploadedBy(testUser);
        stationeryRecord.setIsActive(true);
        stationeryRecord = documentRepository.save(stationeryRecord);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void assignStationeryToEmployee_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/documents/" + stationeryRecord.getId() + "/assign-stationery")
                .param("employeeId", employeeUser.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedEmployee.id").value(employeeUser.getId()));

        // Verify assignment
        var assignedDocument = documentRepository.findById(stationeryRecord.getId());
        assertTrue(assignedDocument.isPresent());
        assertNotNull(assignedDocument.get().getAssignedEmployee());
        assertEquals(employeeUser.getId(), assignedDocument.get().getAssignedEmployee().getId());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "OFFICER")
    void unassignStationeryFromEmployee_IntegrationTest() throws Exception {
        // Given - Assign stationery first
        stationeryRecord.setAssignedEmployee(employeeUser);
        documentRepository.save(stationeryRecord);

        // When & Then
        mockMvc.perform(post("/api/documents/" + stationeryRecord.getId() + "/unassign-stationery")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedEmployee").doesNotExist());

        // Verify unassignment
        var unassignedDocument = documentRepository.findById(stationeryRecord.getId());
        assertTrue(unassignedDocument.isPresent());
        assertNull(unassignedDocument.get().getAssignedEmployee());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getStationeryRecords_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/stationery")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getStationeryRecordsByEmployee_IntegrationTest() throws Exception {
        // Given - Assign stationery to employee
        stationeryRecord.setAssignedEmployee(employeeUser);
        documentRepository.save(stationeryRecord);

        // When & Then
        mockMvc.perform(get("/api/documents/stationery/employee/" + employeeUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].documentType").value("STATIONERY_RECORD"))
                .andExpect(jsonPath("$[0].assignedEmployee.id").value(employeeUser.getId()));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getStationeryStatistics_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/stationery/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStationeryRecords").exists())
                .andExpect(jsonPath("$.assignedRecords").exists())
                .andExpect(jsonPath("$.unassignedRecords").exists())
                .andExpect(jsonPath("$.employeesWithStationery").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VIEWER")
    void getStationeryStatisticsPerEmployee_IntegrationTest() throws Exception {
        // Given - Assign stationery to employee
        stationeryRecord.setAssignedEmployee(employeeUser);
        documentRepository.save(stationeryRecord);

        // When & Then
        mockMvc.perform(get("/api/documents/stationery/statistics/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employeeId").value(employeeUser.getId()))
                .andExpect(jsonPath("$[0].stationeryCount").exists());
    }
}

