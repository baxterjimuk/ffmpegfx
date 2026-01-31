package com.ahthek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

public class BatController {
  Preferences preferences = Preferences.userNodeForPackage(BatController.class);

  @FXML
  private ListView<String> batFileListView;

  @FXML
  private CheckBox putInFolderCheckBox, moveCheckBox, shutdownCheckBox;

  @FXML
  private Label durationLabel, estimateLabel;

  @FXML
  private HBox durationHBox;

  @FXML
  private Spinner<Integer> dSpinner, hSpinner, mSpinner, sSpinner;

  @FXML
  private TextField putInFolderTextField, combinedNameTextField, batchFileFolderTextField;

  @FXML
  private Button allFolderButton, clearButton, removeButton, startButton, normalizeButton, resetButton;

  private ObservableList<String> batPaths = FXCollections.observableArrayList();

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  @FXML
  private void generateCombinedBat() throws IOException {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    alert.setContentText("This will combine the selected .bat file(s) into one(1) .bat file.\n"
      + "Do not combine Trim-Copy .bat file(s) with Trim & Re-encode .bat file(s).\n"
      + "Please make sure the selected .bat file(s) either belong to\n"
      + "Trim-Copy only OR Trim & Re-encode only and not both.\n"
      + "If that is not the case, please do not proceed and\n"
      + "make the necessary amendment before trying again.\n\n"
      + "Are you sure you want to continue?"
    );
    alert.setHeaderText("Combine batch (.bat) file(s)");

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      Path cmdPath = Paths.get(batchFileFolderTextField.getText(), combinedNameTextField.getText() + ".bat");
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(cmdPath.toString(), true))) {
        bw.write("@chcp 65001");
        bw.newLine();
        bw.write("@echo [%date% %time%] Process started.");
        bw.newLine();
        bw.write("@echo Assuming 1s is used to process 1s of video, the whole process will take"
          + " ~ " + estimateLabel.getText() + " to complete starting from the time above."
        );
        bw.newLine();
        for (String batPath: batPaths) {
          File batFile = new File(batPath);
          List<String> cmdList = Files.readAllLines(batFile.toPath());
          for (String cmd: cmdList) {
            if (!Arrays.asList("@chcp", "chcp", "@pause", "pause", "@echo.").stream().anyMatch(cmd::startsWith)) {
              if (cmd.startsWith("@echo")) {
                bw.write("@echo.");
                bw.newLine();
                bw.write(cmd);
              } else {
                // cmd is ffmpeg command. for now let's just do placing all output into 1 same
                // destination like the old way. or if the user does not select that option,
                // then just copy the old command.
                if (putInFolderCheckBox.isSelected()) {
                  String folderString = putInFolderTextField.getText();
                  if (folderString.isBlank()) {
                    bw.write(cmd);
                  } else {
                    List<String> inOut = quotedStrings(cmd);
                    Path outPath = Paths.get(inOut.get(1));
                    String outFileName = outPath.getFileName().toString();
                    Path newOutPath = Paths.get(folderString, outFileName);
                    String newcmd = cmd.replace(outPath.toString(), newOutPath.toString());
                    bw.write(newcmd);
                  }
                } else {
                  bw.write(cmd);
                }
              }
              bw.newLine();
            }
          }
        }
        if (shutdownCheckBox.isSelected()) {
          bw.newLine();
          bw.write(shutdownCmd(durationInSeconds()));
        } else {
          bw.write("@pause");
        }
        System.out.println(cmdPath.toString() + " generated!");
        alert.setAlertType(AlertType.INFORMATION);
        alert.setContentText(cmdPath.toString() + " generated!");
        alert.showAndWait();
      }
    }
  }

  private String shutdownCmd(long timeout) {
    String ls = System.lineSeparator();
    StringBuilder sb = new StringBuilder();
    sb.append("@echo off" + ls + "echo." + ls + "shutdown /s /t ");
    sb.append(String.valueOf(timeout));
    sb.append(ls + "echo Would you like to abort the shutdown?");
    sb.append(ls + "choice /t 9999 /c yn /d n /m \"PLEASE DECIDE NOW!!!\"");
    sb.append(ls + "if %errorlevel% equ 1 goto CONTINUE");
    sb.append(ls + "if %errorlevel% equ 2 goto END");
    sb.append(ls + ls + ":END" + ls + "exit" + ls + ls + ":CONTINUE");
    sb.append(ls + "shutdown /a" + ls + "echo Shutdown aborted" + ls + "pause");

    return sb.toString();
  }

  @FXML
  public void initialize() {
    batFileListView.setItems(batPaths);
    batFileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    removeButton.disableProperty().bind(Bindings.isEmpty(batFileListView.getSelectionModel().getSelectedItems()));
    clearButton.disableProperty().bind(Bindings.isEmpty(batPaths));
    putInFolderTextField.disableProperty().bind(putInFolderCheckBox.selectedProperty().not());
    allFolderButton.disableProperty().bind(putInFolderCheckBox.selectedProperty().not());
    BooleanBinding disableStart = Bindings.isEmpty(batPaths)
    .or(combinedNameTextField.textProperty().isEmpty())
    .or(batchFileFolderTextField.textProperty().isEmpty())
    .or(putInFolderCheckBox.selectedProperty().and(putInFolderTextField.textProperty().isEmpty())
    );
    startButton.disableProperty().bind(disableStart);

    durationHBox.setBorder(new Border(new BorderStroke(
      Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(2)
    )));
    dSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999));
    dSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
    dSpinner.setOnScroll(event -> {
      if (event.getDeltaY() > 0) {
        dSpinner.increment();
      } else if (event.getDeltaY() < 0) {
        dSpinner.decrement();
      }
    });
    hSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999));
    hSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
    hSpinner.setOnScroll(event -> {
      if (event.getDeltaY() > 0) {
        hSpinner.increment();
      } else if (event.getDeltaY() < 0) {
        hSpinner.decrement();
      }
    });
    mSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999));
    mSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
    mSpinner.setOnScroll(event -> {
      if (event.getDeltaY() > 0) {
        mSpinner.increment();
      } else if (event.getDeltaY() < 0) {
        mSpinner.decrement();
      }
    });
    sSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999));
    sSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
    sSpinner.setOnScroll(event -> {
      if (event.getDeltaY() > 0) {
        sSpinner.increment();
      } else if (event.getDeltaY() < 0) {
        sSpinner.decrement();
      }
    });

    resetButton.visibleProperty().bind(shutdownCheckBox.selectedProperty());
    normalizeButton.visibleProperty().bind(shutdownCheckBox.selectedProperty());
    durationLabel.visibleProperty().bind(shutdownCheckBox.selectedProperty());
    durationHBox.visibleProperty().bind(shutdownCheckBox.selectedProperty());

    batPaths.addListener((ListChangeListener<String>) change -> {
      long totalMillis = 0;
      for (String batPath: batPaths) {
        File batFile = new File(batPath);
        try {
          List<String> cmdList = Files.readAllLines(batFile.toPath());
          for (String cmd: cmdList) {
            if (cmd.startsWith("@ffmpeg")) {
              List<String> startEnd = timeStrings(cmd);
              Duration duration = Duration.between(
                LocalTime.parse(startEnd.get(0), formatter), 
                LocalTime.parse(startEnd.get(1), formatter)
              );
              totalMillis += duration.toMillis();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      Duration totalDuration = Duration.ofMillis(totalMillis);
      if (totalMillis > 0) {
        estimateLabel.setText(String.valueOf(totalDuration.toDaysPart()) + " days "
        + String.valueOf(totalDuration.toHoursPart()) + " hours "
        + String.valueOf(totalDuration.toMinutesPart()) + " minutes "
        + String.valueOf(totalDuration.toSecondsPart()) + " seconds");
      }
      if (batPaths.size() == 0) {
        estimateLabel.setText(null);
      }
    });
  }

  private void numericalizeSpinner() {
    String dval = dSpinner.getEditor().getText();
    String hval = hSpinner.getEditor().getText();
    String mval = mSpinner.getEditor().getText();
    String sval = sSpinner.getEditor().getText();
    if (dval.isBlank() || dval.matches(".*\\D.*")) {
      dSpinner.getValueFactory().setValue(0);
    }
    if (hval.isBlank() || hval.matches(".*\\D.*")) {
      hSpinner.getValueFactory().setValue(0);
    }
    if (mval.isBlank() || mval.matches(".*\\D.*")) {
      mSpinner.getValueFactory().setValue(0);
    }
    if (sval.isBlank() || sval.matches(".*\\D.*")) {
      sSpinner.getValueFactory().setValue(0);
    }
  }

  private long durationInSeconds() {
    numericalizeSpinner();
    int dval = dSpinner.getValue();
    int hval = hSpinner.getValue();
    int mval = mSpinner.getValue();
    int sval = sSpinner.getValue();
    return dval * 24 * 3600 + hval * 3600 + mval * 60 + sval;
  }

  @FXML
  private void normalizeDuration() throws IOException {
    Duration duration = Duration.ofSeconds(durationInSeconds());
    dSpinner.getValueFactory().setValue((int) duration.toDaysPart());
    hSpinner.getValueFactory().setValue(duration.toHoursPart());
    mSpinner.getValueFactory().setValue(duration.toMinutesPart());
    sSpinner.getValueFactory().setValue(duration.toSecondsPart());
  }

  @FXML
  private void resetDuration() throws IOException {
    dSpinner.getValueFactory().setValue(0);
    hSpinner.getValueFactory().setValue(0);
    mSpinner.getValueFactory().setValue(0);
    sSpinner.getValueFactory().setValue(0);
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

  private static List<String> quotedStrings(String text) {
    Pattern pattern = Pattern.compile("\"([^\"]*)\"");
    Matcher matcher = pattern.matcher(text);
    List<String> result = new ArrayList<>();
    while(matcher.find()) {
      result.add(matcher.group(1));
    }
    return result;
  }

  private static List<String> timeStrings(String text) {
    Pattern pattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    Matcher matcher = pattern.matcher(text);
    List<String> result = new ArrayList<>();
    while(matcher.find()) {
      result.add(matcher.group(0));
    }
    return result;
  }

  private void selectFolder(
    ActionEvent event, 
    String title,
    String lastUsedDir,
    TextField textField
  ) throws IOException {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    File initialDir = new File(preferences.get(lastUsedDir, System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    directoryChooser.setTitle(title);
    directoryChooser.setInitialDirectory(initialDir);
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
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setTitle("Select .bat file(s)");
    fileChooser.setInitialDirectory(initialDir);
    fileChooser.getExtensionFilters().addAll(
      new ExtensionFilter("All files", "*.*"),
      new ExtensionFilter("Batch files", "*.bat")
    );
    fileChooser.setSelectedExtensionFilter(new ExtensionFilter("Batch files", "*.bat"));
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
