package ui.controllers;

import Persons.Doctor;
import Persons.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ui.auth.AuthService;
import ui.auth.LoginContext;
import ui.auth.Session;
import ui.auth.UserAccount;
import ui.auth.UserRepository;
import ui.models.Role;
import ui.services.HospitalService;

public class LoginController implements ServiceAware {

    private HospitalService service;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Label lblTitle;
    @FXML private TextField txtPasswordVisible;


    // (Varsa) register butonunu admin portalında gizlemek için:
    @FXML private Button btnRegister;

    private final AuthService authService = new AuthService(new UserRepository());

    @Override
    public void setService(HospitalService service) {
        this.service = service;
    }

    @FXML
    public void initialize() {
        if (lblTitle != null) {
            lblTitle.setText(LoginContext.portalName);
        }

        txtUsername.textProperty().addListener((o, a, b) -> clearError());
        txtPassword.textProperty().addListener((o, a, b) -> clearError());

        // ✅ show/hide password: iki alanı senkronla
        if (txtPasswordVisible != null && txtPassword != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }
    }


    @FXML
    private void onLogin() {
        clearError();

        String username = (txtUsername.getText() == null) ? "" : txtUsername.getText().trim();
        String password = (txtPassword.getText() == null) ? "" : txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username/TC/DoctorID and password.");
            return;
        }

        if (service == null) {
            showError("Service is not ready (HospitalService null).");
            return;
        }
        System.out.println("LOGIN service=" + System.identityHashCode(service)
        + " | doctorsSize=" + service.getAllDoctors().size());


        UserAccount acc;
        try {
            acc = authService.authenticate(username, password);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Auth error: " + ex.getMessage());
            return;
        }

        if (acc == null) {
            showError("Invalid credentials.");
            return;
        }

        if (!isAllowed(acc.getRole())) {
            showError("This account cannot login from this portal.");
            return;
        }

        Session.setCurrent(acc);

        if (acc.getRole() == Role.DOCTOR) {

            String doctorId = acc.getUsername().trim().toUpperCase();
            if (!doctorId.startsWith("D")) {
                doctorId = "D" + doctorId;
            }

            Doctor d = service.findDoctorByDoctorId(doctorId);

            if (d == null) {
                showError("Doctor not found: " + doctorId + " (not in doctors file)");
                Session.clear();
                return;
            }

            Session.setCurrentDoctor(d);
        }


        if (acc.getRole() == Role.PATIENT) {
            String tc = acc.getLinkedTc();
            if (tc == null || tc.isBlank()) tc = acc.getUsername();
            Patient p = service.findPatientByNationalId(tc);
            if (p == null) {
                showError("Patient not found for TC: " + tc);
                Session.clear();
                return;
            }
            Session.setCurrentPatient(p);
        }

        ShellController shell = ShellController.getInstance();
        if (shell == null) {
            showError("ShellController instance is null.");
            Session.clear();
            return;
        }

        shell.setSidebarVisible(true);
        shell.updateLoginStatusText();

        // ✅ Admin artık register'a gitmez
        if (acc.getRole() == Role.ADMIN) {
        	if (acc.getRole() == Role.ADMIN) {
        	    shell.showDoctorRegister();   // ✅ admin girince direkt Register Doctor açılsın
        	}
// sende var dedin
        } else if (acc.getRole() == Role.DOCTOR) {
            shell.showQueue();
        } else {
            shell.showBook();
        }

        shell.applyRoleMenu();
    }

    // ✅ FXML’de varsa diye garanti handler’lar (FXMLLoader patlamasın diye)
    @FXML private void onBack() {
        ShellController shell = ShellController.getInstance();
        if (shell != null) shell.showPortalSelect();
    }

    @FXML private void onOpenPatientRegister() {
        LoginContext.portalName = "Patient Register";
        LoginContext.allowedRoles = null;

        ShellController shell = ShellController.getInstance();
        if (shell != null) {
            shell.setSidebarVisible(false);
            shell.loadPage("register_patient.fxml");
        }
    }

    // Bazı FXML'lerde farklı isimle çağrılıyor olabiliyor -> güvenli alias
    @FXML private void onRegister() { onOpenPatientRegister(); }
    @FXML private void onOpenRegister() { onOpenPatientRegister(); }

    private boolean isAllowed(Role role) {
        if (LoginContext.allowedRoles == null || LoginContext.allowedRoles.length == 0) return true;
        for (String r : LoginContext.allowedRoles) {
            if (r != null && r.equalsIgnoreCase(role.name())) return true;
        }
        return false;
    }

    private void showError(String msg) {
        if (lblError != null) lblError.setText(msg);
    }

    private void clearError() {
        if (lblError != null) lblError.setText("");
    }
    private Doctor loadDoctorFromFiles(String doctorId) {
        // 1) data/doctors.txt
        Doctor d = loadDoctorFromPath(java.nio.file.Paths.get("data", "doctors.txt"), doctorId);
        if (d != null) return d;

        // 2) resources/doctors.txt
        try (java.io.InputStream in = getClass().getResourceAsStream("/doctors.txt")) {
            if (in == null) return null;
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                return loadDoctorFromReader(br, doctorId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Doctor loadDoctorFromPath(java.nio.file.Path path, String doctorId) {
        try {
            if (!java.nio.file.Files.exists(path)) return null;
            try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(
                    path, java.nio.charset.StandardCharsets.UTF_8)) {
                return loadDoctorFromReader(br, doctorId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Doctor loadDoctorFromReader(java.io.BufferedReader br, String doctorId) throws Exception {

        // ✅ login id temizliği (BOM + boşluk/sekme)
        doctorId = (doctorId == null) ? "" : doctorId;
        doctorId = doctorId.replace("\uFEFF", "").trim().toUpperCase();
        doctorId = doctorId.replaceAll("\\s+", "");
        if (!doctorId.startsWith("D")) doctorId = "D" + doctorId;

        String line;
        while ((line = br.readLine()) != null) {

            String t = (line == null) ? "" : line;
            t = t.replace("\uFEFF", "").trim();   // ✅ satır BOM temizle
            if (t.isEmpty() || t.startsWith("#")) continue;

            String delim = detectDelim(t);
            String[] p = (delim == null) ? new String[]{t} : t.split(java.util.regex.Pattern.quote(delim), -1);

            if (p.length < 1) continue;

            // ✅ dosyadaki id temizliği (BOM + boşluk/sekme)
            String id = (p[0] == null) ? "" : p[0];
            id = id.replace("\uFEFF", "").trim().toUpperCase();
            id = id.replaceAll("\\s+", "");
            if (!id.startsWith("D")) id = "D" + id;

            // DEBUG (istersen sonra silersin)
            // System.out.println("FILE ID=[" + id + "] LOGIN ID=[" + doctorId + "]");

            if (!id.equalsIgnoreCase(doctorId)) continue;

            String name = (p.length >= 2 && p[1] != null) ? p[1].trim() : "";
            String surname = (p.length >= 3 && p[2] != null) ? p[2].trim() : "";
            String dept = (p.length >= 4 && p[3] != null) ? p[3].trim() : "";

            Doctor d = buildDoctorByReflection(id, name, surname, dept);
            return d; // bulduk (d null ise null döner)
        }

        return null;
    }
    private String detectDelim(String s) {
        if (s == null) return null;
        if (s.contains(";")) return ";";
        if (s.contains(",")) return ",";
        if (s.contains("|")) return "|";
        if (s.contains("\t")) return "\t";
        return null;
    }

    private Doctor buildDoctorByReflection(String id, String name, String surname, String dept) {
        try {
            java.lang.reflect.Constructor<?>[] cons = Persons.Doctor.class.getConstructors();

            // En çok kullanılan ihtimaller: (String,String,String,String) / (String,String,String)
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 4 && allString(t)) {
                    return (Persons.Doctor) c.newInstance(id, name, surname, dept);
                }
            }
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 3 && allString(t)) {
                    // bazı projelerde surname yok: (id,name,dept) veya (id,name,surname)
                    // dept boşsa surname'ü kullan, değilse dept'ü kullan
                    String third = !dept.isBlank() ? dept : surname;
                    return (Persons.Doctor) c.newInstance(id, name, third);
                }
            }
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 2 && allString(t)) {
                    return (Persons.Doctor) c.newInstance(id, name);
                }
            }
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 1 && allString(t)) {
                    return (Persons.Doctor) c.newInstance(id);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean allString(Class<?>[] types) {
        for (Class<?> c : types) {
            if (!String.class.equals(c)) return false;
        }
        return true;
    }
    @FXML
    private void togglePassword() {
        boolean show = !txtPasswordVisible.isVisible();

        txtPasswordVisible.setVisible(show);
        txtPasswordVisible.setManaged(show);

        txtPassword.setVisible(!show);
        txtPassword.setManaged(!show);

        if (show) txtPasswordVisible.requestFocus();
        else txtPassword.requestFocus();
    }


}

