package Persons;

import Appointments.Appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// Hastanın randevu geçmişini Stack ile tutuyoruz (LIFO)
public class Patient extends Person {

    private final String nationalId;  // 11 haneli TC
    private String surname;
    private String phone;

    private final Stack<Appointment> historyStack = new Stack<>();

    public Patient(int internalId,
                   String nationalId,
                   String name,
                   String surname,
                   String phone) {
        super(internalId, name);
        this.nationalId = nationalId;
        this.surname = surname;
        this.phone = phone;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void addToHistory(Appointment appointment) {
        historyStack.push(appointment);
    }

    public List<Appointment> getHistory() {
        return new ArrayList<>(historyStack);
    }

    @Override
    public String getInfo() {
        return "Patient: " + getName() + " " + surname +
                " | TC: " + nationalId +
                " | Phone: " + phone;
    }

  

    // HashMap<Patient,...> için gerekli
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;
        Patient p = (Patient) o;
        return this.nationalId != null && this.nationalId.equals(p.nationalId);
    }

    @Override
    public int hashCode() {
        return nationalId == null ? 0 : nationalId.hashCode();
    }
    @Override
    public String toString() {
        return getName() + " " + getSurname() + " (TC: " + getNationalId() + ")";
    }

}

