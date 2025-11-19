package com.bpdb.dms.controller;

import com.bpdb.dms.entity.SmartFolderDefinition;
import com.bpdb.dms.entity.SmartFolderScope;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.SmartFolderDefinitionRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.SmartFolderEvaluationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dmc/folders")
@CrossOrigin(origins = "*")
public class SmartFolderController {

    @Autowired
    private SmartFolderDefinitionRepository smartFolderRepository;

    @Autowired
    private SmartFolderEvaluationService evaluationService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @CacheEvict(cacheNames = "dmcEval", allEntries = true)
    public ResponseEntity<?> createFolder(@AuthenticationPrincipal UserDetails principal,
                                          @RequestBody Map<String, Object> body) {
        User owner = getUser(principal);
        if (owner == null) return ResponseEntity.status(401).body("Unauthorized");

        String name = (String) body.getOrDefault("name", "");
        String description = (String) body.getOrDefault("description", "");
        String definition = (String) body.getOrDefault("definition", "{}");
        String scopeStr = (String) body.getOrDefault("scope", "PRIVATE");
        SmartFolderScope scope = parseScope(scopeStr);

        SmartFolderDefinition folder = new SmartFolderDefinition();
        folder.setName(name);
        folder.setDescription(description);
        folder.setDefinition(definition);
        folder.setScope(scope);
        folder.setOwner(owner);

        SmartFolderDefinition saved = smartFolderRepository.save(folder);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFolder(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long id) {
        User user = getUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Optional<SmartFolderDefinition> opt = smartFolderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SmartFolderDefinition folder = opt.get();
        if (!canView(folder, user)) return ResponseEntity.status(403).body("Forbidden");
        return ResponseEntity.ok(folder);
    }

    @GetMapping
    public ResponseEntity<?> listFolders(@AuthenticationPrincipal UserDetails principal,
                                         @RequestParam(value = "scope", required = false) String scopeStr) {
        User user = getUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        if (scopeStr == null || scopeStr.isBlank()) {
            List<SmartFolderDefinition> mine = smartFolderRepository.findByOwner(user);
            return ResponseEntity.ok(mine);
        }
        SmartFolderScope scope = parseScope(scopeStr);
        if (scope == SmartFolderScope.PRIVATE) {
            List<SmartFolderDefinition> mine = smartFolderRepository.findByOwner(user);
            return ResponseEntity.ok(mine);
        } else {
            List<SmartFolderDefinition> shared = smartFolderRepository.findByScope(scope);
            return ResponseEntity.ok(shared);
        }
    }

    @PutMapping("/{id}")
    @CacheEvict(cacheNames = "dmcEval", allEntries = true)
    public ResponseEntity<?> updateFolder(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        User user = getUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<SmartFolderDefinition> opt = smartFolderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SmartFolderDefinition folder = opt.get();
        if (!isOwnerOrAdmin(folder, user)) return ResponseEntity.status(403).body("Forbidden");

        if (body.containsKey("name")) folder.setName((String) body.get("name"));
        if (body.containsKey("description")) folder.setDescription((String) body.get("description"));
        if (body.containsKey("definition")) folder.setDefinition((String) body.get("definition"));
        if (body.containsKey("scope")) folder.setScope(parseScope((String) body.get("scope")));
        if (body.containsKey("isActive")) folder.setIsActive(Boolean.valueOf(String.valueOf(body.get("isActive"))));

        SmartFolderDefinition saved = smartFolderRepository.save(folder);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(cacheNames = "dmcEval", allEntries = true)
    public ResponseEntity<?> deleteFolder(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id) {
        User user = getUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<SmartFolderDefinition> opt = smartFolderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SmartFolderDefinition folder = opt.get();
        if (!isOwnerOrAdmin(folder, user)) return ResponseEntity.status(403).body("Forbidden");

        folder.setIsActive(false);
        smartFolderRepository.save(folder);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<?> evaluate(@AuthenticationPrincipal UserDetails principal,
                                      @PathVariable Long id,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        User user = getUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<SmartFolderDefinition> opt = smartFolderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SmartFolderDefinition folder = opt.get();
        if (!canView(folder, user)) return ResponseEntity.status(403).body("Forbidden");
        Pageable pageable = PageRequest.of(page, size);
        Page<?> results = evaluationService.evaluate(folder, user, pageable);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}/share")
    @CacheEvict(cacheNames = "dmcEval", allEntries = true)
    public ResponseEntity<?> share(@AuthenticationPrincipal UserDetails principal,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        User user = getUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<SmartFolderDefinition> opt = smartFolderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SmartFolderDefinition folder = opt.get();
        if (!isOwnerOrAdmin(folder, user)) return ResponseEntity.status(403).body("Forbidden");

        SmartFolderScope scope = parseScope(body.getOrDefault("scope", "PRIVATE"));
        folder.setScope(scope);
        SmartFolderDefinition saved = smartFolderRepository.save(folder);
        return ResponseEntity.ok(saved);
    }

    // Create from (saved) search definition (linkage)
    @PostMapping("/from-search")
    @CacheEvict(cacheNames = "dmcEval", allEntries = true)
    public ResponseEntity<?> createFromSearch(@AuthenticationPrincipal UserDetails principal,
                                              @RequestBody Map<String, Object> body) {
        // Treat the provided 'definition' as the saved search JSON
        return createFolder(principal, body);
    }

    private User getUser(UserDetails principal) {
        if (principal == null) return null;
        return userRepository.findByUsernameWithRole(principal.getUsername()).orElse(null);
    }

    private boolean canView(SmartFolderDefinition folder, User user) {
        if (folder.getScope() == SmartFolderScope.PRIVATE) {
            return isOwnerOrAdmin(folder, user);
        }
        if (folder.getScope() == SmartFolderScope.DEPARTMENT) {
            if (folder.getOwner() == null || folder.getOwner().getDepartment() == null) return false;
            return folder.getOwner().getDepartment().equals(user.getDepartment()) || isAdminUser(user);
        }
        return Boolean.TRUE.equals(folder.getIsActive());
    }

    private boolean isOwnerOrAdmin(SmartFolderDefinition folder, User user) {
        return (folder.getOwner() != null && folder.getOwner().getId().equals(user.getId())) || isAdminUser(user);
    }

    private boolean isAdminUser(User user) {
        try {
            return user != null && user.getRole() != null && user.getRole().getName() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName().name());
        } catch (Throwable t) {
            return false;
        }
    }

    private SmartFolderScope parseScope(String scope) {
        try {
            return SmartFolderScope.valueOf(scope == null ? "PRIVATE" : scope.toUpperCase());
        } catch (Exception e) {
            return SmartFolderScope.PRIVATE;
        }
    }
}


