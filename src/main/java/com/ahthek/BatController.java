package com.ahthek;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class BatController {
  Preferences preferences = Preferences.userNodeForPackage(BatController.class);

  @FXML
  private ListView<String> batFileListView;

  @FXML
  private CheckBox putInFolderCheckBox;

  @FXML
  private TextField putInFolderTextField, combinedNameTextField, batchFileFolderTextField;

  @FXML
  private Button allFolderButton, clearButton, removeButton;

  private ObservableList<String> batPaths = FXCollections.observableArrayList();

  @FXML
  private void swtichToMain() throws IOException {
    MainApp.setRoot("main");
  }

  @FXML
  private void switchToUfc() throws IOException {
    MainApp.setRoot("ufc");
  }

  @FXML
  public void initialize() {
    batFileListView.setItems(batPaths);
    batFileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    removeButton.disableProperty().bind(Bindings.isEmpty(batFileListView.getSelectionModel().getSelectedItems()));
    clearButton.disableProperty().bind(Bindings.isEmpty(batPaths));
    putInFolderTextField.disableProperty().bind(putInFolderCheckBox.selectedProperty().not());
    allFolderButton.disableProperty().bind(putInFolderCheckBox.selectedProperty().not());
  }

  @FXML
  private void selectFileFolder(ActionEvent event) throws IOException {
    selectFolder(
      event, 
      "Select folder to save all the video files to", 
      "lastUsedDir", 
      putInFolderTextField
    );
  }

  @FXML
  private void generateCombinedName() throws IOException {
    combinedNameTextField.setText(
      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
      + "_combine"
    );
  }

  @FXML
  private void selectCombinedFolder(ActionEvent event) throws IOException {
    selectFolder(
      event,
      "Select folder to save the combined batch file to", 
      "lastUsedDir",
      batchFileFolderTextField
      );
  }

  private void selectFolder(
    ActionEvent event, 
    String title,
    String lastUsedDir,
    TextField textField
  ) throws IOException {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle(title);
    directoryChooser.setInitialDirectory(new File(preferences.get(lastUsedDir, System.getProperty("user.home"))));
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    File selectedFolder = directoryChooser.showDialog(stage);
    if (selectedFolder != null) {
        textField.setText(selectedFolder.getAbsolutePath());
        preferences.put(lastUsedDir, selectedFolder.getAbsolutePath());
    }
  }

  @FXML
  private void clearFiles() throws IOException {
    batPaths.clear();
  }

  @FXML
  private void removeFiles() throws IOException {
    batPaths.removeAll(batFileListView.getSelectionModel().getSelectedItems());
    batFileListView.getSelectionModel().clearSelection();
  }

  @FXML
  private void selectFiles(ActionEvent event) throws IOException {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select .bat file(s)");
    fileChooser.setInitialDirectory(new File(preferences.get("lastUsedDir", System.getProperty("user.home"))));;
    fileChooser.getExtensionFilters().addAll(
      new ExtensionFilter("All files", "*.*"),
      new ExtensionFilter("Batch files", "*.bat")
    );
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    List<File> selectedBat = fileChooser.showOpenMultipleDialog(stage);
    if (selectedBat != null) {
      for (File file: selectedBat) {
        if(!batPaths.contains(file.getAbsolutePath())){
          batPaths.add(file.getAbsolutePath());
        }
      }
      preferences.put("lastUsedDir", selectedBat.getLast().getParent());
    }
  }
}
