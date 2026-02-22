package com.ahthek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class UfcController {
  Preferences preferences = Preferences.userNodeForPackage(UfcController.class);

  @FXML
  private TextField mltFileTextField, txtFileTextField, videoFileTextField;

  @FXML
  private Button startButton, combineButton, removeButton, clearButton, moveUpButton, moveDownButton;

  @FXML
  private Label estimateLabel;

  private ObservableList<String> batPaths = FXCollections.observableArrayList();
  ExtensionFilter allFilter = new ExtensionFilter("All files", "*.*");
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  @FXML
  private ListView<String> batFileListView;

  @FXML
  private void combine() throws IOException {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    alert.setContentText("This will generate the following files:\n"
      + "1. A .bat file containing the combined ffmpeg commands\n"
      + "2. A .txt file containing the combined list of temporary output to be joined\n"
      + "3. A .ffmetadata chapter file that is combined\n\n"
      + "Are you sure you want to continue?"
    );
    alert.setHeaderText("Combine files");
    alert.getDialogPane().setPrefSize(500, 500);

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      // assuming they're sorted well
      String combineName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_combine.";
      Path parent = Paths.get(batPaths.get(0)).getParent();
      Path combinePath = Paths.get(parent.toString(), combineName + "bat");
      Path bigFfmetadata = Paths.get(parent.toString(), combineName + "ffmetadata");
      Path txtPath = Paths.get(parent.toString(), combineName + "txt");
      Files.deleteIfExists(combinePath);
      Files.deleteIfExists(bigFfmetadata);
      Files.deleteIfExists(txtPath);
      try (
        BufferedWriter cmdWriter = new BufferedWriter(new FileWriter(combinePath.toString(), true));
        BufferedWriter ffmetadataWriter = new BufferedWriter(new FileWriter(bigFfmetadata.toString(), true));
        // BufferedWriter txtWriter = new BufferedWriter(new FileWriter(txtPath.toString(), true));
      ) {
        long totalMillis = 0;
        List<String> fileList = new ArrayList<>();
        ffmetadataWriter.write(";FFMETADATA1");
        for (String batPath: batPaths) {
          // the last line contains the ffmetadata file
          // need to read that and extract every match into a list
          List<String> cmdList = Files.readAllLines(Paths.get(batPath));

          Path ffmetadataPath = Paths.get(parent.toString(), quotedStrings(cmdList.getLast()).get(1));
          // System.out.println(ffmetadataPath.toString());
          List<String> ffmetadataList = Files.readAllLines(ffmetadataPath);
          List<String> matchList = new ArrayList<>();
          for (String ffmetadata: ffmetadataList) {
            if (ffmetadata.startsWith("TITLE=")) {
              matchList.add(ffmetadata.substring("TITLE=".length()));
            }
          }

          for (int i = 0 ; i < cmdList.size() - 1; i++) {
            // copy this command to a new file
            cmdWriter.write(cmdList.get(i));
            cmdWriter.newLine();
            List<String> startEnd = timeStrings(cmdList.get(i));
            List<String> inOut = quotedStrings(cmdList.get(i));
            fileList.add("file '" + inOut.get(1) + "'");
            // txtWriter.write("file '" + inOut.get(1) + "'");
            // txtWriter.newLine();
            Duration duration = Duration.between(
              LocalTime.parse(startEnd.get(0), formatter), 
              LocalTime.parse(startEnd.get(1), formatter)
            );
            ffmetadataWriter.newLine();
            ffmetadataWriter.newLine();
            ffmetadataWriter.write("[CHAPTER]");
            ffmetadataWriter.newLine();
            ffmetadataWriter.write("TIMEBASE=1/1000");
            ffmetadataWriter.newLine();
            ffmetadataWriter.write("START=" + String.valueOf(totalMillis));
            ffmetadataWriter.newLine();
            totalMillis += duration.toMillis();
            ffmetadataWriter.write("END=" + String.valueOf(totalMillis - 1));
            ffmetadataWriter.newLine();
            ffmetadataWriter.write("TITLE=" + matchList.get(i));
          }
        }
        cmdWriter.write("ffmpeg -y -hide_banner -loglevel warning -stats -f concat -safe 0 -i "
          + "\"" + txtPath.getFileName().toString() + "\" -i "
          + "\"" + bigFfmetadata.getFileName().toString() + "\" -map 0 -map_metadata 1 -c copy "
          + "\"" + combineName + "mkv\""
        );
        cmdWriter.close();
        ffmetadataWriter.close();

        Files.write(txtPath, fileList);

        // create executable .sh file to be used in linux
        List<String> cmdlines = Files.readAllLines(combinePath, StandardCharsets.UTF_8);
        cmdlines.add(0, "#!/bin/bash");
        Path shPath = Paths.get(parent.toString(), combineName + "sh");
        Files.write(shPath, cmdlines, StandardCharsets.UTF_8);
        shPath.toFile().setExecutable(true, false);

        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append(combinePath.toString() 
          + "\n" + shPath.toString()
          + "\n" + txtPath.toString()
          + "\n" + bigFfmetadata.toString()
        );
        System.out.println("combine completed!" + "\n" + sb.toString());
        alert.setAlertType(AlertType.INFORMATION);
        alert.setHeaderText("Process completed!");
        alert.setContentText("The following files have been generated:\n" + sb.toString());
        alert.showAndWait();
      }
    }
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
    estimateLabel.setText(null);
    selectFile(event, "Select .mlt file", List.of(allFilter, mltFilter), mltFilter, mltFileTextField);
    
    // create a new label in fxml to show the estimated duration and calculate the duration here
    // error possibility: not an mlt file, mlt file doesn't contain the necessary to calculate the sum
    if (!mltFileTextField.getText().isBlank()) {
      try{
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
              Element entryElement = (Element) entryNodeList.item(j);
              String start = entryElement.getAttribute("in");
              String end = entryElement.getAttribute("out");
              sum += convertDurationToMillis(end) - convertDurationToMillis(start);
            }
            Duration totalDuration = Duration.ofMillis(sum);
            estimateLabel.setText(String.valueOf(totalDuration.toDaysPart()) + " days "
            + String.valueOf(totalDuration.toHoursPart()) + " hours "
            + String.valueOf(totalDuration.toMinutesPart()) + " minutes "
            + String.valueOf(totalDuration.toSecondsPart()) + " seconds");
          }
        }
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (SAXException e) {
        e.printStackTrace();
      }
    }
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
    batFileListView.setItems(batPaths);
    batFileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    combineButton.disableProperty().bind(Bindings.size(batPaths).lessThan(2));
    clearButton.disableProperty().bind(Bindings.isEmpty(batPaths));
    removeButton.disableProperty().bind(Bindings.isEmpty(batFileListView.getSelectionModel().getSelectedItems()));
    moveUpButton.disableProperty().bind(Bindings.size(batPaths).lessThan(2)
    .or(Bindings.isEmpty(batFileListView.getSelectionModel().getSelectedItems())));
    moveDownButton.disableProperty().bind(Bindings.size(batPaths).lessThan(2)
    .or(Bindings.isEmpty(batFileListView.getSelectionModel().getSelectedItems())));
  }

  @FXML
  private void moveUp() {
    ObservableList<Integer> selectedIndices = batFileListView.getSelectionModel().getSelectedIndices();
    if (selectedIndices.isEmpty()) return;

    ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
    FXCollections.sort(indicesCopy); // ascending

    // If any selected item is at the top, do nothing
    if (indicesCopy.get(0) == 0) return;

    for (int index : indicesCopy) {
        String current = batPaths.get(index);
        batPaths.set(index, batPaths.get(index - 1));
        batPaths.set(index - 1, current);

        batFileListView.getSelectionModel().clearSelection(index);
        batFileListView.getSelectionModel().select(index - 1);
    }
  }

  @FXML
  private void moveDown() {
    ObservableList<Integer> selectedIndices = batFileListView.getSelectionModel().getSelectedIndices();
    if (selectedIndices.isEmpty()) return;

    ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
    FXCollections.sort(indicesCopy, (a, b) -> b - a); // descending

    // If any selected item is at the bottom, do nothing
    if (indicesCopy.get(0) == batPaths.size() - 1) return;

    for (int index : indicesCopy) {
        String current = batPaths.get(index);
        batPaths.set(index, batPaths.get(index + 1));
        batPaths.set(index + 1, current);

        batFileListView.getSelectionModel().clearSelection(index);
        batFileListView.getSelectionModel().select(index + 1);
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
    ExtensionFilter batFilter = new ExtensionFilter("Batch files", "*.bat");
    fileChooser.getExtensionFilters().addAll(allFilter, batFilter);
    fileChooser.setSelectedExtensionFilter(batFilter);
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
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    alert.setContentText("This will generate the following files:\n"
      + "1. A .bat file containing ffmpeg commands\n"
      + "2. A .txt file containing the list of temporary output to be joined\n"
      + "3. A .ffmetadata chapter file\n\n"
      + "Are you sure you want to continue?"
    );
    alert.setHeaderText("Generate files");

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
        StringBuilder sb = new StringBuilder();
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
              sb.setLength(0);
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
        sb.setLength(0);
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

        sb.setLength(0);
        sb.append(cmdPath.toString() 
          + "\n" + shPath.toString()
          + "\n" + txtPath.toString()
          + "\n" + ffmetadataPath.toString()
        );
        System.out.println("generateFiles completed!" + "\n" + sb.toString());
        alert.setAlertType(AlertType.INFORMATION);
        alert.setHeaderText("Process completed!");
        alert.setContentText("The following files have been generated:\n" + sb.toString());
        alert.showAndWait();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (SAXException e) {
        e.printStackTrace();
      }
    }
  }
}
// last thing done is involving stringbuilder