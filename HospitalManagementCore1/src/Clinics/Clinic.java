package Clinics;

import Persons.Doctor;

import java.util.ArrayList;
import java.util.List;

public class Clinic {

    private final Department department;
    private final List<Doctor> doctors = new ArrayList<>();

    public Clinic(Department department) {
        this.department = department;
    }

    public Department getDepartment() {
        return department;
    }

    public List<Doctor> getDoctors() {
        return doctors;
    }

    public void addDoctor(Doctor doctor) {
        doctors.add(doctor);
    }

    @Override
    public String toString() {
        return department.getDisplayName() + " clinic (" + doctors.size() + " doctors)";
    }
}
