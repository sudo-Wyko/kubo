package com.teamroy;

import com.teamroy.controller.DatabaseSetupDialogController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DatabaseSetupDialog {
    private DatabaseSetupDialog() {
    }

    public static boolean promptUntilConfigured() {
        while (!DatabaseConfig.exists()) {
            if (!showBlocking()) {
                return false;
            }
        }
        return true;
    }

    private static boolean showBlocking() {
        if (Platform.isFxApplicationThread()) {
            return showDialogInternal();
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        Platform.runLater(() -> {
            try {
                success.set(showDialogInternal());
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
        return success.get();
    }

    private static boolean showDialogInternal() {
        try {
            URL location = DatabaseSetupDialog.class.getResource("/com/teamroy/database_setup_dialog.fxml");
            if (location == null) {
                throw new IOException("Missing FXML: /com/teamroy/database_setup_dialog.fxml");
            }
            FXMLLoader loader = new FXMLLoader(location);
            Parent root = loader.load();
            DatabaseSetupDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Kubo — Database Setup");
            Scene scene = new Scene(root);
            URL css = App.class.getResource("/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

            return controller.wasCompleted();
        } catch (IOException ex) {
            throw new RuntimeException("Could not open database setup dialog.", ex);
        }
    }
}
