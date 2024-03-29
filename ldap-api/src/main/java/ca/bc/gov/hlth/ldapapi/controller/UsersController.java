package ca.bc.gov.hlth.ldapapi.controller;

import ca.bc.gov.hlth.ldapapi.model.UserCredentials;
import ca.bc.gov.hlth.ldapapi.service.UserService;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.NamingException;

@RestController
public class UsersController {

    final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<Object> getUser(@RequestBody UserCredentials userCredentials) throws NamingException {
        Object user = userService.authenticate(userCredentials);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity(new EmptyJsonResponse(), HttpStatus.OK);
        }
    }

    @JsonSerialize
    public class EmptyJsonResponse {
    }

}
