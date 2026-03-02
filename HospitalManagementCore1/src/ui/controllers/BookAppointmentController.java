package ui.controllers;

import Clinics.Department;
import Persons.Doctor;
import Persons.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ui.auth.Session;
import ui.services.HospitalService;

import java.time.LocalDate;
import java.util.List;

public class BookAppointmentController implements ServiceAware {

    private HospitalService service;

    @FXML private ComboBox<Department> cbDepartment;
    @FXML private ComboBox<Doctor> cbDoctor;

    // Admin için hasta seçimi (FXML'de de olmalı!)
    @FXML private ComboBox<Patient> cbPatient;

    @FXML private Label lblPatient; // "Patient:" yazan label
    @FXML private DatePicker dpDate;

    @FXML private ListView<HospitalService.Slot> lvSlots;

    @FXML private Label lblInfo;

    @Override
    public void setService(HospitalService service) {
        this.service = service;

        if (service == null) {
            info("Service not ready.");
            return;
        }

        // 1) Department yükle + değişince doctorları yükle
        if (cbDepartment != null) {
            cbDepartment.getItems().setAll(service.getDepartments());
            cbDepartment.setOnAction(e -> onDepartmentChanged());
        }

        // 2) İlk açılış temizliği
        if (cbDoctor != null) cbDoctor.getItems().clear();
        if (lvSlots != null) lvSlots.getItems().clear();

        // 3) Slot seçimi (tıklayınca seçilmeme sorununu çözen yer)
        setupSlotsSelection();

        // 4) Doctor seçilince otomatik slot yükle
        if (cbDoctor != null) {
            cbDoctor.setOnAction(e -> tryAutoLoadSlots());
        }

        // 5) Date seçilince otomatik slot yükle
        if (dpDate != null) {
            dpDate.valueProperty().addListener((obs, oldV, newV) -> tryAutoLoadSlots());
        }

        // 6) Patient/Admin modu
        Patient loggedPatient = Session.getCurrentPatient();

        if (loggedPatient != null) {
            // PATIENT login -> label + combobox tamamen gizle
            hidePatientUI();

            // patient için cbPatient'e dokunmaya gerek yok (book sırasında Session'dan alıyorsun)
            info("Logged in: " + loggedPatient.getName() + " " + loggedPatient.getSurname());

        } else {
            // ADMIN login -> label + combobox göster ve tüm hastaları yükle
            showPatientUI();

            if (cbPatient != null) {
                cbPatient.getItems().setAll(service.getPatients());
                cbPatient.setDisable(false);
            }

            info("Admin mode: select a patient to book.");
        }
    }

    private void setupSlotsSelection() {
        if (lvSlots == null) return;

        lvSlots.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Hücreleri saat gibi göster
        lvSlots.setCellFactory(list -> {
            ListCell<HospitalService.Slot> cell = new ListCell<>() {
                @Override
                protected void updateItem(HospitalService.Slot item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.time.toString());
                }
            };

            // tıklayınca seçimi manuel ver (bazı style/cell durumlarında seçilmiyor)
            cell.setOnMousePressed(e -> {
                if (!cell.isEmpty()) {
                    lvSlots.requestFocus();
                    lvSlots.getSelectionModel().select(cell.getIndex());
                }
            });

            return cell;
        });

        // seçim değişince bilgi yaz
        lvSlots.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                info("Selected time: " + newV.time);
            }
        });
    }

    private void hidePatientUI() {
        if (lblPatient != null) {
            lblPatient.setVisible(false);
            lblPatient.setManaged(false);
        }
        if (cbPatient != null) {
            cbPatient.setVisible(false);
            cbPatient.setManaged(false);
            cbPatient.setDisable(true);
        }
    }

    private void showPatientUI() {
        if (lblPatient != null) {
            lblPatient.setVisible(true);
            lblPatient.setManaged(true);
        }
        if (cbPatient != null) {
            cbPatient.setVisible(true);
            cbPatient.setManaged(true);
        }
    }

    private void onDepartmentChanged() {
        if (service == null) return;

        Department dep = cbDepartment == null ? null : cbDepartment.getValue();
        if (dep == null) return;

        List<Doctor> docs = service.getDoctorsByDepartment(dep);

        if (cbDoctor != null) {
            cbDoctor.getItems().setAll(docs);
            cbDoctor.getSelectionModel().clearSelection();

            // ✅ departman değişince doctor seçilince otomatik slot yüklesin
            cbDoctor.setOnAction(e -> tryAutoLoadSlots());
        }

    }

    @FXML
    private void onLoadSlots() {
        if (service == null) { info("Service not ready."); return; }

        Doctor doctor = cbDoctor == null ? null : cbDoctor.getValue();
        LocalDate date = dpDate == null ? null : dpDate.getValue();

        if (doctor == null) { info("Please select a doctor."); return; }
        if (date == null)   { info("Please select a date."); return; }

        List<HospitalService.Slot> slots = service.getAvailableSlotsForDoctor(doctor, date);

        if (lvSlots != null) {
            lvSlots.getItems().setAll(slots);
            lvSlots.getSelectionModel().clearSelection();
        }

        info(slots.isEmpty()
                ? "No available slots for this selection."
                : "Slots loaded. Select one time.");
    }

    @FXML
    private void onBook() {

        if (service == null) { info("Service not ready."); return; }

        // ✅ Patient belirleme
        Patient patient = Session.getCurrentPatient();
        if (patient == null) {
            patient = (cbPatient == null) ? null : cbPatient.getValue();
        }
        if (patient == null) {
            info("Please select a patient.");
            return;
        }

        Doctor doctor = (cbDoctor == null) ? null : cbDoctor.getValue();
        LocalDate date = (dpDate == null) ? null : dpDate.getValue();
        HospitalService.Slot slot = (lvSlots == null) ? null : lvSlots.getSelectionModel().getSelectedItem();

        if (doctor == null) { info("Please select a doctor."); return; }
        if (date == null)   { info("Please select a date."); return; }
        if (slot == null)   { info("Please select a time slot."); return; }

        try {
            service.bookAppointment(doctor, patient, date, slot.time);

            // booked olunca saatleri yenile
            onLoadSlots();

            info("✅ Appointment booked: " + date + " " + slot.time);

        } catch (Exception ex) {
            ex.printStackTrace();
            info("❌ Could not book: " + ex.getMessage());
        }
        System.out.println(
        	    "BOOK doctor id=" + doctor.getId() +
        	    " | doctor obj=" + System.identityHashCode(doctor)
        	);

    }

    // Eğer FXML'de onMouseClicked="#onSlotsClicked" yazdıysan hata vermesin diye duruyor
    @FXML
    private void onSlotsClicked() {
        // İstersen burada "tıklayınca slotları yükle" yapabilirsin:
        // onLoadSlots();
    }

    private void info(String msg) {
        if (lblInfo != null) lblInfo.setText(msg == null ? "" : msg);
    }
    
    private void tryAutoLoadSlots() {
        if (service == null) return;
        if (cbDoctor == null || dpDate == null) return;

        Doctor doctor = cbDoctor.getValue();
        LocalDate date = dpDate.getValue();

        // Doctor + Date ikisi de seçiliyse otomatik yükle
        if (doctor != null && date != null) {
            onLoadSlots();
        }
    }
}
