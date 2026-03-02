package History;

import Persons.Patient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import java.util.*;

public class HistoryOfPatient {

    private final Map<Patient, Stack<History>> historyMap = new HashMap<>();

    public void addHistory(History history) {
        Patient p = history.getPatient();
        historyMap
                .computeIfAbsent(p, k -> new Stack<>())
                .push(history);
    }

    public List<History> getHistory(Patient patient) {
        Stack<History> stack = historyMap.get(patient);
        if (stack == null) return List.of();
        return new ArrayList<>(stack);
    }
    public List<History> getAll() {
        List<History> all = new ArrayList<>();
        for (Stack<History> stack : historyMap.values()) {
            all.addAll(stack);
        }
        return all;
    }

}
