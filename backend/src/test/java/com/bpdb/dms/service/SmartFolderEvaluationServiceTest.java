package com.bpdb.dms.service;

import com.bpdb.dms.entity.DocumentIndex;
import com.bpdb.dms.entity.SmartFolderDefinition;
import com.bpdb.dms.entity.SmartFolderScope;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentIndexRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

class SmartFolderEvaluationServiceTest {

    @Test
    void evaluate_appliesDepartmentScopeForNonAdmin() {
        DocumentIndexRepository repo = Mockito.mock(DocumentIndexRepository.class);
        SmartFolderEvaluationService svc = new SmartFolderEvaluationService();
        inject(svc, "documentIndexRepository", repo);

        DocumentIndex d1 = new DocumentIndex();
        setField(d1, "department", "A");
        DocumentIndex d2 = new DocumentIndex();
        setField(d2, "department", "B");
        Page<DocumentIndex> page = new PageImpl<>(List.of(d1, d2), PageRequest.of(0, 10), 2);
        Mockito.when(repo.findAll(Mockito.any())).thenReturn(page);

        SmartFolderDefinition def = new SmartFolderDefinition();
        def.setIsActive(true);
        def.setScope(SmartFolderScope.DEPARTMENT);
        def.setDefinition("{}");

        User user = new User();
        user.setDepartment("A");

        Page<DocumentIndex> result = svc.evaluate(def, user, PageRequest.of(0, 10));
        Assertions.assertEquals(1, result.getContent().size());
    }

    private static void inject(Object target, String field, Object value) {
        try {
            var f = SmartFolderEvaluationService.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            // ignore for test simplicity
        }
    }
}


