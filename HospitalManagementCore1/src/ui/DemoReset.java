package ui;

import java.nio.file.Files;
import java.nio.file.Path;

public class DemoReset {

    public static void resetOnStartup() {
        try {
            Path dir = AppPaths.DATA_DIR;
            if (!Files.exists(dir)) Files.createDirectories(dir);

            // demo’da sıfırlanacak dosyalar
            Files.deleteIfExists(AppPaths.USERS_FILE);
           
            // (opsiyonel) eğer doktor register da kaydediyorsa sıfırlayabilirsin
            // Files.deleteIfExists(AppPaths.DOCTORS_FILE);

            System.out.println("🧹 DEMO MODE: users.txt ve patients.txt sıfırlandı.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
