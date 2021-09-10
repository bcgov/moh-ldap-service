package ca.bc.gov.hlth.ldapapi.model;

public class User {
    private boolean authenticated;
    private boolean unlocked;
    private String username;
    private String gisuserrole;
    private Long lockoutTimeInHours;
    private Integer remainingAttempts;

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGisuserrole() {
        return gisuserrole;
    }

    public void setGisuserrole(String gisuserrole) {
        this.gisuserrole = gisuserrole;
    }
    
    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(int remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }
    
    public Long getLockoutTimeInHours() {
        return lockoutTimeInHours;
    }

    public void setLockoutTimeInHours(long lockoutTimeInHours) {
        this.lockoutTimeInHours = lockoutTimeInHours;
    }
    
    
}
