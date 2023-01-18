package ca.bc.gov.hlth.ldapapi.service;

import ca.bc.gov.hlth.ldapapi.OrganizationsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class OrgLookup {

    private final Map<String, String> orgs = new HashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Pattern orgIdPattern = Pattern.compile("o=(\\d{8})");

    private final Logger logger = LoggerFactory.getLogger(OrgLookup.class);

    @Autowired
    private final OrganizationsConfiguration organizationsConfiguration;

    public OrgLookup(OrganizationsConfiguration organizationsConfiguration){
        this.organizationsConfiguration = organizationsConfiguration;
        init();
    }

    private synchronized void init() {
        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Bearer " + getKeycloakAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(organizationsConfiguration.getOrganizationJsonUrl(), HttpMethod.GET, entity, List.class);
        List<Map<String, String>> orgList = response.getBody();
        orgList.forEach(org -> orgs.put(org.get("organizationId"), org.get("name")));
    }

    /**
     * Takes a username like "uid=1-primehcimintegrationtest,o=00002855" and returns
     * a Map {"id": "00002855", "name": "PRS BCMOH - Registry Administrator"}.
     * <p>
     * Has special case handling for "o=Ministry of Health". For this case it returns
     * a Map {"id": "00000010", "name": "Ministry of Health"}.
     *
     * @param userName an LDAP user name, e.g. "uid=1-primehcimintegrationtest,o=00002855".
     * @return a Map containing keys "id" and "name".
     */
    Map<String, String> determineOrgJsonFromLdapUserName(String userName) {

        String orgId = parseOrgId(userName)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Could not find organization ID in userName: '%s'", userName)));

        Optional<String> possiblyOrg = lookup(orgId);
        if (!possiblyOrg.isPresent()) {
            logger.warn("Organization '{}' not found. Reloading orgs from '{}'.", orgId, organizationsConfiguration.getOrganizationJsonUrl());
            init();
            possiblyOrg = lookup(orgId); // Try another lookup attempt following org reload
        }
        String orgName = possiblyOrg.orElseThrow(
                () -> new IllegalArgumentException(String.format("Could not find organization name for ID: '%s'", orgId))
        );

        Map<String, String> org = new HashMap<>();
        org.put("id", orgId);
        org.put("name", orgName);
        return org;
    }

    private Optional<String> lookup(String orgid) {
        return Optional.ofNullable(orgs.get(orgid));
    }

    private String getKeycloakAccessToken() {

        HttpHeaders headers = setRequestHeaders();
        MultiValueMap<String, String> credentialsMap = setRequestCredentials();

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(credentialsMap, headers);

        ResponseEntity<Object> response = restTemplate.exchange(organizationsConfiguration.getKeycloakTokenUri(), HttpMethod.POST, entity, Object.class);
        LinkedHashMap<String, Object> responseBody = (LinkedHashMap<String, Object>)response.getBody();

        return (String)responseBody.get("access_token");
    }

    private HttpHeaders setRequestHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        headers.set("Cache-Control", "no-cache");
        headers.set("Accept", "application/json");
        return headers;
    }

    private MultiValueMap<String, String> setRequestCredentials(){
        MultiValueMap<String, String> credentialsMap = new LinkedMultiValueMap<>();
        credentialsMap.add("grant_type", "client_credentials");
        credentialsMap.add("client_id", organizationsConfiguration.getClientId());
        credentialsMap.add("client_secret", organizationsConfiguration.getClientSecret());
        return credentialsMap;
    }

    private static Optional<String> parseOrgId(String userName) {
        if (userName.contains("o=Ministry of Health")) {
            return Optional.of("00000010");
        }
        Matcher matcher = orgIdPattern.matcher(userName);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }
}
