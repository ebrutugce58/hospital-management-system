package ui.controllers;

import Persons.Patient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ui.auth.LoginContext;
import ui.auth.Session;
import ui.auth.UserAccount;
import ui.auth.UserRepository;
import ui.models.Role;
import ui.services.HospitalService;

public class RegisterPatientController implements ServiceAware {

    private HospitalService service;

    @FXML private TextField tfNationalId;
    @FXML private TextField tfName;
    @FXML private TextField tfSurname;
    @FXML private TextField tfPhone;

    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmPassword;

    @FXML private Label lblRegisteredPatients;
    @FXML private ListView<Patient> lvPatients;
    @FXML private TextField tfPasswordVisible;
    @FXML private TextField tfConfirmPasswordVisible;


    // ✅ EKLENDİ: FXML’de fx:id vermen lazım
    @FXML private Button btnBack;
    @FXML private Button btnGoLogin;

    private final UserRepository userRepo = new UserRepository();

    private boolean isPatientRegisterPortal() {
        return LoginContext.portalName != null
                && LoginContext.portalName.equalsIgnoreCase("Patient Register");
    }

    private boolean isAdminPanel() {
        return Session.getCurrent() != null && Session.getCurrent().getRole() == Role.ADMIN;
    }

    @FXML
    public void initialize() {
        // show/hide sync
        if (pfPassword != null && tfPasswordVisible != null) {
            tfPasswordVisible.textProperty().bindBidirectional(pfPassword.textProperty());
        }
        if (pfConfirmPassword != null && tfConfirmPasswordVisible != null) {
            tfConfirmPasswordVisible.textProperty().bindBidirectional(pfConfirmPassword.textProperty());
        }
    }


    @Override
    public void setService(HospitalService service) {
        this.service = service;

        // ✅ 1) MOD BELİRLE
        boolean patientPortal = isPatientRegisterPortal();
        boolean adminPanel = isAdminPanel();

        // ✅ 2) Patient portal: hasta listesi gizli, Back/Login butonları açık
        if (patientPortal) {
            if (lvPatients != null) { lvPatients.setVisible(false); lvPatients.setManaged(false); }
            if (lblRegisteredPatients != null) { lblRegisteredPatients.setVisible(false); lblRegisteredPatients.setManaged(false); }

            if (btnBack != null) { btnBack.setVisible(true); btnBack.setManaged(true); }
            if (btnGoLogin != null) { btnGoLogin.setVisible(true); btnGoLogin.setManaged(true); }
        }

        // ✅ 3) Admin panel: Back/Login butonları gizli, hasta listesi açık ve dolu
        else if (adminPanel) {
            if (btnBack != null) { btnBack.setVisible(false); btnBack.setManaged(false); }
            if (btnGoLogin != null) { btnGoLogin.setVisible(false); btnGoLogin.setManaged(false); }

            if (lvPatients != null) { lvPatients.setVisible(true); lvPatients.setManaged(true); }
            if (lblRegisteredPatients != null) { lblRegisteredPatients.setVisible(true); lblRegisteredPatients.setManaged(true); }

            if (lvPatients != null) {
                lvPatients.getItems().setAll(service.getPatients());
            }
        }

        // ✅ 4) Diğer durumlar (ör: portal_select'ten açıldı ama admin değil)
        // varsayılan: Back/Login açık kalsın, listeyi göstermeyelim
        else {
            if (lvPatients != null) { lvPatients.setVisible(false); lvPatients.setManaged(false); }
            if (lblRegisteredPatients != null) { lblRegisteredPatients.setVisible(false); lblRegisteredPatients.setManaged(false); }

            if (btnBack != null) { btnBack.setVisible(true); btnBack.setManaged(true); }
            if (btnGoLogin != null) { btnGoLogin.setVisible(true); btnGoLogin.setManaged(true); }
        }
    }

    @FXML
    private void onRegister() {
        if (service == null) {
            alert("Error", "Service not ready.");
            return;
        }

        String id = tfNationalId.getText() == null ? "" : tfNationalId.getText().trim();
        String name = tfName.getText() == null ? "" : tfName.getText().trim();
        String surname = tfSurname.getText() == null ? "" : tfSurname.getText().trim();
        String phone = tfPhone.getText() == null ? "" : tfPhone.getText().trim();

        String pass1 = pfPassword.getText() == null ? "" : pfPassword.getText();
        String pass2 = pfConfirmPassword.getText() == null ? "" : pfConfirmPassword.getText();

        if (!id.matches("\\d{11}")) {
            alert("Invalid ID", "National ID must be 11 digits.");
            return;
        }
        if (name.isEmpty() || surname.isEmpty()) {
            alert("Missing Info", "Name and Surname cannot be empty.");
            return;
        }
        if (pass1.isEmpty() || pass2.isEmpty()) {
            alert("Missing Password", "Please enter password twice.");
            return;
        }
        if (!pass1.equals(pass2)) {
            alert("Password Mismatch", "Passwords do not match.");
            return;
        }
        if (pass1.length() < 6) {
            alert("Weak Password", "Password must be at least 6 characters.");
            return;
        }

        if (userRepo.findByUsername(id) != null) {
            alert("Already Exists", "This National ID already has an account.");
            return;
        }

        Patient p;
        try {
            p = service.registerPatient(id, name, surname, phone.isEmpty() ? "N/A" : phone);
        } catch (Exception ex) {
            ex.printStackTrace();
            alert("Registration Error", ex.getMessage());
            return;
        }

        String hash = ui.auth.PasswordUtil.sha256(pass1);
        UserAccount acc = new UserAccount(id, hash, Role.PATIENT, id);

        try {
            userRepo.add(acc);
        } catch (IllegalArgumentException dup) {
            alert("Already Exists", dup.getMessage());
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            alert("Save Error", "Could not save user account: " + ex.getMessage());
            return;
        }

        alert("Success", "Registered: " + p.getInfo() + "\nNow you can login with your National ID.");

        // ✅ Admin paneldeyken listeyi güncelle
        if (isAdminPanel() && lvPatients != null) {
            lvPatients.getItems().setAll(service.getPatients());
        }

        tfNationalId.clear();
        tfName.clear();
        tfSurname.clear();
        tfPhone.clear();
        pfPassword.clear();
        pfConfirmPassword.clear();
    }

    @FXML
    private void onBackMenu() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
        Scene scene = new Scene(loader.load(), 750, 520);

        MainMenuController c = loader.getController();
        c.setService(service);

        Stage stage = (Stage) tfName.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Hospital Management System");
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void onBack() {
        ShellController shell = ShellController.getInstance();
        if (shell != null) {
            shell.setSidebarVisible(false);
            shell.loadPage("portal_select.fxml");
        }
    }

    @FXML
    private void onGoLogin() {
        LoginContext.portalName = "Patient / Doctor Login";
        LoginContext.allowedRoles = new String[]{"PATIENT", "DOCTOR"};

        ShellController shell = ShellController.getInstance();
        if (shell != null) {
            shell.setSidebarVisible(false);
            shell.loadPage("login.fxml");
        }
    }
    @FXML
    private void togglePassword() {
        toggle(pfPassword, tfPasswordVisible);
    }

    @FXML
    private void toggleConfirmPassword() {
        toggle(pfConfirmPassword, tfConfirmPasswordVisible);
    }

    private void toggle(PasswordField pf, TextField tf) {
        boolean show = !tf.isVisible();

        tf.setVisible(show);
        tf.setManaged(show);

        pf.setVisible(!show);
        pf.setManaged(!show);

        if (show) tf.requestFocus();
        else pf.requestFocus();
    }

}


