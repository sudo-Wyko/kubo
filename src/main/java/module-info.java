module com.teamroy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.teamroy to javafx.fxml;

    exports com.teamroy;

    opens com.teamroy.controller to javafx.fxml;

}
