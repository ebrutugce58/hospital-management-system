package ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import Clinics.Department;



import ui.auth.PasswordUtil;
import ui.auth.UserAccount;
import ui.auth.UserRepository;
import ui.models.Role;
import ui.services.HospitalService;

public class RegisterDoctorController implements ServiceAware {

    private HospitalService service;

    @FXML private TextField txtDoctorId;
    @FXML private TextField txtName;
    @FXML private TextField txtSurname;
    @FXML private ComboBox<Department> cbDepartment;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblMsg;
    @FXML private TextField tfPasswordVisible;
    @FXML private TextField tfConfirmPasswordVisible;
    @FXML PasswordField pfConfirmPassword;
   
    
    @Override
    public void setService(HospitalService service) {
        this.service = service;
    }
    @FXML
  
    public void initialize() {
        if (cbDepartment != null) {
            cbDepartment.getItems().setAll(Department.values());
            cbDepartment.getSelectionModel().clearSelection();
            cbDepartment.setPromptText("Select department");
        }

        if (lblMsg != null) lblMsg.setText("");
    }

    
    @FXML
    private void onSave() {

        String doctorId = txtDoctorId.getText() == null ? "" : txtDoctorId.getText().trim().toUpperCase();
        String name     = txtName.getText()     == null ? "" : txtName.getText().trim();
        String surname  = txtSurname.getText()  == null ? "" : txtSurname.getText().trim();

        Department dep = cbDepartment.getValue(); // ComboBox<Department> olduğu için direkt Department

        String password = pfPassword.getText() == null ? "" : pfPassword.getText();
        String confirm  = pfConfirmPassword.getText() == null ? "" : pfConfirmPassword.getText();

        // 1) Boş kontrol
        if (doctorId.isEmpty() || name.isEmpty() || surname.isEmpty() || dep == null || password.isEmpty() || confirm.isEmpty()) {
            lblMsg.setText("Please fill all fields.");
            return;
        }

        // 2) ID format kontrol
        if (!doctorId.matches("^D\\d{3,6}$")) {
            lblMsg.setText("Doctor ID must look like D1001.");
            return;
        }

        // 3) Şifre eşleşme
        if (!password.equals(confirm)) {
            lblMsg.setText("Passwords do not match.");
            return;
        }

        // 4) Service kontrol
        if (service == null) {
            lblMsg.setText("Service not ready.");
            return;
        }

        // 5) UserRepository - login hesabı var mı?
        UserRepository repo = new UserRepository();
        if (repo.findByUsername(doctorId) != null) {
            lblMsg.setText("This Doctor ID already exists.");
            return;
        }

        // 6) users.txt’ye login hesabını yaz
        String hash = PasswordUtil.sha256(password);
        UserAccount acc = new UserAccount(doctorId, hash, Role.DOCTOR, "-");
        repo.add(acc);

        // 7) RAM’e doktoru ekle (uygulama açıkken hemen görünsün diye)
        service.addDoctorRuntime(doctorId, name, surname, dep);

        // 8) DEMO MODE değilse doctors.txt’ye de yaz
        if (!ui.AppConfig.DEMO_MODE) {
        	service.saveDoctorToDataFile(doctorId, name, surname, dep.toString());

        }

        lblMsg.setText("✅ Doctor saved. Now check Book Appointment.");

        // Temizle
        txtDoctorId.clear();
        txtName.clear();
        txtSurname.clear();
        pfPassword.clear();
        pfConfirmPassword.clear();
        cbDepartment.getSelectionModel().clearSelection();
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

