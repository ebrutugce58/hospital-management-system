package ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.auth.AuthService;
import ui.auth.PasswordUtil;
import ui.auth.Session;
import ui.auth.UserAccount;
import ui.auth.UserRepository;
import ui.models.Role;

public class ChangePasswordController {

    @FXML private Label lblUser;
    @FXML private Label lblInfo;

    // ✅ Admin için hedef kullanıcı
    @FXML private HBox rowAdminTarget;
    @FXML private TextField tfTargetUser;

    // ✅ Patient/Doctor için eski şifre
    @FXML private Label lblOldTitle;
    @FXML private PasswordField pfOld;

    @FXML private PasswordField pfNew;
    @FXML private PasswordField pfConfirm;

    @FXML private TextField tfOldVisible;
    @FXML private TextField tfNewVisible;
    @FXML private TextField tfConfirmVisible;


    private final UserRepository repo = new UserRepository();
    private final AuthService authService = new AuthService(repo);

    @FXML
    public void initialize() {
        UserAccount cur = Session.getCurrent();

        if (cur != null && lblUser != null) {
            lblUser.setText("User: " + cur.getUsername() + " (" + cur.getRole() + ")");
        }
        info("");

        boolean isAdmin = (cur != null && cur.getRole() == Role.ADMIN);

        // ✅ Admin UI göster/gizle
        if (rowAdminTarget != null) {
            rowAdminTarget.setVisible(isAdmin);
            rowAdminTarget.setManaged(isAdmin);
        }

        // ✅ Admin'de old password alanı gizli (zorunlu değil)
        if (lblOldTitle != null) {
            lblOldTitle.setVisible(!isAdmin);
            lblOldTitle.setManaged(!isAdmin);
        }
        if (pfOld != null) {
            pfOld.setVisible(!isAdmin);
            pfOld.setManaged(!isAdmin);
        }
    }

    @FXML
    private void onSave() {

        UserAccount cur = Session.getCurrent();
        if (cur == null) { info("Please login."); return; }

        boolean isAdmin = cur.getRole() == Role.ADMIN;

        // ✅ hedef kullanıcı
        String targetUsername = cur.getUsername();
        if (isAdmin) {
            targetUsername = (tfTargetUser == null || tfTargetUser.getText() == null)
                    ? "" : tfTargetUser.getText().trim();

            if (targetUsername.isBlank()) {
                info("Please enter target username/ID.");
                return;
            }

            if (repo.findByUsername(targetUsername) == null) {
                info("Target user not found: " + targetUsername);
                return;
            }
        }

        // ✅ OLD password (patient/doctor için) -> hangi field görünürse oradan oku
        if (!isAdmin) {
            String oldP = readPassword(pfOld, tfOldVisible);

            if (oldP.isBlank()) { info("Current password is required."); return; }

            if (authService.authenticate(cur.getUsername(), oldP) == null) {
                info("Current password is incorrect.");
                return;
            }
        }

        // ✅ NEW + CONFIRM -> hangi field görünürse oradan oku
        String newP  = readPassword(pfNew, tfNewVisible);
        String conf  = readPassword(pfConfirm, tfConfirmVisible);

        if (newP.isBlank() || conf.isBlank()) { info("New password fields are required."); return; }
        if (!newP.equals(conf)) { info("New passwords do not match."); return; }
        if (newP.length() < 4) { info("Password must be at least 4 characters."); return; }

        String newHash = PasswordUtil.sha256(newP);

        // ✅ data/users.txt içinde güncelle
        repo.updatePassword(targetUsername, newHash);

        info(isAdmin
                ? ("Password updated for: " + targetUsername)
                : "Password updated. Please login again.");

        // ✅ temizle (göz açıkken textfield da temizlensin)
        clearPassword(pfOld, tfOldVisible);
        clearPassword(pfNew, tfNewVisible);
        clearPassword(pfConfirm, tfConfirmVisible);

        if (tfTargetUser != null) tfTargetUser.clear();
    }

    /** pf görünürse pf'den, değilse tf'den okur (👁 toggle uyumlu) */
    private String readPassword(PasswordField pf, TextField tf) {
        if (pf != null && pf.isVisible()) {
            return pf.getText() == null ? "" : pf.getText();
        }
        if (tf != null) {
            return tf.getText() == null ? "" : tf.getText();
        }
        return "";
    }

    /** hem PasswordField hem TextField'i temizler */
    private void clearPassword(PasswordField pf, TextField tf) {
        if (pf != null) pf.clear();
        if (tf != null) tf.clear();
    }

    private void info(String msg) {
        if (lblInfo != null) lblInfo.setText(msg == null ? "" : msg);
    }
    @FXML
    private void toggleOldPassword() {
        toggle(pfOld, tfOldVisible);
    }

    @FXML
    private void toggleNewPassword() {
        toggle(pfNew, tfNewVisible);
    }

    @FXML
    private void toggleConfirmPassword() {
        toggle(pfConfirm, tfConfirmVisible);
    }

    private void toggle(PasswordField pf, TextField tf) {
        if (pf.isVisible()) {
            tf.setText(pf.getText());
            pf.setVisible(false);
            pf.setManaged(false);
            tf.setVisible(true);
            tf.setManaged(true);
        } else {
            pf.setText(tf.getText());
            tf.setVisible(false);
            tf.setManaged(false);
            pf.setVisible(true);
            pf.setManaged(true);
        }
    }

}



