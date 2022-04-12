package ca.bc.gov.hlth.ldapapi.service;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrgLookup {

    final Map<String, String> orgs = new HashMap<>();
    final RestTemplate restTemplate = new RestTemplate();

    public OrgLookup(String organizationJsonUrl) {
        List<Map<String, String>> orgList = restTemplate.getForObject(organizationJsonUrl, List.class);
        orgList.stream().forEach(org -> orgs.put(org.get("id"), org.get("name")));
    }

    Optional<String> lookup(String orgid) {
        return Optional.ofNullable(orgs.get(orgid));
    }
}
