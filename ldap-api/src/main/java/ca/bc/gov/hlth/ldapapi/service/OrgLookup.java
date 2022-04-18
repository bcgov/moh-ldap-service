package ca.bc.gov.hlth.ldapapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrgLookup {

    private final Map<String, String> orgs = new HashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Pattern orgIdPattern = Pattern.compile("o=(\\d{8})");
    private final String organizationJsonUrl;

    private final Logger logger = LoggerFactory.getLogger(OrgLookup.class);

    public OrgLookup(@Value("${organizationJsonUrl}") String organizationJsonUrl) {
        this.organizationJsonUrl = organizationJsonUrl;
        init();
    }

    private synchronized void init() {
        List<Map<String, String>> orgList = restTemplate.getForObject(this.organizationJsonUrl, List.class);
        orgList.forEach(org -> orgs.put(org.get("id"), org.get("name")));
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
            logger.warn("Organization '{}' not found. Reloading orgs from '{}'.", orgId, organizationJsonUrl);
            init();
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
