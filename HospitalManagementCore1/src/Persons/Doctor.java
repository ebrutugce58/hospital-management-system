package Persons;

import Clinics.Department;

public class Doctor extends Person {

    private Department department;

    public Doctor(int id, String name, Department department) {
        super(id, name);
        this.department = department;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public String getInfo() {
        return "Dr. " + getName() + " (" + department.getDisplayName() + ")";
    }

    @Override
    public String toString() {
        return getInfo();
    }
}
