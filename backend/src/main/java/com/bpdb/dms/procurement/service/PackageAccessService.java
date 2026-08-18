package com.bpdb.dms.procurement.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bpdb.dms.entity.User;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.repository.UserRepository;

/**
 * Who may see and act on which package (REQ-P20).
 *
 * <p>Every procurement route takes a package id straight from the path. The role model
 * decides <em>what</em> a user may do — capture, approve — but nothing decided <em>which
 * packages</em> they may do it to, so any authenticated holder of PROCUREMENT_VIEW could
 * read and act on every package in the system. The requirement is explicit that
 * persistence must not bypass the role model this way.
 *
 * <p>Ownership follows the department, which is a first-class column on the package
 * exactly so this is possible (Q-20). Administrators are unscoped, because somebody has to
 * be able to see across departments to run the thing.
 *
 * <p><b>Off by default.</b> Q-20 puts one department in the pilot, and switching on
 * cross-department refusal in a single-department deployment can only produce false
 * refusals — a user whose department is spelled differently, or not filled in at all,
 * would lose access to work that is plainly theirs. So the rule is built, tested and
 * dormant, and {@code app.procurement.enforce-department-scope=true} turns it on for the
 * day a second department arrives. A capability that exists and is disabled is a
 * configuration change; one that does not exist is a project.
 */
@Service
public class PackageAccessService {

    private static final Logger log = LoggerFactory.getLogger(PackageAccessService.class);

    private static final String ADMIN = "ROLE_ADMIN";

    private final ProcurementPackageRepository packageRepository;
    private final UserRepository userRepository;

    @Value("${app.procurement.enforce-department-scope:false}")
    private boolean enforce;

    public PackageAccessService(ProcurementPackageRepository packageRepository,
                                UserRepository userRepository) {
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
    }

    public boolean isEnforcing() {
        return enforce;
    }

    void setEnforcing(boolean enforce) {
        this.enforce = enforce;
    }

    /**
     * Refuse the call if this user has no business with this package.
     *
     * @throws AccessDeniedForPackage when the package belongs to another department
     */
    public void requireAccess(Long packageId) {
        if (!enforce || packageId == null) {
            return;
        }
        ProcurementPackage pkg = packageRepository.findById(packageId).orElse(null);
        if (pkg == null) {
            return; // a missing package is the caller's problem to report, not a denial
        }
        if (!canAccess(pkg)) {
            log.warn("User {} refused access to package {} ({} department)",
                    currentUsername(), packageId, pkg.getDepartment());
            throw new AccessDeniedForPackage(
                    "This package belongs to " + pkg.getDepartment()
                    + " and you are not in that department (REQ-P20)");
        }
    }

    /** Whether the acting user may see this package at all. */
    public boolean canAccess(ProcurementPackage pkg) {
        if (!enforce || pkg == null || isAdmin()) {
            return true;
        }
        String theirs = currentUser().map(User::getDepartment).orElse(null);
        String owning = pkg.getDepartment();
        if (owning == null || owning.isBlank()) {
            // An unassigned package is nobody's private property
            return true;
        }
        return theirs != null && theirs.trim().equalsIgnoreCase(owning.trim());
    }

    /** Narrow a list to what the caller may see, for the list and dashboard routes. */
    public List<ProcurementPackage> visible(List<ProcurementPackage> packages) {
        if (!enforce || isAdmin()) {
            return packages;
        }
        return packages.stream().filter(this::canAccess).toList();
    }

    /**
     * The department to scope a query to, or null for "all of them".
     *
     * <p>Used where a caller supplies a department as a filter: taking it from the request
     * lets anyone read any department by asking, so the identity wins over the parameter.
     */
    public String scopeDepartment(String requested) {
        if (!enforce || isAdmin()) {
            return requested;
        }
        return currentUser().map(User::getDepartment).orElse(requested);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (ADMIN.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Optional<User> currentUser() {
        String username = currentUsername();
        return username == null ? Optional.empty() : userRepository.findByUsername(username);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    /** Refused because of who owns the package, not because of what the user may do. */
    public static class AccessDeniedForPackage extends RuntimeException {
        public AccessDeniedForPackage(String message) {
            super(message);
        }
    }
}
