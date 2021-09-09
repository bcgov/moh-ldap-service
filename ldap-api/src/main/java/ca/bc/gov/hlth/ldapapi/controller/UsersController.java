package ca.bc.gov.hlth.ldapapi.controller;

import ca.bc.gov.hlth.ldapapi.model.User;
import ca.bc.gov.hlth.ldapapi.model.UserCredentials;
import ca.bc.gov.hlth.ldapapi.service.LdapService;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController {

    final LdapService ldapService;

    public UsersController(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    @PostMapping("/users")
    public ResponseEntity<User> getUser(@RequestBody UserCredentials userCredentials) {
        User user = ldapService.authenticate(userCredentials);
        if (user != null) {
            return new ResponseEntity<User>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity(new EmptyJsonResponse(), HttpStatus.OK);
        }
    }

    @JsonSerialize
    public class EmptyJsonResponse {
    }

}
