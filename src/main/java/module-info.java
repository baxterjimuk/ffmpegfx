module com.ahthek {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bytedeco.javacv;
    requires java.prefs;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;
    requires org.apache.commons.io;
    
    opens com.ahthek to javafx.fxml;
    exports com.ahthek;
}
