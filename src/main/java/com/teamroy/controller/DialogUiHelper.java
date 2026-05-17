package com.teamroy.controller;

import com.teamroy.App;
import javafx.scene.Scene;

import java.net.URL;

final class DialogUiHelper {
    private DialogUiHelper() {
    }

    static void applyStyles(Scene scene) {
        URL css = App.class.getResource("/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
