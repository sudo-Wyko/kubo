module com.teamroy {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive java.sql;
    requires java.desktop;
    opens com.teamroy to javafx.fxml;
    opens com.teamroy.controller to javafx.fxml;
    opens com.teamroy.model.entity to javafx.base;
    exports com.teamroy;
    exports com.teamroy.service;
    exports com.teamroy.model.entity;
    exports com.teamroy.model.dao;
}
