package Repository;

import Persons.Doctor;

public class DoctorRepository {

    private static class Node {
        Doctor doctor;
        Node left, right;
        Node(Doctor doctor) { this.doctor = doctor; }
    }

    private Node root;

    public void add(Doctor doctor) {
        if (doctor == null) return;
        root = insert(root, doctor);
    }

    public Doctor findById(int id) {
        Node n = search(root, id);
        return n == null ? null : n.doctor;
    }

    private Node insert(Node cur, Doctor doctor) {
        if (cur == null) return new Node(doctor);

        int id = doctor.getId();
        int curId = cur.doctor.getId();

        if (id < curId) cur.left = insert(cur.left, doctor);
        else if (id > curId) cur.right = insert(cur.right, doctor);
        else cur.doctor = doctor; // same ID -> replace
        return cur;
    }

    private Node search(Node cur, int id) {
        if (cur == null) return null;

        int curId = cur.doctor.getId();
        if (id == curId) return cur;
        if (id < curId) return search(cur.left, id);
        return search(cur.right, id);
    }
}
