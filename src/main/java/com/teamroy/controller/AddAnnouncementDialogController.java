package com.teamroy.controller;

import com.teamroy.model.dao.AnnouncementDaoImpl;
import com.teamroy.model.dao.DaoException;
import com.teamroy.model.entity.Announcement;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;

public class AddAnnouncementDialogController {
    @FXML
    private TextField titleField;
    @FXML
    private TextArea messageField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private AnnouncementDaoImpl announcementDao;
    private Runnable onSuccess;

    public void configure(Connection conn, Runnable onSuccessCallback) {
        this.announcementDao = new AnnouncementDaoImpl(conn);
        this.onSuccess = onSuccessCallback;
        titleField.clear();
        messageField.clear();
        clearError();
    }

    @FXML
    private void confirm() {
        clearError();
        String title = safe(titleField.getText());
        String message = safe(messageField.getText());
        if (title.isBlank()) {
            showError("Title is required.");
            return;
        }
        if (message.isBlank()) {
            showError("Message is required.");
            return;
        }
        try {
            Announcement announcement = new Announcement(title, message, null);
            announcementDao.Create(announcement);
            if (announcement.GetAnnouncementID() <= 0) {
                throw new DaoException("Announcement was not saved.");
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
            close();
        } catch (DaoException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
