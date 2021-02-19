package ca.bc.gov.hlth.ldapapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class LdapConfiguration {

    @Value("${ldap.provider.url}")
    private String providerUrl;

    @Value("${ldap.security.principal}")
    private String securityPrincipal;

    @Value("${ldap.security.credentials}")
    private String securityCredentials;

    @Bean("ldapProperties")
    public Properties ldapProperties() {
        Properties ldapProperties = new Properties();
        ldapProperties.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        ldapProperties.put("java.naming.security.authentication", "simple");
        ldapProperties.put("java.naming.provider.url", providerUrl);
        ldapProperties.put("java.naming.security.principal", securityPrincipal);
        ldapProperties.put("java.naming.security.credentials", securityCredentials);
        return ldapProperties;
    }
}
