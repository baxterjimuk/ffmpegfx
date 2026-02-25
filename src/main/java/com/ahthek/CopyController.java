package com.ahthek;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class CopyController {
  Preferences preferences = Preferences.userNodeForPackage(UfcController.class);

  private ObservableList<Path> sourcePaths = FXCollections.observableArrayList();

  @FXML
  private Button sourceFileButton, sourceFolderButton, removeButton, clearButton, destinationButton, startButton;

  @FXML
  private ListView<Path> sourceListView;

  @FXML
  private TextField destinationTextField;

  @FXML
  private ToggleGroup actionGroup;

  @FXML
  private Label sizeLabel;

  @FXML
  private CheckBox shutdownCheckBox, directoryCheckBox;

  @FXML
  private HBox timerHBox;

  @FXML
  private Spinner<Integer> dSpinner, hSpinner, mSpinner, sSpinner;
  
  @FXML
  public void initialize() {
    sourceListView.setItems(sourcePaths);

    startButton.disableProperty().bind(Bindings.isEmpty(sourcePaths)
      .or(destinationTextField.textProperty().isEmpty())
    );

    sourcePaths.addListener((ListChangeListener<Path>) change -> {
      if (sourcePaths.size() == 0) {
        sizeLabel.setText(null);
      } else {
        try {
          long sourceSize = getSelectedSizeNio(sourcePaths.toArray(new Path[0]));
          sizeLabel.setText("(" + formatFileSize(sourceSize) + ")");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    timerHBox.disableProperty().bind(shutdownCheckBox.selectedProperty().not());

    Arrays.asList(dSpinner, hSpinner, mSpinner, sSpinner).forEach(spinner -> {
      spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999));
      spinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
      spinner.setOnScroll(event -> {
        if (event.getDeltaY() > 0) {
          spinner.increment();
        } else if (event.getDeltaY() < 0) {
          spinner.decrement();
        }
      });
    });
  }

  private long getSelectedSizeNio(Path[] paths) throws IOException {
    long totalSize = 0;
    for (Path path : paths) {
      if (Files.exists(path)) {
        if (Files.isRegularFile(path)) {
          totalSize += Files.size(path);
        } else {
          // For directory, use Files.walk() to sum sizes
          totalSize += Files.walk(path).filter(p -> p.toFile().isFile()).mapToLong(p -> {
            try {
              return Files.size(p);
            } catch (IOException e) {
              return 0L;
            }
          }).sum();
        }
      }
    }
    return totalSize;
  }

  @FXML
  private void createBat(ActionEvent event) {
    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String action = ((RadioButton) actionGroup.getSelectedToggle()).getText();
    String destination = destinationTextField.getText();

    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save File");
    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));
    fileChooser.setInitialFileName(now + "_" + action + ".bat");
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setInitialDirectory(initialDir);
    File file = fileChooser.showSaveDialog(stage);
    if (file != null) {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      alert.setTitle("FFMPEGFX");
      alert.setHeaderText("Create batch file");
      alert.setContentText("\"" + file.getName() + "\" will be created in\n" 
        + "\"" + file.getParent() 
        + "\"\n\nAre you sure you want to continue?"
      );
      Optional<ButtonType> result = alert.showAndWait();
      if (result.isPresent() && result.get() == ButtonType.OK) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file.getAbsolutePath(), true))) {
          writer.println("@chcp 65001");
          writer.println("@echo [%date% %time%] Process started. "
            + sizeLabel.getText().replaceAll("[()]", "") + " to be copied."
          );
          for (Path path : sourcePaths) {
            if (action.equals("Robo-Copy")) {
              robocopy(path, writer, destination);
            } else {
              robomove(path, writer, destination);
            }
          }
          if (shutdownCheckBox.isSelected()) {
            writer.println("@shutdown -s -t " + durationInSeconds());
            Duration duration = Duration.ofSeconds(durationInSeconds());
            writer.println("@echo [%date% %time%] Process completed! Auto-shutdown in ~ "
              + String.valueOf(duration.toDaysPart()) + " days "
              + String.valueOf(duration.toHoursPart()) + " hours "
              + String.valueOf(duration.toMinutesPart()) + " minutes "
              + String.valueOf(duration.toSecondsPart()) + " seconds."
            );
          }
          writer.println("@pause");
          writer.close();
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
  private void clearSource() {
    sourcePaths.clear();
  }

  @FXML
  private void removeSource() {
    sourcePaths.removeAll(sourceListView.getSelectionModel().getSelectedItems());
    sourceListView.getSelectionModel().clearSelection();
  }

  @FXML
  private void addSourceFile(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setTitle("Select file(s)");
    fileChooser.setInitialDirectory(initialDir);
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    List<File> files = fileChooser.showOpenMultipleDialog(stage);
    if (files != null) {
      for (File file: files) {
        if(!sourcePaths.contains(file.toPath())){
          sourcePaths.add(file.toPath());
        }
      }
      preferences.put("lastUsedDir", files.getLast().getParent());
    }
  }

  @FXML
  private void addSourceFolder(ActionEvent event) {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    directoryChooser.setTitle("Select folder");
    directoryChooser.setInitialDirectory(initialDir);
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    File file = directoryChooser.showDialog(stage);
    if (file != null) {
      if (!sourcePaths.contains(file.toPath())) {
        sourcePaths.add(file.toPath());
      }
      preferences.put("lastUsedDir", file.getAbsolutePath());
    }
  }

  @FXML
  private void selectDestination(ActionEvent event) {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    directoryChooser.setTitle("Select destination");
    directoryChooser.setInitialDirectory(initialDir);
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    File file = directoryChooser.showDialog(stage);
    if (file != null) {
      destinationTextField.setText(file.getAbsolutePath());
      preferences.put("lastUsedDir", file.getAbsolutePath());
    }
  }

  public static String formatFileSize(long bytes) {
    if (bytes < 0) {
      return "Invalid size";
    }

    // Define the units and the 1024 multiplier (binary prefix standard)
    final long KILOBYTE = 1024;
    final long MEGABYTE = KILOBYTE * 1024;
    final long GIGABYTE = MEGABYTE * 1024;
    final long TERABYTE = GIGABYTE * 1024;

    // Use the largest possible unit that the size can be divided into
    if (bytes < KILOBYTE) {
      return String.format("%,d bytes", bytes);
    } else if (bytes < MEGABYTE) {
      return String.format("%,.1f KB", (double) bytes / KILOBYTE);
    } else if (bytes < GIGABYTE) {
      return String.format("%,.1f MB", (double) bytes / MEGABYTE);
    } else if (bytes < TERABYTE) {
      return String.format("%,.1f GB", (double) bytes / GIGABYTE);
    } else {
      return String.format("%,.1f TB", (double) bytes / TERABYTE);
    }
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
  private void normalizeDuration() {
    Duration duration = Duration.ofSeconds(durationInSeconds());
    dSpinner.getValueFactory().setValue((int) duration.toDaysPart());
    hSpinner.getValueFactory().setValue(duration.toHoursPart());
    mSpinner.getValueFactory().setValue(duration.toMinutesPart());
    sSpinner.getValueFactory().setValue(duration.toSecondsPart());
  }

  @FXML
  private void resetDuration() {
    dSpinner.getValueFactory().setValue(0);
    hSpinner.getValueFactory().setValue(0);
    mSpinner.getValueFactory().setValue(0);
    sSpinner.getValueFactory().setValue(0);
  }

  private void robocopy(Path path, PrintWriter writer, String destination) {
    if (Files.isDirectory(path)) {
      if (directoryCheckBox.isSelected()) {
        writer.println("robocopy \"" + path.toString() + "\" \"" 
          + Paths.get(destination, path.getFileName().toString()).toString() 
          + "\" /E"
        );
      } else {
        writer.println("robocopy \"" + path.toString() + "\" \"" + destination + "\" /E");
      }
    } else {
      writer.println("robocopy \"" + path.getParent().toString() + "\" \""
        + destination + "\" \"" + path.getFileName().toString() + "\""
      );
    }
  }

  private void robomove(Path path, PrintWriter writer, String destination) {
    if (Files.isDirectory(path)) {
      if (directoryCheckBox.isSelected()) {
        writer.println("robocopy \"" + path.toString() + "\" \"" 
          + Paths.get(destination, path.getFileName().toString()).toString() 
          + "\" /MOVE /E"
        );
      } else {
        writer.println("robocopy \"" + path.toString() + "\" \"" + destination + "\" /MOVE /E");
      }
    } else {
      writer.println("robocopy \"" + path.getParent().toString() + "\" \""
        + destination + "\" \"" + path.getFileName().toString() + "\" /MOV"
      );
    }
  }
}
