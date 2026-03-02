package ui.auth;

import Persons.Doctor;
import Persons.Patient;

public class Session {

    private static UserAccount current;
    private static Doctor currentDoctor;
    private static Patient currentPatient;

    public static void setCurrent(UserAccount acc) {
        current = acc;
        currentDoctor = null;
        currentPatient = null;
    }

    public static UserAccount getCurrent() {
        return current;
    }

    public static void setCurrentDoctor(Doctor d) {
        currentDoctor = d;
    }

    public static Doctor getCurrentDoctor() {
        return currentDoctor;
    }

    public static void setCurrentPatient(Patient p) {
        currentPatient = p;
    }

    public static Patient getCurrentPatient() {
        return currentPatient;
    }

    public static void clear() {
        current = null;
        currentDoctor = null;
        currentPatient = null;
    }
}
