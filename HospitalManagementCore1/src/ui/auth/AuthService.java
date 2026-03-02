package ui.auth;

public class AuthService {
    private final UserRepository repo;

    public AuthService(UserRepository repo) {
        this.repo = repo;
    }

    public UserAccount authenticate(String username, String password) {
        UserAccount user = repo.findByUsername(username);
        if (user == null) return null;

        String stored = user.getPasswordHash(); // veya getPassword()

        // stored hash mi plain mi?
        boolean storedLooksHashed = stored != null && stored.matches("^[a-f0-9]{64}$");

        if (storedLooksHashed) {
            String inputHash = PasswordUtil.sha256(password);
            return inputHash.equals(stored) ? user : null;
        } else {
            // demo plain password
            return stored.equals(password) ? user : null;
        }
    }


    
}
