module com.teamroy {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.teamroy to javafx.fxml;
    exports com.teamroy;
}
