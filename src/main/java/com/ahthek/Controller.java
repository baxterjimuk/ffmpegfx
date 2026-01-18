package com.ahthek;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.SelectionMode;
import javafx.collections.ObservableList;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.apache.commons.io.FilenameUtils;

import java.util.prefs.Preferences;

public class Controller {
  Preferences preferences = Preferences.userNodeForPackage(Controller.class);

  // start with 0 so that if we want to call 'B', we can use atoz[2] instead of atoz[1]
  // in other words, 'B' is the number 2 letter in the list of alphabet
  String[] atoz = ("0ABCDEFGHIJKLMNOPQRSTUVWXYZ").split("");

  @FXML
  private ListView<String> edlFileListView;

  private ObservableList<String> filePaths = FXCollections.observableArrayList();

  @FXML
  private TextArea selectedTextArea;

  @FXML
  private void switchToBat() throws IOException {
    MainApp.setRoot("batCombine");
  }

  @FXML
  private Button removeEdlFileButton, clearButton, startButton;

  @FXML
  private RadioButton trimCopyRadioButton;

  @FXML
  private void generateBatFile() throws IOException {
    // loop through each edl file
    for (String edlPath : filePaths) {
      File edlFile = new File(edlPath);
      String edlFileName = edlFile.getName();
      String videoFileName = FilenameUtils.getBaseName(edlFileName);
      String parent = edlFile.getParent();
      
      String batPath = edlPath.replace(".edl", ".bat");

      Files.deleteIfExists(Paths.get(batPath));
      
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(batPath, true))) {
        bw.write("chcp 65001");
        bw.newLine();

        try (BufferedReader br = new BufferedReader(new FileReader(edlFile))) {
          int count = 0;
          String line;
          while ((line = br.readLine()) != null) {
            count += 1;

            // for each line in an edl file, the time in miliseconds is written with comma (,)
            // here we convert it to dot (.) and then convert each line to an array
            // each line contains only 2 timestamps which is a start and an end and is separated by 'tab'.
            String[] arr = line.replace(",", ".").split("\t");

            String idx = trimCopyRadioButton.isSelected() ? atoz[count] : String.format("%02d", count);
            String codec = trimCopyRadioButton.isSelected() ? " -c copy " : " -c:v libx265 -c:a aac ";
            String out = Paths.get(parent, FilenameUtils.removeExtension(videoFileName) + "_" + idx + ".mkv").toString();

            // need to check the audio codec of the file first. if already aac no need convert.
            // but this one later la
            StringBuilder cmd = new StringBuilder();
            cmd.append("ffmpeg -y -hide_banner -ss ");
            cmd.append(arr[0]);
            cmd.append(" -to ");
            cmd.append(arr[1]);
            cmd.append(" -i ");
            cmd.append("\"" + FilenameUtils.removeExtension(edlPath) + "\"");
            cmd.append(codec);
            cmd.append("\"" + out + "\"");

            bw.write(cmd.toString());
            bw.newLine();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        bw.write("@pause");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    System.out.println("generateBatFile completed!");
  }

  @FXML
  public void initialize() {
    edlFileListView.setItems(filePaths);
    edlFileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    edlFileListView.getSelectionModel().getSelectedItems().addListener(
      (ListChangeListener.Change<? extends String> changed) -> {
          // System.out.println(edlFileListView.getSelectionModel().getSelectedItems());
          selectedTextArea.setText(
            edlFileListView.getSelectionModel().getSelectedItems().stream().collect(
              Collectors.joining("\n"))
            );
    });
    removeEdlFileButton.disableProperty().bind(Bindings.isEmpty(edlFileListView.getSelectionModel().getSelectedItems()));
    clearButton.disableProperty().bind(Bindings.isEmpty(filePaths));
    startButton.disableProperty().bind(Bindings.isEmpty(filePaths));
  }

  @FXML
  private void removeFiles(ActionEvent event) throws IOException {
    filePaths.removeAll(edlFileListView.getSelectionModel().getSelectedItems());
    edlFileListView.getSelectionModel().clearSelection();
  }

  @FXML
  private void selectFiles(ActionEvent event) throws IOException {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select .edl File(s)");
    fileChooser.setInitialDirectory(new File(preferences.get("lastUsedDir", System.getProperty("user.home"))));
    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All files", "*.*"),
        new ExtensionFilter("EDL files", "*.edl"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
    if (selectedFiles != null) {
      for (File file : selectedFiles) {
        if (!filePaths.contains(file.getAbsolutePath())) {
          filePaths.add(file.getAbsolutePath());
        }
      }
      preferences.put("lastUsedDir", selectedFiles.getLast().getParent());
    } else {
      System.out.println("No file selected.");
    }
  }

  @FXML
  private void clearFiles(ActionEvent event) throws IOException {
    filePaths.clear();
  }
}
