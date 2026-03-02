package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.controllers.ShellController;
import ui.services.HospitalService;

public class MainApp extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		ui.AppPaths.ensureDataFiles();
		HospitalService service = new HospitalService();

		

	    FXMLLoader loader = new FXMLLoader(getClass().getResource("/shell.fxml"));
	    Scene scene = new Scene(loader.load(), 950, 600);

	    scene.getStylesheets().add(
	            getClass().getResource("/styles.css").toExternalForm()
	    );

	    ShellController shellController = loader.getController();
	    shellController.setService(service);

	    // ✅ İlk ekran: shell içinden portal seçimi göster
	    shellController.showPortalSelect();

	    stage.setTitle("Hospital Management System");
	    stage.setScene(scene);
	    stage.show();

	    // ✅ Sadece stage/service set edebilirsin (opsiyonel)
	    SceneManager.setStage(stage);
	   
	    // ❌ BUNU SİL:
	    // SceneManager.show("portal_select.fxml");
	}


    public static void main(String[] args) {
        launch(args);
    }
    public class AppConfig {
        // true  => KALICI (kaydeder, kapanınca kalır)
        // false => DEMO   (kapanınca/sıradaki açılışta sıfırlar)
        public static final boolean PERSIST_DATA = false;
    }

    
}
