package ui.auth;

import ui.models.Role;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;

public class UserRepository {

    // ✅ Yazılabilir dosya yolu (proje klasörü içinde data/users.txt)
	private static final Path DATA_DIR = ui.AppPaths.DATA_DIR;
	private static final Path DATA_FILE = ui.AppPaths.USERS_FILE;
	public static final Path BASE_DIR = Paths.get(System.getProperty("user.home"), "HospitalManagementSystem");


    /**
     * Öncelik:
     * 1) data/users.txt (yazılabilir, register ile buraya ekleyeceğiz)
     * 2) resources/users.txt (fallback, read-only)
     */
    public UserAccount findByUsername(String username) {
        System.out.println("DATA_FILE = " + DATA_FILE.toAbsolutePath());

        if (username == null) return null;

        // ✅ GEREKLİ: normalize (boşluk/büyük-küçük harf problemi olmasın)
        username = username.trim();
        if (username.isEmpty()) return null;

        System.out.println("🔍 LOGIN SEARCH USER: " + username);
        System.out.println("📁 DATA FILE: " + DATA_FILE.toAbsolutePath());

        UserAccount fromData = findInDataFile(username);
        if (fromData != null) {
    
            System.out.println("✅ FOUND IN data/users.txt");
            return fromData;
        }

        UserAccount fromRes = findInResourceFile(username);
        if (fromRes != null) {
            System.out.println("⚠️ FOUND IN resources/users.txt");
            return fromRes;
        }

        System.out.println("❌ USER NOT FOUND ANYWHERE");
        return null;
    }

    private UserAccount findInDataFile(String username) {
        if (!Files.exists(DATA_FILE)) return null;

        try (BufferedReader br = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                UserAccount acc = parseLine(line, username);
                if (acc != null) return acc;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private UserAccount findInResourceFile(String username) {
        try (InputStream in = getClass().getResourceAsStream("/users.txt")) {
            if (in == null) {
                System.out.println("users.txt not found in resources!");
                return null;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    UserAccount acc = parseLine(line, username);
                    if (acc != null) return acc;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Satır parse (format: username;passwordHash;role;linkedTc)
    private UserAccount parseLine(String line, String username) {
        if (line == null) return null;
        line = line.replace("\uFEFF", "").trim();

        // boş / yorum
        if (line.isEmpty() || line.startsWith("#")) return null;

        // ✅ GEREKLİ: -1 -> linkedTc boş bile olsa array bozulmasın
        String[] p = line.split(";", -1);
        if (p.length < 3) return null;

        String u = p[0].replace("\uFEFF", "").trim();
        username = username.replace("\uFEFF", "").trim();
        String passHash = p[1].trim();

        // ✅ Role güvenli parse (patient / Patient / PATIENT hepsi olur)
        Role role;
        try {
            role = Role.valueOf(p[2].trim().toUpperCase());
        } catch (Exception ex) {
            System.out.println("Invalid role in users file line: " + line);
            return null;
        }

        String linkedTc = (p.length >= 4) ? p[3].trim() : "-";

        // ✅ GEREKLİ: büyük/küçük harf yüzünden login kaçmasın
        if (u.equalsIgnoreCase(username)) {
            return new UserAccount(u, passHash, role, linkedTc);
        }
        return null;
    }

    /**
     * ✅ Yeni kullanıcı ekler (data/users.txt içine append)
     * Format: username;passwordHash;role;linkedTc
     */
    public void add(UserAccount acc) {
        if (acc == null) return;

        System.out.println("🧾 ADD USER -> writing to: " + DATA_FILE.toAbsolutePath());

        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);

            // Aynı username var mı? (data + resource kontrol)
            if (findByUsername(acc.getUsername()) != null) {
                throw new IllegalArgumentException("User already exists: " + acc.getUsername());
            }

            String line =
                    acc.getUsername().trim() + ";" +
                    acc.getPasswordHash() + ";" +
                    acc.getRole().name() + ";" +
                    (acc.getLinkedTc() == null ? "-" : acc.getLinkedTc());

            try (BufferedWriter bw = Files.newBufferedWriter(
                    DATA_FILE,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                bw.write(line);
                bw.newLine();
                bw.flush(); // ✅ burada mantıklı
            }

            System.out.println("✅ User saved to data/users.txt");

        } catch (Exception e) {
            throw new RuntimeException("Could not save user: " + e.getMessage(), e);
        }
    }public void updatePassword(String username, String newPasswordHash) {
        try {
            if (username == null || username.isBlank())
                throw new IllegalArgumentException("username is empty");

            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);

            String uNorm = username.trim();

            java.util.List<String> lines = new java.util.ArrayList<>();
            if (Files.exists(DATA_FILE)) {
                lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
            }

            boolean updated = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank() || line.startsWith("#")) continue;

                String[] p = line.split(";", -1);
                if (p.length < 3) continue;

                if (p[0].trim().equalsIgnoreCase(uNorm)) {
                    String role = p[2].trim();
                    String linkedTc = (p.length >= 4) ? p[3].trim() : "-";
                    lines.set(i, uNorm + ";" + newPasswordHash + ";" + role + ";" + linkedTc);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                throw new IllegalArgumentException("User not found: " + uNorm);
            }

            Files.write(
                    DATA_FILE,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (Exception e) {
            throw new RuntimeException("Could not update password: " + e.getMessage(), e);
        }
    }
}
