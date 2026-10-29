package com.example.foodndeliv.controller;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Value("${keycloak.server-url:http://my-keycloak:80}")
    private String serverUrl;

    @Value("${keycloak.realm:foodndeliv}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String clientId;

    @PostMapping("/bulk-reset-passwords")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bulkResetPasswords() {
        logger.info("AUDIT: Bulk password reset initiated by admin user.");

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // Admin operations are usually authenticated against the master realm
                .clientId(clientId)
                .username(adminUsername)
                .password(adminPassword)
                .build()) {

            List<UserRepresentation> users = keycloak.realm(realm).users().list();
            int resetCount = 0;
            
            for (UserRepresentation user : users) {
                // Set the UPDATE_PASSWORD required action
                List<String> requiredActions = user.getRequiredActions();
                if (requiredActions != null && !requiredActions.contains("UPDATE_PASSWORD")) {
                    requiredActions.add("UPDATE_PASSWORD");
                    user.setRequiredActions(requiredActions);
                    keycloak.realm(realm).users().get(user.getId()).update(user);
                    resetCount++;
                } else if (requiredActions == null) {
                    user.setRequiredActions(List.of("UPDATE_PASSWORD"));
                    keycloak.realm(realm).users().get(user.getId()).update(user);
                    resetCount++;
                }
            }

            logger.info("AUDIT: Bulk password reset completed successfully. {} users affected.", resetCount);
            return ResponseEntity.ok("Successfully triggered password reset for " + resetCount + " users.");
            
        } catch (Exception e) {
            logger.error("AUDIT: Failed to execute bulk password reset.", e);
            return ResponseEntity.internalServerError().body("Failed to execute bulk password reset: " + e.getMessage());
        }
    }
}
