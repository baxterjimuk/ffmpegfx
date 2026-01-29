package com.ahthek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.apache.commons.io.FilenameUtils;
// import org.bytedeco.javacpp.Loader;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Move {
  Preferences preferences = Preferences.userNodeForPackage(Controller.class);
  String[] atoz = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ").split("");
  private ObservableList<String> filePaths = FXCollections.observableArrayList();

  ExtensionFilter allFilter = new ExtensionFilter("All files", "*.*");
  ExtensionFilter edlFilter = new ExtensionFilter("EDL files", "*.edl");

  String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
  String sample = "* The resulting .bat files will have names such as " + now + "-move-full-do.bat"
  + " and " + now + "-move-full-undo.bat";

  @FXML
  private ToggleGroup moveGroup;

  @FXML
  private RadioButton moveFullRadioButton, moveTrimRadioButton;

  @FXML
  private ListView<String> edlFileListView;

  @FXML
  private Button clearButton, removeEdlFileButton, startButton;

  @FXML
  private TextField batchFileFolderTextField;

  @FXML
  private Label batSampleLabel;

  @FXML
  private CheckBox darkCheckBox;
  
  @FXML
  private void generateBatFile() throws IOException {
    RadioButton selectedRadio = (RadioButton) moveGroup.getSelectedToggle();
    Alert alert = new Alert(AlertType.CONFIRMATION);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    alert.setContentText("Are you sure you want to continue?");
    alert.setHeaderText(selectedRadio.equals(moveFullRadioButton) ? "Move-Full" : "Move-Trim");

    Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.OK) {
      if (selectedRadio.equals(moveFullRadioButton)) {
        System.out.println("doing move-full");
        moveFull();
        System.out.println("move-full done");
      } else {
        System.out.println("doing move-trim");
        moveTrim();
        System.out.println("move-trim done");
      }
    }
  }

  @FXML
  private void switchToBat() throws IOException {
    MainApp.setRoot("batCombine");
  }

  @FXML
  private void switchToUfc() throws IOException {
    MainApp.setRoot("ufc");
  }

  @FXML
  private void swtichToMain() throws IOException {
    MainApp.setRoot("main");
  }

  @FXML
  private void darkMode(ActionEvent event) {
    darkCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
      Scene scene = ((Node) event.getSource()).getScene();
      if (isSelected) {
        scene.getStylesheets().add("style.css");
      } else {
        scene.getStylesheets().remove("style.css");
      }
    });
  }

  @FXML
  private void selectFiles(ActionEvent event) throws IOException {
    FileChooser fileChooser = new FileChooser();
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setTitle("Select .edl file(s)");
    fileChooser.setInitialDirectory(initialDir);
    fileChooser.getExtensionFilters().addAll(allFilter, edlFilter);
    fileChooser.setSelectedExtensionFilter(edlFilter);
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
    if (selectedFiles != null) {
      for (File file : selectedFiles) {
        if (!filePaths.contains(file.getAbsolutePath())) {
          filePaths.add(file.getAbsolutePath());
        }
      }
      preferences.put("lastUsedDir", selectedFiles.getLast().getParent());
    }
  }

  @FXML
  private void removeFiles() {
    filePaths.removeAll(edlFileListView.getSelectionModel().getSelectedItems());
    edlFileListView.getSelectionModel().clearSelection();
  }

  @FXML
  private void clearFiles() {
    filePaths.clear();
  }

  @FXML
  public void initialize() {
    edlFileListView.setItems(filePaths);
    edlFileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    edlFileListView.getSelectionModel().getSelectedItems().addListener(
      (ListChangeListener.Change<? extends String> changed) -> {
          System.out.println(edlFileListView.getSelectionModel().getSelectedItems());
    });
    removeEdlFileButton.disableProperty().bind(Bindings.isEmpty(edlFileListView.getSelectionModel().getSelectedItems()));
    clearButton.disableProperty().bind(Bindings.isEmpty(filePaths));
    BooleanBinding disableStart = Bindings.isEmpty(filePaths)
    .or(batchFileFolderTextField.textProperty().isEmpty()
    );
    startButton.disableProperty().bind(disableStart);
    batSampleLabel.setText(sample);
  }

  @FXML
  private void selectBatFolder(ActionEvent event) throws IOException {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    directoryChooser.setTitle("Select folder to save the batch files to");
    directoryChooser.setInitialDirectory(initialDir);
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    File selectedFolder = directoryChooser.showDialog(stage);
    if (selectedFolder != null) {
        batchFileFolderTextField.setText(selectedFolder.getAbsolutePath());
        preferences.put("lastUsedDir", selectedFolder.getAbsolutePath());
    }
  }

  private void moveTrim() throws IOException {
    // no undo is possible here as it involves deleting
    System.out.println("this is moveTrim()");
    StringBuilder sb = new StringBuilder();
    now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    Path doPath = Paths.get(batchFileFolderTextField.getText(), now + "-move-full-do.bat");
    
    try (
      BufferedWriter doWriter = new BufferedWriter(new FileWriter(doPath.toString(), true));
    ) {
      doWriter.write("@echo off");
      doWriter.newLine();
      doWriter.write("chcp 65001");
      for (String edlPath: filePaths) {
        File edlFile = new File(edlPath);
        File videoFile = new File(edlPath.replace(".edl", ""));
        String videoFileBaseName = FilenameUtils.getBaseName(videoFile.getName());
        // Path newFolder = Paths.get(edlFile.getParent(), videoFileBaseName);

        doWriter.newLine();
        doWriter.write("cd /d \"" + edlFile.getParent() + "\"");
        doWriter.newLine();
        doWriter.write("if not exist \".\\" + videoFileBaseName + "\\\" mkdir \"" + videoFileBaseName + "\"");
        doWriter.newLine();
        moveWriter(doWriter, edlFile.getName(), ".\\" + videoFileBaseName + "\\");
        doWriter.newLine();
        moveWriter(doWriter, videoFile.getName(), ".\\" + videoFileBaseName + "\\");
        doWriter.newLine();
        doWriter.write("cd /d \".\\" + videoFileBaseName + "\"");

        List<String> timelist = Files.readAllLines(edlFile.toPath());
        doWriter.newLine();
        doWriter.write("echo [" + timelist.size() + "] " + videoFile.getName());
        for (int i = 0; i < timelist.size(); i++) {
          String[] timearr = timelist.get(i).replace(",", ".").split("\t");
          sb.setLength(0);
          sb.append("ffmpeg -y -hide_banner -loglevel warning -stats -ss ");
          sb.append(timearr[0]);
          sb.append(" -to ");
          sb.append(timearr[1]);
          sb.append(" -i ");
          sb.append("\"" + videoFile.getName() + "\"");
          sb.append(" -c copy ");
          sb.append("\"" + videoFileBaseName + "_" + atoz[i] + ".mkv\"");
          doWriter.newLine();
          doWriter.write(sb.toString());
        }
        doWriter.newLine();
        doWriter.write("del \"" + edlFile.getName() + "\" \"" + videoFile.getName() + "\"");

        /* if (!Files.isDirectory(newFolder)) {
          Files.createDirectory(newFolder);
        }

        Path edlTarget = Paths.get(newFolder.toString(), edlFile.getName());
        Path videoTarget = Paths.get(newFolder.toString(), videoFile.getName());

        Files.move(edlFile.toPath(), edlTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.move(videoFile.toPath(), videoTarget, StandardCopyOption.REPLACE_EXISTING);

        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        List<String> pbList = new ArrayList<>();
        List<String> timeList = Files.readAllLines(edlTarget);
        for (int i = 0; i < timeList.size(); i++) {
          String[] timearr = timeList.get(i).replace(",", ".").split("\t");
          pbList.clear();
          pbList.add(ffmpeg);
          pbList.add("-y");
          pbList.add("-hide_banner");
          pbList.add("-loglevel");
          pbList.add("warning");
          pbList.add("-stats");
          pbList.add("-ss");
          pbList.add(timearr[0]);
          pbList.add("-to");
          pbList.add(timearr[1]);
          pbList.add("-i");
          pbList.add(videoFile.getName());
          pbList.add("-c");
          pbList.add("copy");
          pbList.add(videoFileBaseName + "_" + atoz[i] + ".mkv");
          ProcessBuilder pb = new ProcessBuilder(pbList);
          pb.directory(newFolder.toFile());
          try {
            System.out.println(String.join(" ", pb.command()));
            pb.inheritIO().start().waitFor();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        // delete video and edl file here so that we quickly regain space for the next video
        Files.deleteIfExists(edlTarget);
        Files.deleteIfExists(videoTarget); */
      }
      doWriter.newLine();
      doWriter.write("pause");
      doWriter.close();
    }
  }

  private void moveFull() throws IOException {
    now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    Path doPath = Paths.get(batchFileFolderTextField.getText(), now + "-move-full-do.bat");
    Path undoPath = Paths.get(batchFileFolderTextField.getText(), now + "-move-full-undo.bat");
    
    try (
      BufferedWriter doWriter = new BufferedWriter(new FileWriter(doPath.toString(), true));
      BufferedWriter undoWriter = new BufferedWriter(new FileWriter(undoPath.toString(), true));
    ) {
      doWriter.write("@echo off");
      doWriter.newLine();
      doWriter.write("chcp 65001");
      undoWriter.write("@echo off");
      undoWriter.newLine();
      undoWriter.write("chcp 65001");
      for (String edlPath: filePaths) {
        File edlFile = new File(edlPath);
        File videoFile = new File(edlPath.replace(".edl", ""));
        String videoFileBaseName = FilenameUtils.getBaseName(videoFile.getName());
        Path newFolder = Paths.get(edlFile.getParent(), videoFileBaseName);

        /* Path edlTarget = Paths.get(newFolder.toString(), edlFile.getName());
        Path videoTarget = Paths.get(newFolder.toString(), videoFile.getName());

        if (!Files.isDirectory(newFolder)) {
          Files.createDirectory(newFolder);
        }

        Files.move(edlFile.toPath(), edlTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.move(videoFile.toPath(), videoTarget, StandardCopyOption.REPLACE_EXISTING); */
        
        if (!Files.isDirectory(newFolder)) {
          doWriter.newLine();
          doWriter.write("mkdir \"" + newFolder.toString() + "\"");
        }
        doWriter.newLine();
        moveWriter(doWriter, edlPath, newFolder.toString());
        doWriter.newLine();
        moveWriter(doWriter, videoFile.getAbsolutePath(), newFolder.toString());

        undoWriter.newLine();
        moveWriter(undoWriter, Paths.get(newFolder.toString()) + "\\*.*", edlFile.getParent());
        undoWriter.newLine();
        undoWriter.write("rmdir \"" + newFolder.toString() + "\"");
      }
      doWriter.newLine();
      doWriter.write("pause");
      doWriter.close();
      undoWriter.newLine();
      undoWriter.write("pause");
      undoWriter.close();
    }
  }

  private void moveWriter(BufferedWriter writer, String source, String target) throws IOException {
    writer.write("move \"" + source + "\" \"" + target + "\"");
  }
}
