package ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.controllers.ServiceAware;
import ui.services.HospitalService;

public class SceneManager {

    private static Stage stage;
    private static HospitalService service;   // ✅ EKLENDİ

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    // ✅ Tek service'i burada sakla
    public static void setService(HospitalService s) {
        service = s;
    }
    public static HospitalService getService() {         // ✅ EKLE
        return service;
    }

    public static void show(String fxmlName) {
        try {
        	FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/" + fxmlName));
        	Parent root = loader.load();
        	System.out.println("LOGIN service=" + System.identityHashCode(service)
            + " | doctorsSize=" + service.getAllDoctors().size());


        	Object controller = loader.getController();
        	if (controller instanceof ServiceAware sa) {
        	    sa.setService(SceneManager.getService());
        	}

            Scene scene = new Scene(root);

            scene.getStylesheets().clear();
            root.getStylesheets().clear();

            String cssFile = fxmlName.equalsIgnoreCase("portal_select.fxml")
                    ? "/hospital-theme.css"
                    : "/styles.css";

            var cssUrl = SceneManager.class.getResource(cssFile);
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                scene.getStylesheets().add(css);
                root.getStylesheets().add(css);
            } else {
                System.out.println("❌ CSS bulunamadı: " + cssFile);
            }

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
