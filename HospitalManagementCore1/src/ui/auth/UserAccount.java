package ui.auth;

import ui.models.Role;

public class UserAccount {
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final String linkedTc;

    public UserAccount(String username, String passwordHash, Role role, String linkedTc) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.linkedTc = linkedTc;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getLinkedTc() { return linkedTc; }
    
    
}

