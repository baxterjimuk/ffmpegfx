package com.ahthek;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.apache.commons.io.FilenameUtils;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class SimpleController {
  // need to create a class for important ffmpeg property
  Preferences preferences = Preferences.userNodeForPackage(UfcController.class);
  int hmax, vmax;

  private ObservableList<Path> videoPaths = FXCollections.observableArrayList();
  private ObservableList<String> vcodecs = FXCollections.observableArrayList(
    "H.265", "H.264", "Copy", "No Video"
  );
  private ObservableList<String> acodecs = FXCollections.observableArrayList(
    "aac", "mp3", "Copy", "No Audio"
  );
  private ObservableList<String> containers = FXCollections.observableArrayList("mkv", "mp4");
  private ObservableList<String> actions = FXCollections.observableArrayList(
    "Join files", "Create batch file"
  );

  ExtensionFilter allFilter = new ExtensionFilter("All files", "*.*");
  ExtensionFilter videoFilter = new ExtensionFilter("Video files", 
    "*.mkv", "*.mp4", "*.mov", "*.m4v", "*.avi"
  );
  ExtensionFilter batFilter = new ExtensionFilter("Batch files", "*.bat");

  @FXML
  private ListView<Path> inputListView;

  @FXML
  private ComboBox<String> vcodecComboBox, acodecComboBox, containerComboBox, actionComboBox;

  @FXML
  private Button addButton, removeButton, clearButton, moveUpButton, moveDownButton, startButton;

  @FXML
  private TextField hMaxHeightTextField, vMaxHeightTextField;

  @FXML
  private Label estimateLabel;

  @FXML
  private CheckBox skipCheckBox;

  @FXML
  public void initialize() {
    vcodecComboBox.setItems(vcodecs);
    vcodecComboBox.getSelectionModel().selectFirst();
    acodecComboBox.setItems(acodecs);
    acodecComboBox.getSelectionModel().selectFirst();
    containerComboBox.setItems(containers);
    containerComboBox.getSelectionModel().selectFirst();
    actionComboBox.setItems(actions);
    actionComboBox.getSelectionModel().selectFirst();

    inputListView.setItems(videoPaths);
    inputListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    removeButton.disableProperty().bind(Bindings.isEmpty(inputListView.getSelectionModel().getSelectedItems()));
    removeButton.setOnAction(e -> {
      videoPaths.removeAll(inputListView.getSelectionModel().getSelectedItems());
      inputListView.getSelectionModel().clearSelection();
    });
    
    clearButton.disableProperty().bind(Bindings.isEmpty(videoPaths));
    clearButton.setOnAction(e -> videoPaths.clear());
    
    Arrays.asList(moveUpButton, moveDownButton).forEach(button -> {
      button.disableProperty().bind(Bindings.size(videoPaths).lessThan(2)
      .or(Bindings.isEmpty(inputListView.getSelectionModel().getSelectedItems())));
    });

    addButton.setOnAction(e -> {
      FileChooser fileChooser = new FileChooser();
      File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
      if (!initialDir.exists()) {
        initialDir = new File(System.getProperty("user.home"));
      }
      fileChooser.setTitle("Select video file");
      fileChooser.setInitialDirectory(initialDir);
      fileChooser.getExtensionFilters().addAll(allFilter, videoFilter);
      fileChooser.setSelectedExtensionFilter(videoFilter);
      Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
      List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
      if (selectedFiles != null) {
        selectedFiles.forEach(file -> {
          if (!videoPaths.contains(file.toPath())) {
            videoPaths.add(file.toPath());
          }
        });
        preferences.put("lastUsedDir", selectedFiles.getLast().getParent());
      }
    });

    Arrays.asList(hMaxHeightTextField, vMaxHeightTextField).forEach(textField -> {
      textField.setTextFormatter(new TextFormatter<>((UnaryOperator<Change>) change -> {
        String newText = change.getControlNewText();
        if (newText.length() <= 4 && newText.matches("\\d*")) {
          return change;
        }
        return null;
      }));
      textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue) {
          Platform.runLater(() -> textField.selectAll());
        }
      });
    });

    startButton.disableProperty().bind(Bindings.isEmpty(videoPaths)
    .or(Bindings.size(videoPaths).lessThan(2)
    .and(Bindings.equal(0, actionComboBox.getSelectionModel().selectedIndexProperty())))
    );

    videoPaths.addListener((ListChangeListener<Path>) change -> {
      List<Duration> durations = new ArrayList<>();
      videoPaths.forEach(path -> {
        durations.add(Duration.ofMillis((long) videoDetails(path).get(0)));
      });
      Duration totalDuration = durations.stream().reduce(Duration.ZERO, Duration::plus);
      estimateLabel.setText(String.valueOf(totalDuration.toDaysPart()) + " days "
        + String.valueOf(totalDuration.toHoursPart()) + " hours "
        + String.valueOf(totalDuration.toMinutesPart()) + " minutes "
        + String.valueOf(totalDuration.toSecondsPart()) + " seconds"
      );
      if (videoPaths.size() == 0) {
        estimateLabel.setText(null);
      }
    });
  }

  public static boolean isValidPath(String path) {
    try {
      Paths.get(path);
    } catch (InvalidPathException | NullPointerException e) {
      return false;
    }
    return true;
  }

  @FXML
  private void moveUp() {
    ObservableList<Integer> selectedIndices = inputListView.getSelectionModel().getSelectedIndices();
    if (selectedIndices.isEmpty()) return;

    ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
    FXCollections.sort(indicesCopy); // ascending

    // If any selected item is at the top, do nothing
    if (indicesCopy.get(0) == 0) return;

    for (int index : indicesCopy) {
      Path current = videoPaths.get(index);
      videoPaths.set(index, videoPaths.get(index - 1));
      videoPaths.set(index - 1, current);

      inputListView.getSelectionModel().clearSelection(index);
      inputListView.getSelectionModel().select(index - 1);
    }
  }

  @FXML
  private void moveDown() {
    ObservableList<Integer> selectedIndices = inputListView.getSelectionModel().getSelectedIndices();
    if (selectedIndices.isEmpty()) return;

    ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
    FXCollections.sort(indicesCopy, (a, b) -> b - a); // descending

    // If any selected item is at the bottom, do nothing
    if (indicesCopy.get(0) == videoPaths.size() - 1) return;

    for (int index : indicesCopy) {
      Path current = videoPaths.get(index);
      videoPaths.set(index, videoPaths.get(index + 1));
      videoPaths.set(index + 1, current);

      inputListView.getSelectionModel().clearSelection(index);
      inputListView.getSelectionModel().select(index + 1);
    }
  }

  private void joinFiles(Alert alert, Stage stage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save File");
    fileChooser.setInitialFileName(
      FilenameUtils.getBaseName(videoPaths.getFirst().toString()) + "_joined.mkv"
    );
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setInitialDirectory(initialDir);
    fileChooser.getExtensionFilters().addAll(allFilter, videoFilter);
    fileChooser.setSelectedExtensionFilter(videoFilter);
    File file = fileChooser.showSaveDialog(stage);
    if (file != null) {
      if (file.exists()) {
        TextArea textArea = new TextArea("Apologies. Strictly unable to proceed as \""
          + file.getAbsolutePath() + "\" already exists. Please choose a different file name "
          + "or if you insist on sticking to an existing file name, "
          + "choose a different location to save the file instead."
        );
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        alert.setAlertType(AlertType.ERROR);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
        return;
      } else {
        alert.setHeaderText("Join files");
        alert.setContentText("""
          Are you sure you want to continue?

          * Make sure all the files to be joined are of the same codecs and dimensions
        """);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
          try {
            Path tempFile = Files.createTempFile(null, null);
            PrintWriter writer = new PrintWriter(new FileWriter(tempFile.toFile(), true));
            videoPaths.forEach(path -> {
              writer.println("file '" + path.toString() + "'");
            });
            writer.close();
            /*
              https://bytedeco.org/javacpp-presets/ffmpeg/apidocs/org/bytedeco/ffmpeg/ffmpeg.html
            */
            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            ProcessBuilder pb = new ProcessBuilder(
              ffmpeg, "-y", "-hide_banner", "-loglevel", "warning", "-stats",
              "-f", "concat", "-safe", "0", "-i", tempFile.toString(), "-c", "copy", file.getAbsolutePath()
            );
            pb.inheritIO().start().waitFor();
            Files.delete(tempFile);
            alert.setAlertType(AlertType.INFORMATION);
            TextArea textArea = new TextArea("Process completed!\n\n\""
              + file.getName() + "\" can be found at\n\"" + file.getParent() + "\""
            );
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
          } catch (IOException | InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private List<Object> videoDetails(Path path) {
    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path.toFile())) {
      List<Object> details = new ArrayList<>();
      avutil.av_log_set_level(avutil.AV_LOG_QUIET);
      grabber.start();
      details.add(grabber.getLengthInTime() / 1000);
      details.add(grabber.getImageWidth());
      details.add(grabber.getImageHeight());
      details.add(grabber.getImageHeight() > grabber.getImageWidth() ? "v": "h");
      grabber.stop();
      grabber.release();
      grabber.close();
      return details;
    } catch (Exception e) {
      e.printStackTrace();
      return List.of();
    }
  }

  private String formatDuration(List<Object> details) {
    long duration = (long) details.get(0);
    long hours = duration / (1000 * 60 * 60);
    long minutes = (duration / (1000 * 60)) % 60;
    long seconds = (duration / 1000) % 60;
    long ms = duration % 1000;
    return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
  }
  
  private void createBat(String sekarang, Alert alert, Stage stage) {
    hmax = hMaxHeightTextField.getText().isBlank() ? 0 : Integer.parseInt(hMaxHeightTextField.getText());
    vmax = vMaxHeightTextField.getText().isBlank() ? 0 : Integer.parseInt(vMaxHeightTextField.getText());

    String numberFormat = "%0" + String.valueOf(videoPaths.size()).length() + "d";
    String vcodec = switch(vcodecComboBox.getValue()) {
      case "H.265" -> "-c:v libx265 -x265-params log-level=warning";
      case "H.264" -> "-c:v libx264";
      case "Copy" -> "-c:v copy";
      default -> "-vn";
    };
    String acodec = switch(acodecComboBox.getValue()) {
      case "aac" -> "-c:a aac";
      case "mp3" -> "-c:a libmp3lame";
      case "Copy" -> "-c:a copy";
      default -> "-an";
    };
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save File");
    fileChooser.setInitialFileName(sekarang + ".bat");
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setInitialDirectory(initialDir);
    fileChooser.getExtensionFilters().addAll(allFilter, batFilter);
    fileChooser.setSelectedExtensionFilter(batFilter);
    File file = fileChooser.showSaveDialog(stage);
    if (file != null) {
      alert.setHeaderText("Create batch file");
      alert.setContentText("\"" + file.getName() + "\" will be created in\n"
        + "\"" + file.getParent() + "\"\n\nAre you sure you want to continue?"
      );
      Optional<ButtonType> result = alert.showAndWait();
      if (result.isPresent() && result.get() == ButtonType.OK) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file.getAbsolutePath(), true))) {
          writer.println("@echo off");
          writer.println("chcp 65001");
          writer.println("echo [%date% %time%] Process started.");
          writer.println("echo Assuming 1s is used to process 1s of video, the whole process will take"
          + " ~ " + estimateLabel.getText() + " to complete starting from the time above.");
          for (int i = 0; i < videoPaths.size(); i++) {
            List<Object> details = videoDetails(videoPaths.get(i));
            String scaleVF = "";
            if (details.get(3).equals("h")) {
              if (hmax > 0 && hmax < ((int) details.get(2))) {
                scaleVF = "-vf \"scale=-2:" + hmax + "\" ";
              }
            } else {
              if (vmax > 0 && vmax < ((int) details.get(2))) {
                scaleVF = "-vf \"scale=-2:" + vmax + "\" ";
              }
            }
            if (scaleVF.equals("") && skipCheckBox.isSelected()) {
              continue;
            }
            String in = videoPaths.get(i).toString();
            Path parent = videoPaths.get(i).getParent();
            String base = FilenameUtils.getBaseName(in);
            String out = sekarang + "_" + base + "." + containerComboBox.getValue(); 
            writer.println("echo.");
            writer.println("cd /d \"" + parent.toString() + "\"");
            writer.println("echo (" + String.format(numberFormat, i + 1) + "/" + videoPaths.size() 
              + ") [" + formatDuration(details) + "] " + FilenameUtils.getName(in)
            );
            writer.println("ffmpeg -y -hide_banner -loglevel warning -stats -i \""
              + FilenameUtils.getName(in) + "\" " + scaleVF + vcodec + " " + acodec + " \"" + out + "\""
            );
          }
          writer.println("echo.");
          writer.println("pause");
          alert.setAlertType(AlertType.INFORMATION);
          TextArea textArea = new TextArea("Process completed!\n\n\""
            + file.getName() + "\" can be found at\n\"" + file.getParent() + "\""
          );
          textArea.setEditable(false);
          textArea.setWrapText(true);
          textArea.setMaxWidth(Double.MAX_VALUE);
          textArea.setMaxHeight(Double.MAX_VALUE);
          alert.getDialogPane().setContent(textArea);
          alert.showAndWait();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    
  }

  @FXML
  private void startAction() {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    if (actionComboBox.getSelectionModel().getSelectedIndex() == 0) {
      joinFiles(alert, stage);
    } else {
      String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      createBat(now, alert, stage);
    }
  }
}