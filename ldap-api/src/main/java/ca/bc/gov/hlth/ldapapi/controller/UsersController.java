package ca.bc.gov.hlth.ldapapi.controller;

import ca.bc.gov.hlth.ldapapi.service.LdapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Properties;

@RestController
public class UsersController {

    final LdapService ldapService;

    public UsersController(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    @GetMapping("/users/{userId}")
    public Map<String, String> getUser(@PathVariable String userId, @RequestBody String userPassword) {
        return ldapService.authenticate(userId, userPassword);
    }

}
