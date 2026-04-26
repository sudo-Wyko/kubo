module com.teamroy {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires transitive java.sql;

    opens com.teamroy to javafx.fxml;

    exports com.teamroy;

    opens com.teamroy.controller to javafx.fxml;

}
