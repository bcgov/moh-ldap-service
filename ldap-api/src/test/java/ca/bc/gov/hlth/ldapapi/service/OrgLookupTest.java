package ca.bc.gov.hlth.ldapapi.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrgLookupTest {

    private static OrgLookup orgLookup;

    @BeforeAll
    static void setUp() {
        orgLookup = new OrgLookup("https://user-management-dev.hlth.gov.bc.ca/organizations.json");
    }

    @Test
    public void lookup1() {
        String orgName = orgLookup.lookup("00000010").get();
        assertEquals("Ministry of Health", orgName);
    }

    @Test
    public void lookup2() {
        String orgName = orgLookup.lookup("00002855").get();
        assertEquals("PRS BCMOH - Registry Administrator", orgName);
    }

    @Test
    public void lookup_doesNotExit_throwsNoSuchElementException() {
        assertFalse(orgLookup.lookup("does_not_exist").isPresent());
    }

}