module com.teamroy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.teamroy to javafx.fxml;
    opens com.teamroy.controller to javafx.fxml;
    opens com.teamroy.model.entity to javafx.base;
 
    exports com.teamroy;
    exports com.teamroy.service;
}
