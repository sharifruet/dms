import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for "password"
        String password = "password";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("Generated Hash: " + hash);
        
        // Test verification
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification test: " + matches);
        
        // Test with a known working hash
        String knownHash = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW";
        boolean knownMatches = encoder.matches(password, knownHash);
        System.out.println("Known hash test: " + knownMatches);
    }
}
