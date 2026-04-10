package com.teamroy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Change "primary" to "login" here to set the initial screen
        // 2. Adjust the dimensions (900x600) to match your login.fxml size
        scene = new Scene(loadFXML("login"), 900, 600);

        // Give the window a professional title
        stage.setTitle("Kubo Property Management");

        stage.setScene(scene);
        stage.show();
    }

    // You will use this method later inside your LoginController
    // to switch to the "primary" dashboard after a successful login!
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}