package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.LdapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ldap")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LdapController {

    private final LdapService ldapService;

 /*   // http://localhost:8081/api/ldap/roles
    @GetMapping("/roles")
    public ResponseEntity<?> getAllRolesAndUsers() {
        try {
            Map<String, List<String>> rolesAndUsers = ldapService.listAllRolesAndUsers();
            return ResponseEntity.ok(rolesAndUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error retrieving roles and users: " + e.getMessage());
        }
    }*/

    // http://localhost:8081/api/ldap/users?role=Italians
    @GetMapping("/users")
    public ResponseEntity<?> getUsersByRole(@RequestParam String role) {
        try {
            List<User> users = ldapService.getUsersWithDetailsByRole(role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error retrieving users for role '" + role + "': " + e.getMessage());
        }
    }
}
