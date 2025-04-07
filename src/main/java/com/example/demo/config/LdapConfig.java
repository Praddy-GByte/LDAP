package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class LdapConfig {

    @Value("${ldap_url}")
    private String ldapUrl;

    @Value("${ldap_port}")
    private String ldapPort;

    @Value("${base_dn}")
    private String baseDn;

    @Value("${bind_dn}")
    private String bindDn;

    @Value("${bind_passwd}")
    private String bindPassword;

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        // Construct the full LDAP URL with port
        String fullLdapUrl = ldapUrl + ":" + ldapPort;
        contextSource.setUrl(fullLdapUrl);
        contextSource.setBase(baseDn);
        contextSource.setUserDn(bindDn);
        contextSource.setPassword(bindPassword);
        
        // Enable connection pooling
        contextSource.setPooled(true);
        
        // Add any company-specific configuration
        Map<String, Object> config = new HashMap<>();
        config.put("java.naming.ldap.attributes.binary", "objectGUID");
        contextSource.setBaseEnvironmentProperties(config);
        
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(contextSource());
    }
}
