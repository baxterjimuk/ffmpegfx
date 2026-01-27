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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.control.SelectionMode;
import javafx.collections.ObservableList;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import org.bytedeco.ffmpeg.global.avcodec; // will need later for constant
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import java.util.prefs.Preferences;

public class Controller {
  Preferences preferences = Preferences.userNodeForPackage(Controller.class);

  // start with 0 so that if we want to call 'B', we can use atoz[2] instead of atoz[1]
  // in other words, 'B' is the number 2 letter in the list of alphabet
  String[] atoz = ("0ABCDEFGHIJKLMNOPQRSTUVWXYZ").split("");

  public static String formatDuration(String start, String end) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    Duration duration = Duration.between(
      LocalTime.parse(start, formatter), 
      LocalTime.parse(end, formatter)
    );
      long millis = duration.toMillis();

      long hours = millis / (1000 * 60 * 60);
      long minutes = (millis / (1000 * 60)) % 60;
      long seconds = (millis / 1000) % 60;
      long ms = millis % 1000;

      return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
  }

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
  private void switchToUfc() throws IOException {
    MainApp.setRoot("ufc");
  }

  @FXML
  private Button removeEdlFileButton, clearButton, startButton;

  @FXML
  private RadioButton trimCopyRadioButton;

  @FXML
  private void generateBatFile() throws IOException {
    StringBuilder cmd = new StringBuilder();
    Alert alert = new Alert(AlertType.CONFIRMATION);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    alert.setHeaderText("Generate batch (.bat) file(s)");
    alert.setContentText("Are you sure you want to continue?");

    Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.OK){
      avutil.av_log_set_level(avutil.AV_LOG_QUIET);
      // loop through each edl file
      for (String edlPath : filePaths) {
        File edlFile = new File(edlPath);
        String edlFileName = edlFile.getName();
        String videoFileName = FilenameUtils.getBaseName(edlFileName);
        String parent = edlFile.getParent();
        
        String batPath = edlPath.replace(".edl", ".bat");
        
        int input_acodec = avcodec.AV_CODEC_ID_AAC, input_vcodec = avcodec.AV_CODEC_ID_H265;
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(FilenameUtils.removeExtension(edlPath))) {
          grabber.start();
          /* System.out.println(
            "VIDEO: " + grabber.getVideoCodecName() + " (" + grabber.getVideoCodec() + ")"
            + "\tAUDIO: " + grabber.getAudioCodecName() + " (" + grabber.getAudioCodec() + ")"
            + "\t" + videoFileName
          ); */
          input_acodec = grabber.getAudioCodec();
          input_vcodec = grabber.getVideoCodec();
          grabber.stop();
          grabber.close();
        } catch (FrameGrabber.Exception e) {
          e.printStackTrace();
        }

        Files.deleteIfExists(Paths.get(batPath));
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(batPath, true))) {
          bw.write("@chcp 65001");
          bw.newLine();
          bw.write("@echo.");
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

              String idx, codec;
              if (trimCopyRadioButton.isSelected()) {
                idx = atoz[count];
                codec = " -c copy ";
              } else {
                idx = String.format("%02d", count);
                // even if user decides to not trim-copy, the input could actually already in h265/hevc
                // and aac audio. in this case we perform the same thing as trim-copy.
                if (input_acodec == avcodec.AV_CODEC_ID_AAC && input_vcodec == avcodec.AV_CODEC_ID_H265) {
                  codec = " -c copy ";
                } else {
                  codec = " -c:v libx265 -x265-params log-level=error -c:a aac ";
                }
              }

              // just transcode to mkv always since we're going to re-encode both video and audio if
              // we're not doing any sort of trim-copy. mp4 while has no black/blank frame being inserted
              // at the beginning of output with copied audio, it tends to not seek friendly (choppy seeking).
              // mkv has also served use well all these while without much playback issue. if anything such as
              // only trimming at keyframes and not exact timestamp etc, can always perform another re-transcode.
              String out = Paths.get(parent, FilenameUtils.removeExtension(videoFileName) + "_" + idx + ".mkv").toString();

              cmd.setLength(0);
              cmd.append("@echo [");
              cmd.append(codec.equals(" -c copy ") ? "copying " : "transcoding ");
              cmd.append(formatDuration(arr[0], arr[1]) + "] ");
              cmd.append(FilenameUtils.getName(out));
              cmd.append(System.lineSeparator());
              cmd.append("@ffmpeg -y -hide_banner -loglevel warning -stats -ss ");
              cmd.append(arr[0]);
              cmd.append(" -to ");
              cmd.append(arr[1]);
              cmd.append(" -i ");
              cmd.append("\"" + FilenameUtils.removeExtension(edlPath) + "\"");
              cmd.append(codec);
              cmd.append("\"" + out + "\"");
              cmd.append(System.lineSeparator());
              cmd.append("@echo.");

              bw.write(cmd.toString());
              bw.newLine();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          bw.write("@pause");
          System.out.println(batPath + " generated!");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      alert.setAlertType(AlertType.INFORMATION);
      alert.setContentText("Process completed!");
      alert.showAndWait();
    }
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
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setTitle("Select .edl File(s)");
    fileChooser.setInitialDirectory(initialDir);
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
    }
  }

  @FXML
  private void clearFiles(ActionEvent event) throws IOException {
    filePaths.clear();
  }
}
