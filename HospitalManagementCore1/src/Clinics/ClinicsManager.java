package Clinics;

import Persons.Doctor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClinicsManager {

    private final Map<Department, Clinic> clinicMap = new EnumMap<>(Department.class);

    public ClinicsManager(List<Doctor> allDoctors) {
        // Her bölüm için bir Clinic oluştur
        for (Department d : Department.values()) {
            clinicMap.put(d, new Clinic(d));
        }
        // Doktorları ait oldukları kliniğe ekle
        for (Doctor doctor : allDoctors) {
            Department dep = doctor.getDepartment();
            Clinic c = clinicMap.get(dep);
            if (c != null) {
                c.addDoctor(doctor);
            }
        }
    }

    public List<Clinic> getAllClinics() {
        return new ArrayList<>(clinicMap.values());
    }

    public Clinic getClinic(Department department) {
        return clinicMap.get(department);
    }
}
