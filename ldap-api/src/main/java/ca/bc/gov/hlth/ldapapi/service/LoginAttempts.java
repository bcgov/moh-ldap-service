package ca.bc.gov.hlth.ldapapi.service;

import java.time.LocalDateTime;

public class LoginAttempts {
    private int attempts;
    private LocalDateTime lastAttempt;

    public LoginAttempts(int attempts, LocalDateTime lastAttempt) {
        this.attempts = attempts;
        this.lastAttempt = lastAttempt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(LocalDateTime lastAttempt) {
        this.lastAttempt = lastAttempt;
    }
}
