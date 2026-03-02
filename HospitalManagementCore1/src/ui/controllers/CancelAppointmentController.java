package ui.controllers;

import Appointments.Appointment;
import Persons.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import ui.auth.Session;
import ui.models.Role;
import ui.services.HospitalService;

import java.util.List;

public class CancelAppointmentController implements ServiceAware {

    private HospitalService service;

    @FXML private Label lblLoggedIn;

    @FXML private Label lblPick;
    @FXML private ComboBox<Patient> cbPatient;

    @FXML private ListView<Appointment> lvAppointments;

    @FXML private Label lblInfo;

    @Override
    
    public void setService(HospitalService service) {
        this.service = service;

        boolean isAdmin = Session.getCurrent() != null
                && Session.getCurrent().getRole() == Role.ADMIN;

        if (isAdmin) {
            // 🔹 Admin: hasta seçmeli
            setPickerVisible(true);
            cbPatient.getItems().setAll(service.getPatients());

            // ✅ KRİTİK SATIR (eksik olan buydu)
            cbPatient.setOnAction(e -> onLoad());

            lblLoggedIn.setText("Logged in: ADMIN (select a patient)");
        } else {
            // 🔹 Patient: kendi randevularını görür
            setPickerVisible(false);

            Patient p = Session.getCurrentPatient();
            if (p == null) {
                info("Please login as a patient.");
                return;
            }

            lblLoggedIn.setText(
                    "Logged in patient: " + p.getName() + " "
                            + p.getSurname() + " (TC: " + p.getNationalId() + ")"
            );

            // Patient için otomatik yükle
            loadForPatient(p);
        }

        info("");
    }


    private void setPickerVisible(boolean show) {
        if (lblPick != null)   { lblPick.setVisible(show);   lblPick.setManaged(show); }
        if (cbPatient != null) { cbPatient.setVisible(show); cbPatient.setManaged(show); }
    }

    @FXML
    private void onLoad() {
        boolean isAdmin = Session.getCurrent() != null && Session.getCurrent().getRole() == Role.ADMIN;
        if (!isAdmin) return;

        Patient p = cbPatient == null ? null : cbPatient.getValue();
        if (p == null) { info("Please select a patient."); return; }

        lblLoggedIn.setText("Logged in patient: " + p.getName() + " " + p.getSurname() + " (TC: " + p.getNationalId() + ")");
        loadForPatient(p);
    }

    private void loadForPatient(Patient p) {
        if (service == null || p == null) return;

        List<Appointment> apps = service.getActiveAppointmentsForPatient(p);
        if (lvAppointments != null) lvAppointments.getItems().setAll(apps);

        info(apps.isEmpty() ? "No active appointments." : "Select an appointment to cancel.");
    }

    @FXML
    private void onCancel() {
        Appointment selected = (lvAppointments == null) ? null : lvAppointments.getSelectionModel().getSelectedItem();
        if (selected == null) { info("Please select an appointment."); return; }

        boolean ok = service.cancelAppointment(selected.getId());
        if (!ok) { info("Could not cancel."); return; }

        // reload
        boolean isAdmin = Session.getCurrent() != null && Session.getCurrent().getRole() == Role.ADMIN;
        Patient p = isAdmin ? (cbPatient == null ? null : cbPatient.getValue()) : Session.getCurrentPatient();
        loadForPatient(p);

        info("✅ Appointment cancelled.");
    }

    private void info(String msg) {
        if (lblInfo != null) lblInfo.setText(msg == null ? "" : msg);
    }
}
