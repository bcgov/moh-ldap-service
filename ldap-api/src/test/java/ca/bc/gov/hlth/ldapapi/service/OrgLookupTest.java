package ca.bc.gov.hlth.ldapapi.service;

import ca.bc.gov.hlth.ldapapi.OrganizationsConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrgLookupTest {

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    @Value("${organizationJsonUrl}")
    private String organizationJsonUrl;
    private OrganizationsConfiguration organizationsConfiguration;
    private OrgLookup orgLookup;

    @BeforeAll
    public void setup() {
        organizationsConfiguration = new OrganizationsConfiguration(
                keycloakTokenUri,
                clientId,
                clientSecret,
                organizationJsonUrl
        );
        orgLookup = new OrgLookup(organizationsConfiguration);
    }


    @Test
    public void determineOrg_moh() {
        Map<String, String> map = orgLookup.determineOrgJsonFromLdapUserName("uid=3-primehcimintegrationtest,o=Ministry of Health");
        assertAll(
                () -> assertEquals("00000010", map.get("id"), "id"),
                () -> assertEquals("Ministry of Health", map.get("name"), "name")
        );
    }

    @Test
    public void determineOrg_standardEightDigit() {
        Map<String, String> map = orgLookup.determineOrgJsonFromLdapUserName("uid=1-primehcimintegrationtest,o=00025379");
        assertAll(
                () -> assertEquals("00025379", map.get("id"), "id"),
                () -> assertEquals("Ironwood Clay Company Inc.", map.get("name"), "name")
        );
    }

    @Test
    public void determineOrg_badOrgFormat() {
        assertThrows(IllegalArgumentException.class, () -> orgLookup.determineOrgJsonFromLdapUserName("uid=1-primehcimintegrationtest,o=badOrgFormat"));
    }

    @Test
    public void determineOrg_orgDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> orgLookup.determineOrgJsonFromLdapUserName("uid=1-primehcimintegrationtest,o=99999999"));
    }

}