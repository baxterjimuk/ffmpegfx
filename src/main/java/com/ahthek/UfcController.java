package com.ahthek;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class UfcController {
  Preferences preferences = Preferences.userNodeForPackage(UfcController.class);

  @FXML
  private void switchToBat() throws IOException {
    MainApp.setRoot("batCombine");
  }

  @FXML
  private void swtichToMain() throws IOException {
    MainApp.setRoot("main");
  }

  @FXML
  private TextField mltFileTextField;

  @FXML
  private void selectMltFile(ActionEvent event) throws IOException {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select .mlt file");
    fileChooser.setInitialDirectory(new File(preferences.get("lastUsedDir", System.getProperty("user.home"))));;
    fileChooser.getExtensionFilters().addAll(
      new ExtensionFilter("All files", "*.*"),
      new ExtensionFilter("MLT files", "*.mlt")
    );
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    File selectedMlt = fileChooser.showOpenDialog(stage);
    if (selectedMlt != null) {
      mltFileTextField.setText(selectedMlt.getAbsolutePath());
      preferences.put("lastUsedDir", selectedMlt.getParent());
    }
  }

  public static long convertDurationToMillis(String duration) {
    String[] parts = duration.split(":");
    long hours = Long.parseLong(parts[0]);
    long minutes = Long.parseLong(parts[1]);
    String[] secondsAndMillis = parts[2].split("\\.");
    long seconds = Long.parseLong(secondsAndMillis[0]);
    long milliseconds = Long.parseLong(secondsAndMillis[1]);

    return hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds;
  }
}
