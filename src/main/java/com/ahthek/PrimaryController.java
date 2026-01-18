package com.ahthek;

import java.io.File;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        MainApp.setRoot("secondary");
    }

    @FXML
    private void switchToMain() throws IOException {
        MainApp.setRoot("main");
    }

    @FXML
    private TextField filePathField;

    @FXML
    private void selectFiles(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File(s)");
        fileChooser.setInitialDirectory(new File("C:\\Users\\te\\Downloads\\new stuff\\in hddrive"));
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Batch File", "*.bat"),
                new ExtensionFilter("EDL File", "*.edl"), new ExtensionFilter("All Files", "*.*"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            // Further processing (e.g., loading the file content) can be done here
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            System.out.println(selectedFile.getParent());
        } else {
            filePathField.setText(null);
        }
    }
}
