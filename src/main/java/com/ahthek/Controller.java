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
import org.bytedeco.ffmpeg.global.avcodec; // will need later for constant
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

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
        System.out.println("Audio Codec (ID, Name): " + grabber.getAudioCodec() + ", " + grabber.getAudioCodecName());
        System.out.println("Video Codec (ID, Name): " + grabber.getVideoCodec() + ", " + grabber.getVideoCodecName());
        input_acodec = grabber.getAudioCodec();
        input_vcodec = grabber.getVideoCodec();
        // System.out.println("AV_CODEC_ID_HEVC = " + avcodec.AV_CODEC_ID_HEVC);
        // System.out.println("AV_CODEC_ID_H265 = " + avcodec.AV_CODEC_ID_H265);
        // System.out.println("AV_CODEC_ID_H264 = " + avcodec.AV_CODEC_ID_H264);
        // System.out.println("AV_CODEC_ID_AAC = " + avcodec.AV_CODEC_ID_AAC);
        grabber.stop();
        grabber.close();
      } catch (FrameGrabber.Exception e) {
        e.printStackTrace();
      }

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

            // String idx = trimCopyRadioButton.isSelected() ? atoz[count] : String.format("%02d", count);
            // String codec = trimCopyRadioButton.isSelected() ? " -c copy " : " -c:v libx265 -c:a aac ";

            String idx, codec, output_acodec, output_vcodec;
            if (trimCopyRadioButton.isSelected()) {
              idx = atoz[count];
              codec = " -c copy ";
            } else {
              idx = String.format("%02d", count);
              if (input_acodec == avcodec.AV_CODEC_ID_AAC && input_vcodec == avcodec.AV_CODEC_ID_H265) {
                // if audio is aac and video is hevc/h265, just copy. this is more towards making the
                // command cleaner. actually the code in else block is enough.
                codec = " -c copy ";
              } else {
                // maybe audio is not aac but video is hevc/h265 and thus only audio need to be converted
                // OR audio is aac but video is not hevc/h265 and thus only video need to be converted
                // OR both audio is not aac and video is not hevc/h265 and thus both need to be converted
                output_acodec = input_acodec == avcodec.AV_CODEC_ID_AAC ? " -c:a copy " : " -c:a aac ";
                output_vcodec = input_vcodec == avcodec.AV_CODEC_ID_H265 ? " -c:v copy" : " -c:v libx265";
                codec = output_vcodec + output_acodec;
              }
            }

            String out = Paths.get(parent, FilenameUtils.removeExtension(videoFileName) + "_" + idx + ".mkv").toString();

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
