package ca.bc.gov.hlth.ldapapi.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrgLookupTest {

    private static OrgLookup orgLookup;

    @BeforeAll
    static void setUp() {
        orgLookup = new OrgLookup("https://user-management-dev.hlth.gov.bc.ca/organizations.json");
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
        Map<String, String> map = orgLookup.determineOrgJsonFromLdapUserName("uid=1-primehcimintegrationtest,o=00002855");
        assertAll(
                () -> assertEquals("00002855", map.get("id"), "id"),
                () -> assertEquals("PRS BCMOH - Registry Administrator", map.get("name"), "name")
        );
    }

    @Test
    public void determineOrg_orgDoesNotExist() {
        assertThrows(IllegalStateException.class, () -> orgLookup.determineOrgJsonFromLdapUserName("uid=1-primehcimintegrationtest,o=DoesNotExist"));
    }

}