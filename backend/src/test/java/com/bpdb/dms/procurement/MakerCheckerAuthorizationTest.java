package com.bpdb.dms.procurement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Maker / Checker split from client answer Q-17 (REQ-X4), pinned at the route level.
 *
 * <p>These assert authorization only, not business outcomes: a Maker must be refused the
 * approval actions, and both roles must be able to read. The roles were seeded in
 * changeset 040 before the routes knew about them, which meant a Maker or Checker was
 * refused everything under /api/procurement - exactly the kind of gap that looks finished
 * from the database side, so it is worth a test rather than an inspection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MakerCheckerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    /** Not 403 - the request got past authorization, whatever it then did. */
    private static org.springframework.test.web.servlet.ResultMatcher notForbidden() {
        return result -> {
            int status = result.getResponse().getStatus();
            if (status == 403) {
                throw new AssertionError("Expected the request to pass authorization, but it was 403 Forbidden");
            }
        };
    }

    @Test
    @WithMockUser(username = "maker", authorities = {"ROLE_MAKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void makerMayReadPackages() throws Exception {
        mockMvc.perform(get("/api/procurement/packages"))
                .andExpect(notForbidden());
    }

    @Test
    @WithMockUser(username = "checker", authorities = {"ROLE_CHECKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE", "PERM_PROCUREMENT_VERIFY", "PERM_PROCUREMENT_OVERRIDE", "PERM_BUDGET_APPROVE"})
    void checkerMayReadPackages() throws Exception {
        mockMvc.perform(get("/api/procurement/packages"))
                .andExpect(notForbidden());
    }

    @Test
    @WithMockUser(username = "maker", authorities = {"ROLE_MAKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void makerMayNotCompleteAStage() throws Exception {
        // Completing a stage is approval: the Checker's job, not the Maker's
        mockMvc.perform(post("/api/procurement/packages/1/stages/1/complete")
                        .contentType("application/json").content("{}").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "maker", authorities = {"ROLE_MAKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void makerMayNotMarkAStageNotApplicable() throws Exception {
        mockMvc.perform(post("/api/procurement/packages/1/stages/9/not-applicable")
                        .contentType("application/json").content("{\"reason\":\"no LC\"}").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "maker", authorities = {"ROLE_MAKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void makerMayNotSendAStageBackForRework() throws Exception {
        mockMvc.perform(post("/api/procurement/packages/1/stages/2/rework")
                        .contentType("application/json").content("{\"reason\":\"wrong doc\"}").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "maker", authorities = {"ROLE_MAKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void makerMayNotReTender() throws Exception {
        mockMvc.perform(post("/api/procurement/packages/1/stages/2/re-tender")
                        .contentType("application/json").content("{\"reason\":\"failed\"}").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "maker", authorities = {"ROLE_MAKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void makerMayNotSetTheDepartmentBudget() throws Exception {
        mockMvc.perform(post("/api/procurement/department-budgets")
                        .contentType("application/json")
                        .content("{\"fiscalYear\":2026,\"department\":\"BPDB\",\"allocatedAmount\":100}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "checker", authorities = {"ROLE_CHECKER", "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE", "PERM_PROCUREMENT_VERIFY", "PERM_PROCUREMENT_OVERRIDE", "PERM_BUDGET_APPROVE"})
    void checkerMaySetTheDepartmentBudget() throws Exception {
        // The positive half of the split: an approval action a Checker is meant to
        // perform, chosen because it succeeds without needing a package fixture
        mockMvc.perform(post("/api/procurement/department-budgets")
                        .contentType("application/json")
                        .content("{\"fiscalYear\":2026,\"department\":\"BPDB\",\"allocatedAmount\":5000}")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "nobody", authorities = {"ROLE_VIEWER", "PERM_DOCUMENT_VIEW"})
    void anUnrelatedRoleGetsNothing() throws Exception {
        mockMvc.perform(get("/api/procurement/packages"))
                .andExpect(status().isForbidden());
    }

    /*
     * The legacy accounts (E-5). A user holds one role, so a DD1 who is also meant to
     * capture procurement data cannot simply be made a MAKER without losing the role that
     * grants their document access. Changeset 042 gives the capture permissions to the
     * legacy roles instead, and these two pin what that buys them: the module opens, but
     * approving still does not.
     */

    @Test
    @WithMockUser(username = "dd1_user",
            authorities = {"ROLE_DD1", "PERM_DOCUMENT_UPLOAD", "PERM_DOCUMENT_VIEW",
                           "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void aMigratedLegacyAccountCanReachTheModule() throws Exception {
        mockMvc.perform(get("/api/procurement/packages"))
                .andExpect(notForbidden());
    }

    @Test
    @WithMockUser(username = "dd1_user",
            authorities = {"ROLE_DD1", "PERM_DOCUMENT_UPLOAD", "PERM_DOCUMENT_VIEW",
                           "PERM_PROCUREMENT_VIEW", "PERM_PROCUREMENT_CAPTURE"})
    void aMigratedLegacyAccountIsNotSilentlyPromotedToApprover() throws Exception {
        // The migration must not hand approval rights to four levels of Deputy Director
        // as a side effect. Checkers are appointed deliberately, not by changeset.
        mockMvc.perform(post("/api/procurement/packages/1/stages/1/complete")
                        .contentType("application/json").content("{}").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
