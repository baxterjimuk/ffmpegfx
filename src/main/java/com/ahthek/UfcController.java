package com.ahthek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
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
  private TextField mltFileTextField, txtFileTextField, videoFileTextField;

  @FXML
  private Button startButton;

  ExtensionFilter allFilter = new ExtensionFilter("All files", "*.*");

  @FXML
  private void selectVideoFile(ActionEvent event) throws IOException {
    ExtensionFilter videoFilter = new ExtensionFilter("Video files", 
      "*.mkv", "*.mp4", "*.mov", "*.m4v", "*.avi"
    );
    selectFile(event, "Select video", List.of(allFilter, videoFilter), videoFilter, videoFileTextField);
  }

  @FXML
  private void selectMltFile(ActionEvent event) throws IOException {
    ExtensionFilter mltFilter = new ExtensionFilter("MLT files", "*.mlt");
    selectFile(event, "Select .mlt file", List.of(allFilter, mltFilter), mltFilter, mltFileTextField);
  }

  @FXML
  private void selectChapterFile(ActionEvent event) throws IOException {
    ExtensionFilter txtFilter = new ExtensionFilter("TXT files", "*.txt");
    selectFile(event, "Select .txt file", List.of(allFilter, txtFilter), txtFilter, txtFileTextField);
  }

  private void selectFile(
    ActionEvent event, String title, List<ExtensionFilter> filters, ExtensionFilter selectedFilter, TextField textField
  ) throws IOException {
    FileChooser fileChooser = new FileChooser();
    File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
    if (!initialDir.exists()) {
      initialDir = new File(System.getProperty("user.home"));
    }
    fileChooser.setTitle(title);
    fileChooser.setInitialDirectory(initialDir);
    fileChooser.getExtensionFilters().addAll(filters);
    fileChooser.setSelectedExtensionFilter(selectedFilter);
    Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);
    if (selectedFile != null) {
      textField.setText(selectedFile.getAbsolutePath());
      preferences.put("lastUsedDir", selectedFile.getParent());
    }
  }

  public void initialize() {
    BooleanBinding blankTextFields = videoFileTextField.textProperty().isEmpty()
    .or(mltFileTextField.textProperty().isEmpty())
    .or(txtFileTextField.textProperty().isEmpty());
    startButton.disableProperty().bind(blankTextFields);
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

  /* 
    Actually our script isn't really portable because we're creating .bat file which can only be run
    on Windows. If we're to run this on Linux, we'll have to create a .sh file instead. So this is also
    not good. Best is to just process everything via the application.
    Also in the future there's probably no need for a .mlt file created using shotcut. As long as we have
    the start timestamp and end timestamp of each section that we can get using any video player will suffice.
  */
  @FXML
  private void generateFiles() throws IOException {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Generate files");
    alert.setContentText("This will generate the following files:\n"
      + "1. A .bat file containing ffmpeg commands\n"
      + "2. A .txt file containing the list of temporary output to be joined\n"
      + "3. A .ffmetadata chapter file"
    );
    alert.setHeaderText("Are you sure you want to continue?");

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      String parent = FilenameUtils.getFullPath(mltFileTextField.getText());
      String videoFileName = FilenameUtils.getName(videoFileTextField.getText());
      String mltBaseName = FilenameUtils.getBaseName(mltFileTextField.getText());
      Path cmdPath = Paths.get(parent, mltBaseName + ".bat");
      Path txtPath = Paths.get(parent, mltBaseName + " list.txt");
      Path ffmetadataPath = Paths.get(parent, mltBaseName + ".ffmetadata");
      Files.deleteIfExists(cmdPath);
      Files.deleteIfExists(txtPath);
      Files.deleteIfExists(ffmetadataPath);
      List<String> matches = Files.readAllLines(Paths.get(txtFileTextField.getText()));
      try (
        BufferedWriter cmdWriter = new BufferedWriter(new FileWriter(cmdPath.toString(), true));
        BufferedWriter txtWriter = new BufferedWriter(new FileWriter(txtPath.toString(), true));
        BufferedWriter ffmetadataWriter = new BufferedWriter(new FileWriter(ffmetadataPath.toString(), true));
      ) {
        ffmetadataWriter.write(";FFMETADATA1");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(mltFileTextField.getText());
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        NodeList playlistNodeList = root.getElementsByTagName("playlist");
        for (int i = 0; i < playlistNodeList.getLength(); i++) {
          if (playlistNodeList.item(i).getAttributes().getNamedItem("id").getNodeValue().equals("playlist0")) {
            NodeList entryNodeList = ((Element) playlistNodeList.item(i)).getElementsByTagName("entry");
            long sum = 0;
            for (int j = 0; j < entryNodeList.getLength(); j++) {
              // can start adding the ffmpeg command to trim the input into temporary files
              String tempOutputName = mltBaseName + " " + String.format("%02d", j + 1) + ".mkv";
              txtWriter.write("file '" + tempOutputName + "'");
              if (j != entryNodeList.getLength() - 1) {
                txtWriter.newLine();
              }
              Element entryElement = (Element) entryNodeList.item(j);
              String start = entryElement.getAttribute("in");
              String end = entryElement.getAttribute("out");
              StringBuilder sb = new StringBuilder();
              sb.append("ffmpeg -y -hide_banner -loglevel warning -stats -ss ");
              sb.append(start);
              sb.append(" -to ");
              sb.append(end);
              sb.append(" -i ");
              sb.append("\"" + Paths.get("..", videoFileName).toString() + "\"");
              sb.append(" -c:v libx265 -x265-params log-level=error -c:a aac ");
              sb.append("\"" + tempOutputName + "\"");
              cmdWriter.write(sb.toString());
              cmdWriter.newLine();
              ffmetadataWriter.newLine();
              ffmetadataWriter.newLine();
              ffmetadataWriter.write("[CHAPTER]");
              ffmetadataWriter.newLine();
              ffmetadataWriter.write("TIMEBASE=1/1000");
              ffmetadataWriter.newLine();
              ffmetadataWriter.write("START=" + sum);
              sum += convertDurationToMillis(end) - convertDurationToMillis(start);
              ffmetadataWriter.newLine();
              ffmetadataWriter.write("END=" + (sum - 1));
              ffmetadataWriter.newLine();
              ffmetadataWriter.write("TITLE=" + matches.get(j));
            }
          }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ffmpeg -y -hide_banner -loglevel warning -stats -f concat -safe 0 -i ");
        sb.append("\"" + txtPath.getFileName().toString() + "\"");
        sb.append(" -i ");
        sb.append("\"" + ffmetadataPath.getFileName().toString() + "\"");
        sb.append(" -map 0 -map_metadata 1 -c copy ");
        sb.append("\"" + mltBaseName + ".mkv\"");
        cmdWriter.write(sb.toString());
        cmdWriter.close();

        // create executable .sh file to be used in linux
        List<String> cmdlines = Files.readAllLines(cmdPath, StandardCharsets.UTF_8);
        cmdlines.add(0, "#!/bin/bash");
        Path shPath = Paths.get(parent, mltBaseName + ".sh");
        Files.write(shPath, cmdlines, StandardCharsets.UTF_8);
        shPath.toFile().setExecutable(true, false);

        System.out.println("generateFiles completed!");
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (SAXException e) {
        e.printStackTrace();
      }
    }
  }
}
