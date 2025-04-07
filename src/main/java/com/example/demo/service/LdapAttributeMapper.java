package com.example.demo.service;

import com.example.demo.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper class to convert LDAP attributes to User objects
 */
@Component
@Slf4j
public class LdapAttributeMapper implements AttributesMapper<User> {

    // LDAP attribute mapping configuration
    @Value("${ldap.attribute.uid:uid}")
    private String uidAttribute;

    @Value("${ldap.attribute.cn:cn}")
    private String cnAttribute;

    @Value("${ldap.attribute.sn:sn}")
    private String snAttribute;

    @Value("${ldap.attribute.givenName:givenName}")
    private String givenNameAttribute;

    @Value("${ldap.attribute.displayName:displayName}")
    private String displayNameAttribute;

    @Value("${ldap.attribute.mail:mail}")
    private String mailAttribute;

    @Value("${ldap.attribute.employeeNumber:employeeNumber}")
    private String employeeNumberAttribute;

    @Value("${ldap.attribute.memberOf:memberOf}")
    private String memberOfAttribute;

    @Value("${ldap.attribute.role:role}")
    private String roleAttribute;

    @Value("${ldap.attribute.title:title}")
    private String titleAttribute;

    @Value("${ldap.attribute.ou:ou}")
    private String ouAttribute;

    @Value("${ldap.attribute.o:o}")
    private String oAttribute;

    /**
     * Maps LDAP attributes to a User object
     * @param attrs LDAP attributes
     * @return User object with mapped attributes
     * @throws LdapService.LdapOperationException if there is an error mapping attributes
     */
    @Override
    public User mapFromAttributes(Attributes attrs) throws NamingException {
        try {
            User user = new User();
            
            // Set basic identity information
            if (attrs.get(uidAttribute) != null) user.setUid(attrs.get(uidAttribute).get().toString());
            if (attrs.get(cnAttribute) != null) user.setCn(attrs.get(cnAttribute).get().toString());
            if (attrs.get(snAttribute) != null) user.setSn(attrs.get(snAttribute).get().toString());
            if (attrs.get(givenNameAttribute) != null) user.setGivenName(attrs.get(givenNameAttribute).get().toString());
            if (attrs.get(displayNameAttribute) != null) user.setDisplayName(attrs.get(displayNameAttribute).get().toString());
            if (attrs.get(mailAttribute) != null) user.setMail(attrs.get(mailAttribute).get().toString());
            if (attrs.get(employeeNumberAttribute) != null) user.setEmployeeNumber(attrs.get(employeeNumberAttribute).get().toString());
            
            // Set role and access information
            if (attrs.get(memberOfAttribute) != null) {
                List<String> memberOf = new ArrayList<>();
                javax.naming.NamingEnumeration<?> memberValues = attrs.get(memberOfAttribute).getAll();
                while (memberValues.hasMore()) {
                    memberOf.add(memberValues.next().toString());
                }
                user.setMemberOf(memberOf);
            }
            
            if (attrs.get(roleAttribute) != null) user.setRole(attrs.get(roleAttribute).get().toString());
            if (attrs.get(titleAttribute) != null) user.setTitle(attrs.get(titleAttribute).get().toString());
            if (attrs.get(ouAttribute) != null) user.setOu(attrs.get(ouAttribute).get().toString());
            if (attrs.get(oAttribute) != null) user.setO(attrs.get(oAttribute).get().toString());
            
            return user;
        } catch (NamingException e) {
            throw new LdapService.LdapOperationException("Error mapping LDAP attributes to User", e);
        }
    }
} 