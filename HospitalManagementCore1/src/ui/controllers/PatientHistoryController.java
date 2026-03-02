package ui.controllers;

import Appointments.Appointment;
import Appointments.AppointmentStatus;
import History.History;
import Persons.Doctor;
import Persons.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import ui.auth.Session;
import ui.models.Role;
import ui.services.HospitalService;

import java.util.ArrayList;
import java.util.List;

public class PatientHistoryController implements ServiceAware {

    private HospitalService service;

    @FXML private Label lblLoggedIn;

    @FXML private HBox rowSelectPatient;
    @FXML private ComboBox<Patient> cbPatient;

    @FXML private ListView<String> lvDiagnosis;
    @FXML private ListView<String> lvBooked;
    @FXML private ListView<String> lvCancelled;

    @FXML private Label lblInfo;

    private boolean listenerBound = false;

    @Override
    public void setService(HospitalService service) {
        this.service = service;

        // UI reset
        clearLists();
        info("");
        if (lblLoggedIn != null) lblLoggedIn.setText("");

        if (Session.getCurrent() == null) {
            hidePickerRow();
            if (lblLoggedIn != null) lblLoggedIn.setText("Logged in: -");
            info("Please login.");
            return;
        }

        Role role = Session.getCurrent().getRole();
        System.out.println("✅ Session role=" + role);
        System.out.println("🧪 service.getPatients size = " + service.getPatients().size());

        // Listener'ı SADECE 1 kez bağla
        bindComboListenerOnce();

        // ROLE switch
        if (role == Role.PATIENT) {
            hidePickerRow();

            Patient p = Session.getCurrentPatient();
            if (p == null) {
                if (lblLoggedIn != null) lblLoggedIn.setText("Logged in patient: -");
                info("Please login as a patient.");
                return;
            }

            if (lblLoggedIn != null) {
                lblLoggedIn.setText("Logged in patient: " + p.getName() + " " + p.getSurname()
                        + " (TC: " + p.getNationalId() + ")");
            }

            loadForPatient(p);
            info("Loaded history for logged-in patient.");
            return;
        }

        if (role == Role.DOCTOR) {
            showPickerRow();

            Doctor d = Session.getCurrentDoctor();
            if (d == null) {
                hidePickerRow();
                if (lblLoggedIn != null) lblLoggedIn.setText("Logged in doctor: -");
                info("Please login as a doctor.");
                return;
            }

            if (lblLoggedIn != null) lblLoggedIn.setText("Logged in: Dr. " + d.getName());

            List<Patient> patients = service.getPatientsForDoctorAll(d);
            cbPatient.getItems().setAll(patients);

            if (!patients.isEmpty()) {
                cbPatient.getSelectionModel().selectFirst(); // listener loadForPatient çağıracak
                info("Select a patient from the list.");
            } else {
                clearLists();
                info("No patients found for this doctor yet.");
            }
            return;
        }

        if (role == Role.ADMIN) {
            showPickerRow();

            if (lblLoggedIn != null) lblLoggedIn.setText("Logged in: ADMIN");

            List<Patient> allPatients = service.getPatients(); // sende bu var
            System.out.println("✅ Admin allPatients size = " + allPatients.size());

            cbPatient.getItems().setAll(allPatients);

            if (!allPatients.isEmpty()) {
                cbPatient.getSelectionModel().selectFirst(); // listener loadForPatient çağıracak
                info("Select a patient from the list.");
            } else {
                clearLists();
                info("No patients found in the system.");
            }
            return;
        }

        // other roles
        hidePickerRow();
        if (lblLoggedIn != null) lblLoggedIn.setText("Logged in: " + role);
        info("This page is only for Patient / Doctor / Admin.");
    }

    private void bindComboListenerOnce() {
        if (cbPatient == null || listenerBound) return;

        listenerBound = true;

        cbPatient.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            if (newP != null) {
                loadForPatient(newP);
                info("Loaded history for selected patient.");
            }
        });
    }

    private void hidePickerRow() {
        if (rowSelectPatient != null) {
            rowSelectPatient.setVisible(false);
            rowSelectPatient.setManaged(false);
        }
        if (cbPatient != null) cbPatient.getItems().clear();
    }

    private void showPickerRow() {
        if (rowSelectPatient != null) {
            rowSelectPatient.setVisible(true);
            rowSelectPatient.setManaged(true);
        }
    }

    private void clearLists() {
        if (lvDiagnosis != null) lvDiagnosis.getItems().clear();
        if (lvBooked != null) lvBooked.getItems().clear();
        if (lvCancelled != null) lvCancelled.getItems().clear();
    }

    private void info(String msg) {
        if (lblInfo != null) lblInfo.setText(msg == null ? "" : msg);
    }

    private void loadForPatient(Patient patient) {
        clearLists();
        if (patient == null || service == null) return;

        // Diagnosis history
        List<History> diag = service.getDiagnosisHistory(patient);
        if (diag != null) {
            for (History h : diag) {
                if (h != null) lvDiagnosis.getItems().add(h.toString());
            }
        }

        // Appointment history (booked & cancelled)
        List<Appointment> all = service.getPatientAppointmentHistoryAll(patient);

        List<String> booked = new ArrayList<>();
        List<String> cancelled = new ArrayList<>();

        if (all != null) {
            for (Appointment a : all) {
                if (a == null) continue;

                if (a.getStatus() == AppointmentStatus.BOOKED) booked.add(a.toString());
                if (a.getStatus() == AppointmentStatus.CANCELLED) cancelled.add(a.toString());
            }
        }

        lvBooked.getItems().setAll(booked);
        lvCancelled.getItems().setAll(cancelled);
    }
}
