package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for stationery tracking per employee
 */
@Service
@Transactional
public class StationeryTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StationeryTrackingService.class);
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Assign a stationery record to an employee
     */
    public Document assignStationeryToEmployee(Long documentId, Long employeeId, User assignedBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        if (!"STATIONERY_RECORD".equals(document.getDocumentType())) {
            throw new RuntimeException("Document is not a stationery record");
        }
        
        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        
        document.setAssignedEmployee(employee);
        Document saved = documentRepository.save(document);
        
        auditService.logActivity(
            assignedBy.getUsername(),
            "STATIONERY_ASSIGNED",
            "Stationery record assigned to employee: " + employee.getUsername(),
            documentId
        );
        
        logger.info("Stationery record {} assigned to employee {} by user {}", 
                   document.getOriginalName(), employee.getUsername(), assignedBy.getUsername());
        return saved;
    }
    
    /**
     * Unassign a stationery record from an employee
     */
    public Document unassignStationeryFromEmployee(Long documentId, User unassignedBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        if (!"STATIONERY_RECORD".equals(document.getDocumentType())) {
            throw new RuntimeException("Document is not a stationery record");
        }
        
        String previousEmployee = document.getAssignedEmployee() != null 
            ? document.getAssignedEmployee().getUsername() 
            : "None";
        
        document.setAssignedEmployee(null);
        Document saved = documentRepository.save(document);
        
        auditService.logActivity(
            unassignedBy.getUsername(),
            "STATIONERY_UNASSIGNED",
            "Stationery record unassigned from employee: " + previousEmployee,
            documentId
        );
        
        logger.info("Stationery record {} unassigned from employee {} by user {}", 
                   document.getOriginalName(), previousEmployee, unassignedBy.getUsername());
        return saved;
    }
    
    /**
     * Get stationery records assigned to an employee
     */
    public List<Document> getStationeryRecordsByEmployee(Long employeeId) {
        return documentRepository.findStationeryRecordsByEmployee(employeeId);
    }
    
    /**
     * Get all stationery records
     */
    public Page<Document> getAllStationeryRecords(Pageable pageable) {
        return documentRepository.findStationeryRecords(pageable);
    }
    
    /**
     * Get stationery statistics per employee
     */
    public List<EmployeeStationeryStats> getStationeryStatisticsPerEmployee() {
        List<Object[]> results = documentRepository.countStationeryRecordsPerEmployee();
        
        return results.stream()
            .map(result -> {
                Long employeeId = (Long) result[0];
                Long count = (Long) result[1];
                
                User employee = userRepository.findById(employeeId).orElse(null);
                if (employee == null) {
                    return null;
                }
                
                List<Document> records = documentRepository.findStationeryRecordsByEmployee(employeeId);
                
                return new EmployeeStationeryStats(
                    employeeId,
                    employee.getUsername(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmail(),
                    count.intValue(),
                    records
                );
            })
            .filter(stats -> stats != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Get stationery statistics summary
     */
    public StationeryStatisticsSummary getStationeryStatisticsSummary() {
        Page<Document> allStationery = documentRepository.findStationeryRecords(
            org.springframework.data.domain.PageRequest.of(0, 1)
        );
        
        long totalStationeryRecords = allStationery.getTotalElements();
        long assignedRecords = documentRepository.countStationeryRecordsPerEmployee().stream()
            .mapToLong(result -> (Long) result[1])
            .sum();
        long unassignedRecords = totalStationeryRecords - assignedRecords;
        
        long employeesWithStationery = documentRepository.countStationeryRecordsPerEmployee().size();
        
        return new StationeryStatisticsSummary(
            totalStationeryRecords,
            assignedRecords,
            unassignedRecords,
            employeesWithStationery
        );
    }
    
    /**
     * Employee stationery statistics
     */
    public static class EmployeeStationeryStats {
        private final Long employeeId;
        private final String username;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final int stationeryCount;
        private final List<Document> stationeryRecords;
        
        public EmployeeStationeryStats(Long employeeId, String username, String firstName, 
                                      String lastName, String email, int stationeryCount,
                                      List<Document> stationeryRecords) {
            this.employeeId = employeeId;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.stationeryCount = stationeryCount;
            this.stationeryRecords = stationeryRecords;
        }
        
        public Long getEmployeeId() { return employeeId; }
        public String getUsername() { return username; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public int getStationeryCount() { return stationeryCount; }
        public List<Document> getStationeryRecords() { return stationeryRecords; }
    }
    
    /**
     * Stationery statistics summary
     */
    public static class StationeryStatisticsSummary {
        private final long totalStationeryRecords;
        private final long assignedRecords;
        private final long unassignedRecords;
        private final long employeesWithStationery;
        
        public StationeryStatisticsSummary(long totalStationeryRecords, long assignedRecords,
                                          long unassignedRecords, long employeesWithStationery) {
            this.totalStationeryRecords = totalStationeryRecords;
            this.assignedRecords = assignedRecords;
            this.unassignedRecords = unassignedRecords;
            this.employeesWithStationery = employeesWithStationery;
        }
        
        public long getTotalStationeryRecords() { return totalStationeryRecords; }
        public long getAssignedRecords() { return assignedRecords; }
        public long getUnassignedRecords() { return unassignedRecords; }
        public long getEmployeesWithStationery() { return employeesWithStationery; }
    }
}

