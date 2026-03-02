package ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ui.services.HospitalService;

public class MainMenuController {

    private HospitalService service;

    @FXML private Label lblInfo;

    public void setService(HospitalService service) {
        this.service = service;
        lblInfo.setText("Loaded: " + service.getPatients().size() + " patients, "
                + service.getAllDoctors().size() + " doctors.");
    }

    @FXML
    private void goRegister() throws Exception {
        switchScene("/register_patient.fxml", "Register Patient");
    }

    @FXML
    private void goBook() throws Exception {
        switchScene("/book_appointment.fxml", "Book Appointment");
    }

    @FXML
    private void goCancel() throws Exception {
        switchScene("/cancel_appointment.fxml", "Cancel Appointment");
    }

    @FXML
    private void goDoctorQueue() throws Exception {
        switchScene("/doctor_queue.fxml", "Doctor Queue");
    }

    @FXML
    private void goHistory() throws Exception {
        switchScene("/patient_history.fxml", "Patient History");
    }

    private void switchScene(String fxmlPath, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load(), 750, 520);

        // Açılan controller'a service gönder
        Object controller = loader.getController();

        // Hepsinde setService(service) olduğu varsayımıyla:
        if (controller instanceof ServiceAware sa) {
            sa.setService(service);
        }

        Stage stage = (Stage) lblInfo.getScene().getWindow();
        stage.setTitle(title);
        stage.setScene(scene);
    }
}
