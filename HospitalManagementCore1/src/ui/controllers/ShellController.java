package ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import ui.auth.LoginContext;
import ui.auth.Session;
import ui.models.Role;
import ui.services.HospitalService;

public class ShellController {

    private HospitalService service;

    @FXML private StackPane contentArea;
    @FXML private Label lblStatus;
    @FXML private Pane sidebar;

    @FXML private Button btnRegister;
    @FXML private Button btnBook;
    @FXML private Button btnCancel;
    @FXML private Button btnQueue;
    @FXML private Button btnHistory;

    @FXML private Button btnLogout;

    private static ShellController instance;

    @FXML
    public void initialize() {
        instance = this;

        setSidebarVisible(false);
        setAllMenuButtonsVisible(false);

        if (lblStatus != null) lblStatus.setText("Not logged in.");
        // burada showLogin() çağırma yok (portal_select akışını bozmasın)
    }

    public static ShellController getInstance() {
        return instance;
    }

    public void setService(HospitalService service) {
        this.service = service;

        setSidebarVisible(false);
        setAllMenuButtonsVisible(false);

        if (lblStatus != null) lblStatus.setText("Ready.");
    }

    public void setSidebarVisible(boolean visible) {
        if (sidebar == null) return;
        sidebar.setVisible(visible);
        sidebar.setManaged(visible);
    }

    // ✅ EKLENDİ: service guard (Patient/Doctor login hatası için)
    private boolean ensureServiceReady() {
        if (service != null) return true;
        if (lblStatus != null) lblStatus.setText("Service not ready. Check app initialization.");
        System.out.println("❌ HospitalService is null in ShellController.");
        return false;
    }

    // ✅ Login sonrası çağır
    public void applyRoleMenu() {
        setAllMenuButtonsVisible(false);

        if (Session.getCurrent() == null) return;

        Role role = Session.getCurrent().getRole();

        if (role == Role.ADMIN) {
            setButtonVisible(btnRegister, true); // artık Doctor Register
            setButtonVisible(btnBook, true);
            setButtonVisible(btnCancel, true);
            setButtonVisible(btnQueue, true);
            setButtonVisible(btnHistory, true);


        } else if (role == Role.DOCTOR) {
            // Doktor: Queue + History
            setButtonVisible(btnQueue, true);
            setButtonVisible(btnHistory, true);

        } else if (role == Role.PATIENT) {
            // Patient: Book + Cancel + History
            setButtonVisible(btnBook, true);
            setButtonVisible(btnCancel, true);
            setButtonVisible(btnHistory, true);
        }

        // Logout her rolde görünür
        setButtonVisible(btnLogout, true);
    }

    public void updateLoginStatusText() {
        if (lblStatus == null) return;

        if (Session.getCurrent() == null) {
            lblStatus.setText("Not logged in.");
            return;
        }

        Role role = Session.getCurrent().getRole();

        if (role == Role.DOCTOR && Session.getCurrentDoctor() != null) {
            lblStatus.setText("Logged in: Dr. " + Session.getCurrentDoctor().getName());
            return;
        }

        if (role == Role.PATIENT && Session.getCurrentPatient() != null) {
            lblStatus.setText("Logged in: " + Session.getCurrentPatient().getName()
                    + " " + Session.getCurrentPatient().getSurname());
            return;
        }

        lblStatus.setText("Logged in: " + Session.getCurrent().getUsername() + " (" + role + ")");
    }

    private void setAllMenuButtonsVisible(boolean visible) {
        setButtonVisible(btnRegister, visible);
        setButtonVisible(btnBook, visible);
        setButtonVisible(btnCancel, visible);
        setButtonVisible(btnQueue, visible);
        setButtonVisible(btnHistory, visible);
        setButtonVisible(btnLogout, visible);
    }

    private void setButtonVisible(Button b, boolean visible) {
        if (b == null) return;
        b.setVisible(visible);
        b.setManaged(visible);
    }

    // ✅ resources kökünden yükleme
    public void loadPage(String fxmlName) {
        if (contentArea == null) {
            System.out.println("❌ contentArea is null. Check shell.fxml fx:id=\"contentArea\"");
            return;
        }

        String path = fxmlName.startsWith("/") ? fxmlName : "/" + fxmlName;

        try {
            System.out.println("➡️ loadPage: " + path);

            var url = getClass().getResource(path);
            if (url == null) {
                throw new IllegalStateException("FXML not found on classpath: " + path);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ServiceAware sa && service != null) {
                sa.setService(service);
            }

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
            if (lblStatus != null) lblStatus.setText("Error loading: " + path);
        }
    }

    public void showLogin() {
        if (!ensureServiceReady()) return;

        LoginContext.portalName = "Patient / Doctor Login";
        // ✅ DÜZELTİLDİ: Admin buradan girmesin (Admin Login ayrı portal)
        LoginContext.allowedRoles = new String[]{"PATIENT", "DOCTOR"};

        loadPage("login.fxml");

        setSidebarVisible(false);
        setAllMenuButtonsVisible(false);
        updateLoginStatusText();
    }

    public void showPortalSelect() {
        LoginContext.portalName = "Select Portal";
        LoginContext.allowedRoles = null;

        loadPage("portal_select.fxml");

        setSidebarVisible(false);
        setAllMenuButtonsVisible(false);
        updateLoginStatusText();
    }

    // ✅ Role guard
    private boolean requireLogin(Role... allowed) {
        if (Session.getCurrent() == null) {
            showLogin();
            if (lblStatus != null) lblStatus.setText("Please login first.");
            return false;
        }

        Role role = Session.getCurrent().getRole();

        // Admin her şeye girebilsin
        if (role == Role.ADMIN) return true;

        if (allowed == null || allowed.length == 0) return true;

        for (Role r : allowed) {
            if (role == r) return true;
        }

        if (lblStatus != null) lblStatus.setText("Access denied for role: " + role);
        return false;
    }

    // ✅ Logout
    @FXML
    private void onLogout() {
        Session.clear();
        if (lblStatus != null) lblStatus.setText("Not logged in.");
        showPortalSelect();
    }

    // Menü actionları
    @FXML
    public void showRegister() {
        if (!ensureServiceReady()) return;
        // ✅ DÜZELTİLDİ: sidebar'ı burada zorla kapatma (admin menüden tıklarsa kaybolmasın)
        loadPage("register_patient.fxml");
    }

    @FXML
    public void showBook() {
        if (!requireLogin(Role.PATIENT)) return;
        loadPage("book_appointment.fxml");
    }

    @FXML
    public void showCancel() {
        if (!requireLogin(Role.PATIENT)) return;
        loadPage("cancel_appointment.fxml");
    }

    @FXML
    public void showQueue() {
        if (!requireLogin(Role.DOCTOR)) return;
        loadPage("doctor_queue.fxml");
    }

    @FXML
    public void showHistory() {
        if (!requireLogin(Role.PATIENT, Role.DOCTOR)) return;
        loadPage("patient_history.fxml");
    }
 // ✅ PortalSelect için TEK doğru giriş noktası
    public void openLoginPortal(String title, String[] allowedRoles) {
        if (service == null) {
            if (lblStatus != null) lblStatus.setText("Service not ready. Restart app.");
            System.out.println("❌ HospitalService is null. Login cannot open.");
            return;
        }

        LoginContext.portalName = title;
        LoginContext.allowedRoles = allowedRoles;

        setSidebarVisible(false);
        setAllMenuButtonsVisible(false);

        loadPage("login.fxml");
        updateLoginStatusText();
    }

  
    public void openRegisterPortal() {
        if (service == null) {
            if (lblStatus != null) lblStatus.setText("Service not ready.");
            System.out.println("❌ openRegisterPortal: service is null");
            return;
        }

        LoginContext.portalName = "Patient Register";
        LoginContext.allowedRoles = null;

        setSidebarVisible(false);
        setAllMenuButtonsVisible(false);

        loadPage("register_patient.fxml");
    }
    @FXML
    public void showDoctorRegister() {
        // Sadece admin girebilsin
        if (!requireLogin(Role.ADMIN)) return;

        loadPage("register_doctor.fxml");
    }
    @FXML
    public void showChangePassword() {
        if (!requireLogin(Role.PATIENT, Role.DOCTOR)) return; // admin de otomatik geçer
        loadPage("change_password.fxml");
    }
 
}
