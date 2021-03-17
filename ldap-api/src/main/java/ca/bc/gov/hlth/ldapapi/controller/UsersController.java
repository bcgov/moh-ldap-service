package ca.bc.gov.hlth.ldapapi.controller;

import ca.bc.gov.hlth.ldapapi.model.User;
import ca.bc.gov.hlth.ldapapi.model.UserCredentials;
import ca.bc.gov.hlth.ldapapi.service.LdapService;
import org.springframework.web.bind.annotation.*;

@RestController
public class UsersController {

    final LdapService ldapService;

    public UsersController(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    @PostMapping("/users")
    public User getUser(@RequestBody UserCredentials userCredentials) {
        return ldapService.authenticate(userCredentials);
    }

}
