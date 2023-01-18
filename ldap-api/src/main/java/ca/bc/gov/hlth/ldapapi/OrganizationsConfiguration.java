package ca.bc.gov.hlth.ldapapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrganizationsConfiguration {

    private final String keycloakTokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String organizationJsonUrl;

    public OrganizationsConfiguration(@Value("${keycloak.token-uri}") String keycloakTokenUri,
                                      @Value("${keycloak.client-id}") String clientId,
                                      @Value("${keycloak.client-secret}") String clientSecret,
                                      @Value("${organizationJsonUrl}") String organizationJsonUrl) {
        this.keycloakTokenUri = keycloakTokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.organizationJsonUrl = organizationJsonUrl;
    }

    public String getKeycloakTokenUri() {
        return keycloakTokenUri;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getOrganizationJsonUrl() {
        return organizationJsonUrl;
    }
}
