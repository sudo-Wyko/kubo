package com.teamroy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        // 1. --- DATABASE INITIALIZATION ---
        // Read the config file and power up the DatabaseUtility
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);

            String fullUrl = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");

            DatabaseUtility.initialize(fullUrl, user, pass);
            System.out.println("DatabaseUtility initialized successfully.");

        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not find or read config.properties file!");
            e.printStackTrace();
            // Note: If this fails, your app will still launch the login screen,
            // but logging in will fail because DatabaseUtility won't have credentials.
        }

        // 2. --- UI INITIALIZATION ---
        // Change "primary" to "login" here to set the initial screen
        // Adjust the dimensions (900x600) to match your login.fxml size
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