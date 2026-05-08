package com.teamroy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Initialize database connection (will create database if it doesn't exist)
        try {
            ConnectionManager.getConnection();
        } catch (RuntimeException e) {
            System.err.println("Warning: Could not initialize database at startup: " + e.getMessage());
        }
        
        scene = new Scene(loadFXML("login"), 900, 600);
        applyGlobalStyles(scene);

        stage.setTitle("Kubo Property Management");
        applyStageIcons(stage);

        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxmlBaseName) throws IOException {
        URL location = App.class.getResource("/com/teamroy/" + fxmlBaseName + ".fxml");
        if (location == null) {
            throw new IOException("Missing FXML classpath resource: /com/teamroy/" + fxmlBaseName + ".fxml");
        }
        return new FXMLLoader(location).load();
    }

    private static void applyGlobalStyles(Scene scene) {
        URL css = App.class.getResource("/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("Warning: Missing stylesheet classpath resource: /style.css");
        }
    }

    private static void applyStageIcons(Stage stage) {
        try (InputStream is = App.class.getResourceAsStream("/com/teamroy/icon.png")) {
            if (is != null) {
                stage.getIcons().add(new Image(is));
                return;
            }
        } catch (IOException ignored) {
            // fallback below
        }
        WritableImage wi = placeholderIcon64();
        stage.getIcons().add(wi);
    }

    private static WritableImage placeholderIcon64() {
        WritableImage img = new WritableImage(64, 64);
        PixelWriter pw = img.getPixelWriter();
        int argb = 0xFF2563EB;
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                pw.setArgb(x, y, argb);
            }
        }
        return img;
    }

    public static void main(String[] args) {
        launch();
    }
}
