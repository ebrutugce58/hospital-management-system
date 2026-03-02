package ui.controllers;

import javafx.fxml.FXML;

public class PortalSelectController {

    @FXML
    private void onUserPortal() {
        ShellController shell = ShellController.getInstance();
        if (shell == null) {
            System.out.println("❌ ShellController instance is null");
            return;
        }
        shell.openLoginPortal("Patient / Doctor Login", new String[]{"PATIENT", "DOCTOR"});
    }

    @FXML
    private void onAdminPortal() {
        ShellController shell = ShellController.getInstance();
        if (shell == null) {
            System.out.println("❌ ShellController instance is null");
            return;
        }
        shell.openLoginPortal("Admin Login", new String[]{"ADMIN"});
    }

    @FXML
    private void onPatientRegister() {
        ShellController shell = ShellController.getInstance();
        if (shell == null) {
            System.out.println("❌ ShellController instance is null");
            return;
        }
        shell.openRegisterPortal();
    }
}



