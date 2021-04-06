package ca.bc.gov.hlth.ldapapi.model;

public class User {
    private boolean authenticated;
    private boolean unlocked;
    private boolean passwordExpired;
    private String username;
    private String gisuserrole;

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

    public boolean isPasswordExpired() {
        return passwordExpired;
    }

    public void setPasswordExpired(boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }
}
