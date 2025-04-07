package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String uid;            // User ID (login username)
    private String cn;             // Common Name (full name)
    private String sn;             // Surname (last name)
    private String givenName;      // First name
    private String displayName;    // Display name
    private String mail;           // Email address
    private String employeeNumber; // Employee ID number
    private List<String> memberOf; // List of groups/roles
    private String role;           // Primary role
    private String title;          // Job title
    private String ou;             // Organizational unit (department)
    private String o;              // Organization (bank name)
} 