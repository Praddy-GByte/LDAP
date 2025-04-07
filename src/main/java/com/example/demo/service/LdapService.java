package com.example.demo.service;

import com.example.demo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LdapService {

    private final LdapTemplate ldapTemplate;
    private final LdapAttributeMapper ldapAttributeMapper;

    @Value("${base_dn}")
    private String baseDn;

    // LDAP schema configuration - can be customized for different LDAP servers
    @Value("${ldap.user.id.attribute:uid}")
    private String userIdAttribute;

    @Value("${ldap.group.objectclass:groupOfUniqueNames}")
    private String groupObjectClass;

    @Value("${ldap.group.member.attribute:uniqueMember}")
    private String groupMemberAttribute;

    @Value("${ldap.group.name.attribute:cn}")
    private String groupNameAttribute;

    /**
     * Custom runtime exception for LDAP operations
     */
    public static class LdapOperationException extends RuntimeException {
        public LdapOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Static inner class to hold search state for pagination
     */
    private static class SearchState {
        int startIndex = 0;
        boolean hasMoreMembers = true;
    }

    /**
     * Retrieves a list of users with their details for users having the specified role.
     * Handles both small groups (<1500 users) and large groups (≥1500 users) using LDAP range retrieval.
     * @param role The role to search for
     * @return List of User objects having the specified role
     * @throws LdapOperationException if there is an error during LDAP operations
     */
    public List<User> getUsersWithDetailsByRole(String role) {
        try {
            log.info("Searching for users with role: {}", role);
            
            List<User> users = new ArrayList<>();
            final SearchState state = new SearchState();
            
            while (state.hasMoreMembers) {
                // Search for the specific group with range if needed
                String searchFilter = buildSearchFilter(role, state.startIndex);
                log.debug("Executing LDAP search with filter: {}", searchFilter);
                
                ldapTemplate.search(
                    "",
                    searchFilter,
                    (AttributesMapper<Void>) attrs -> {
                        try {
                            processGroupMembers(attrs, users, role, state);
                        } catch (Exception e) {
                            log.error("Error processing group members: {}", e.getMessage());
                            state.hasMoreMembers = false;
                        }
                        return null;
                    }
                );
                
                if (state.startIndex == 0) {
                    // If we haven't moved to the next range, we're done
                    state.hasMoreMembers = false;
                }
            }
            
            log.info("Found {} users with details for role {}", users.size(), role);
            return users;
            
        } catch (Exception e) {
            String errorMsg = String.format("Error retrieving users for role %s: %s", role, e.getMessage());
            log.error(errorMsg, e);
            throw new LdapOperationException(errorMsg, e);
        }
    }

    /**
     * Retrieves detailed user information for a specific user ID
     * @param uid The user ID to search for
     * @return User object containing all user details
     * @throws LdapOperationException if there is an error during LDAP operations
     */
    public User getUserDetails(String uid) {
        try {
            log.info("Searching for user details with uid: {}", uid);
            
            List<User> users = ldapTemplate.search(
                "",
                "(uid=" + uid + ")",
                ldapAttributeMapper
            );
            
            if (users != null && !users.isEmpty()) {
                log.info("Found user details for uid: {}", uid);
                return users.get(0);
            } else {
                log.warn("No user found with uid: {}", uid);
                return null;
            }
            
        } catch (Exception e) {
            String errorMsg = String.format("Error retrieving user details for uid %s: %s", uid, e.getMessage());
            log.error(errorMsg, e);
            throw new LdapOperationException(errorMsg, e);
        }
    }

    /**
     * Builds the LDAP search filter based on role and start index
     * @param role Role to search for
     * @param startIndex Start index for range-based retrieval
     * @return LDAP search filter
     */
    private String buildSearchFilter(String role, int startIndex) {
        if (startIndex > 0) {
            return String.format("(&(objectClass=%s)(%s=%s)(%s;range=%d-*)", 
                groupObjectClass, groupNameAttribute, role, groupMemberAttribute, startIndex);
        } else {
            return String.format("(&(objectClass=%s)(%s=%s))", 
                groupObjectClass, groupNameAttribute, role);
        }
    }

    /**
     * Processes group members from LDAP attributes
     * @param attrs LDAP attributes
     * @param users List to add users to
     * @param role Role being searched
     * @param state Search state for pagination
     */
    private void processGroupMembers(Attributes attrs, List<User> users, String role, SearchState state) {
        try {
            if (attrs.get(groupMemberAttribute) != null) {
                javax.naming.NamingEnumeration<?> memberValues = attrs.get(groupMemberAttribute).getAll();
                while (memberValues.hasMore()) {
                    String memberDn = memberValues.next().toString();
                    String uid = extractUidFromDn(memberDn);
                    
                    // Get full user details
                    User userDetails = getUserDetails(uid);
                    if (userDetails != null) {
                        users.add(userDetails);
                        log.debug("Found user details for {} in role {}", uid, role);
                    }
                }
                
                // Check if there are more members to fetch
                processRangeAttribute(attrs, state);
            } else {
                state.hasMoreMembers = false;
            }
        } catch (NamingException e) {
            throw new LdapOperationException("Error processing group members", e);
        }
    }

    /**
     * Extracts the user ID from a DN string
     * @param dn The DN string to extract from
     * @return The extracted user ID
     */
    private String extractUidFromDn(String dn) {
        // This method needs to be customized based on your company's LDAP structure
        // For example, if your company uses "uid=username,ou=users,dc=company,dc=com"
        // you would extract "username" from this DN
        
        // Default implementation assumes uid=username format
        int startIndex = dn.indexOf(userIdAttribute + "=") + userIdAttribute.length() + 1;
        int endIndex = dn.indexOf(",", startIndex);
        if (endIndex == -1) {
            return dn.substring(startIndex);
        }
        return dn.substring(startIndex, endIndex);
    }

    /**
     * Processes the range attribute to determine if more members need to be fetched
     * @param attrs LDAP attributes
     * @param state Search state to update
     */
    private void processRangeAttribute(Attributes attrs, SearchState state) {
        try {
            javax.naming.directory.Attribute rangeAttr = attrs.get(groupMemberAttribute + ";range");
            if (rangeAttr != null) {
                String rangeValue = rangeAttr.get().toString();
                if (rangeValue.contains("range=")) {
                    // Extract the end of the current range
                    String[] rangeParts = rangeValue.split("range=")[1].split("-");
                    if (rangeParts.length == 2) {
                        int endIndex = Integer.parseInt(rangeParts[1]);
                        state.startIndex = endIndex + 1;
                        state.hasMoreMembers = true;
                        log.debug("More members available, next range starts at: {}", state.startIndex);
                    } else {
                        state.hasMoreMembers = false;
                    }
                } else {
                    state.hasMoreMembers = false;
                }
            } else {
                state.hasMoreMembers = false;
            }
        } catch (NamingException e) {
            throw new LdapOperationException("Error processing range attribute", e);
        }
    }
} 