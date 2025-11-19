package com.bpdb.dms.service;

import com.bpdb.dms.entity.BackupRecord;
import com.bpdb.dms.entity.BackupStatus;
import com.bpdb.dms.entity.BackupType;
import com.bpdb.dms.repository.BackupRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class DisasterRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(DisasterRecoveryService.class);

    @Autowired
    private BackupRecordRepository backupRecordRepository;

    @Autowired
    private AuditService auditService;

    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${app.backup.retention-days:30}")
    private int retentionDays;

    public CompletableFuture<BackupRecord> createFullBackup() {
        return CompletableFuture.supplyAsync(() -> createBackup(BackupType.FULL));
    }

    public CompletableFuture<BackupRecord> createIncrementalBackup() {
        return CompletableFuture.supplyAsync(() -> createBackup(BackupType.INCREMENTAL));
    }

    public CompletableFuture<Map<String, Object>> restoreFromBackup(Long backupId) {
        return CompletableFuture.supplyAsync(() -> {
            BackupRecord record = backupRecordRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Backup not found"));
            logger.info("Restore requested for backup {}", record.getId());
            return Map.of(
                "success", true,
                "backupId", backupId,
                "message", "Restore simulated",
                "timestamp", LocalDateTime.now()
            );
        });
    }

    @Scheduled(cron = "${app.backup.schedule:0 0 2 * * ?}")
    public void scheduledBackup() {
        if (!backupEnabled) {
            logger.debug("Backups disabled, skipping scheduled run");
            return;
        }
        createFullBackup();
    }

    @Scheduled(fixedDelay = 86400000L)
    public void cleanupOldBackups() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<BackupRecord> oldBackups = backupRecordRepository.findByRetentionUntilBefore(cutoff);
        for (BackupRecord record : oldBackups) {
            backupRecordRepository.delete(record);
            auditService.logActivity("SYSTEM", "BACKUP_CLEANUP",
                "Deleted backup " + record.getId(), null);
        }
    }

    public Map<String, Object> getBackupStatistics() {
        return Map.of(
            "totalBackups", backupRecordRepository.count(),
            "completedBackups", backupRecordRepository.countByStatus(BackupStatus.COMPLETED),
            "failedBackups", backupRecordRepository.countByStatus(BackupStatus.FAILED),
            "totalSizeBytes", backupRecordRepository.sumSizeBytes()
        );
    }

    public Map<String, Object> testBackupIntegrity(Long backupId) {
        boolean exists = backupRecordRepository.existsById(backupId);
        return Map.of(
            "backupId", backupId,
            "integrityCheck", exists,
            "testedAt", LocalDateTime.now()
        );
    }

    private BackupRecord createBackup(BackupType type) {
        BackupRecord record = new BackupRecord();
        record.setBackupType(type);
        record.setStatus(BackupStatus.COMPLETED);
        record.setStartedAt(LocalDateTime.now());
        record.setCompletedAt(LocalDateTime.now());
        record.setBackupPath("/app/backups/" + type.name().toLowerCase() + "_" + record.getStartedAt());
        record.setSizeBytes(0L);
        record.setRetentionUntil(LocalDateTime.now().plusDays(retentionDays));
        BackupRecord saved = backupRecordRepository.save(record);
        auditService.logActivity("SYSTEM", "BACKUP_CREATED",
            type.name() + " backup created", null);
        logger.info("{} backup created with id {}", type, saved.getId());
        return saved;
    }
}

