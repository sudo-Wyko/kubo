module com.teamroy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.teamroy to javafx.fxml;

    exports com.teamroy;

    opens com.teamroy.Controller to javafx.fxml;
    exports com.teamroy.Model;
    opens com.teamroy.Model to javafx.fxml;

}
