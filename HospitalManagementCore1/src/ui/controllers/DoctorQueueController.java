package ui.controllers;

import Appointments.Appointment;
import Persons.Doctor;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import ui.auth.Session;
import ui.models.Role;
import ui.services.HospitalService;
import Appointments.AppointmentStatus;


import java.util.List;

public class DoctorQueueController implements ServiceAware {

    private HospitalService service;

    @FXML private Label lblLoggedIn;

    @FXML private Label lblPick;
    @FXML private ComboBox<Doctor> cbDoctor;

    @FXML private ListView<Appointment> lvQueue;

    @Override
    public void setService(HospitalService service) {
        this.service = service;

        boolean isAdmin = Session.getCurrent() != null && Session.getCurrent().getRole() == Role.ADMIN;
        boolean isDoctor = Session.getCurrent() != null && Session.getCurrent().getRole() == Role.DOCTOR;

        if (!isAdmin && !isDoctor) {
            if (lblLoggedIn != null) lblLoggedIn.setText("Please login as Doctor or Admin.");
            return;
        }

        cbDoctor.getItems().setAll(service.getAllDoctors());
        cbDoctor.setOnAction(e -> {
            Doctor d = cbDoctor.getValue();
            if (d != null) {
                lblLoggedIn.setText("Selected doctor: Dr. " + d.getName());
                loadForDoctor(d);
            }
        });


        if (isDoctor && !isAdmin) {
            setPickerVisible(false);
            Doctor d = Session.getCurrentDoctor();
            if (d == null) { lblLoggedIn.setText("Doctor session not found."); return; }
            lblLoggedIn.setText("Logged in doctor: Dr. " + d.getName());
            loadForDoctor(d);
        } else {
            setPickerVisible(true);
            lblLoggedIn.setText("Logged in: ADMIN (select a doctor)");

            // ✅ ilk doktoru otomatik seç
            if (!cbDoctor.getItems().isEmpty()) {
            	lblLoggedIn.setText("Logged in: ADMIN (select a doctor)");

            }
        }}

    private void setPickerVisible(boolean show) {
        if (lblPick != null)   { lblPick.setVisible(show);   lblPick.setManaged(show); }
        if (cbDoctor != null)  { cbDoctor.setVisible(show);  cbDoctor.setManaged(show); }
    }

    @FXML
    private void onLoad() {
        boolean isAdmin = Session.getCurrent() != null && Session.getCurrent().getRole() == Role.ADMIN;
        if (!isAdmin) return;

        Doctor d = cbDoctor == null ? null : cbDoctor.getValue();
        if (d == null) return;

        lblLoggedIn.setText("Selected doctor: Dr. " + d.getName());
        loadForDoctor(d);
    }

    private void loadForDoctor(Doctor d) {
    	System.out.println(
    		    "QUEUE doctor id=" + d.getId() +
    		    " | doctor obj=" + System.identityHashCode(d)
    		);

        List<Appointment> list = service.getAppointmentsForDoctor(d);

        List<Appointment> bookedOnly = list.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.BOOKED)
                .toList();

        if (lvQueue != null) {
            lvQueue.getItems().setAll(bookedOnly);
        }
    }
}

