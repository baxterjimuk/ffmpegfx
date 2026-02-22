package com.ahthek;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
// import javafx.util.StringConverter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;

public class TimeTextField extends Application {

    @Override
    public void start(Stage primaryStage) {
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm:ss.SSS");

        // Define the pattern: HH:mm:ss.SSS
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        // Filter to restrict input to digits and colons/periods
        UnaryOperator<javafx.scene.control.TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            // Regex for HH:mm:ss.SSS (basic structure check)
            if (newText.matches("([0-1][0-9]|2[0-3])?(:[0-5][0-9]){0,2}(\\.[0-9]{0,3})?")) {
                return change;
            }
            return null; // Reject change
        };

        timeField.setTextFormatter(new javafx.scene.control.TextFormatter<>(filter));

        // Optional: Validate full format on focus loss
        timeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                try {
                    LocalTime.parse(timeField.getText(), formatter);
                    timeField.setStyle(""); // Valid
                } catch (DateTimeParseException e) {
                    timeField.setStyle("-fx-border-color: red;"); // Invalid
                }
            }
        });

        VBox root = new VBox(timeField);
        primaryStage.setScene(new Scene(root, 300, 100));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
