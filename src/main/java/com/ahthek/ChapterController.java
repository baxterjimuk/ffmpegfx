package com.ahthek;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FilenameUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ChapterController {
  Preferences preferences = Preferences.userNodeForPackage(UfcController.class);
  ExtensionFilter allFilter = new ExtensionFilter("All files", "*.*");
  ExtensionFilter mltFilter = new ExtensionFilter("MLT files", "*.mlt");
  ExtensionFilter edlFilter = new ExtensionFilter("EDL files", "*.edl");
  ExtensionFilter txtFilter = new ExtensionFilter("Text files", "*.txt");
  ExtensionFilter ffFilter = new ExtensionFilter("FFmpeg metadata files", "*.ffmetadata");
  ObservableList<Chapter> list = FXCollections.observableArrayList();

  @FXML
  private TextField timeTextField, matchTextField;

  @FXML
  private Button loadButton, addNewButton, deleteButton, clearButton;
  
  @FXML
  private Button generateChapterButton, moveUpButton, moveDownButton;

  @FXML
  private Button copyStartButton, copyEndButton, resetAllTo0Button;

  @FXML
  private CheckBox startAt0CheckBox;

  @FXML
  private VBox timestampVBox;

  @FXML
  private Spinner<Integer> hSpinner, mSpinner, sSpinner, msSpinner;

  @FXML
  private TableView<Chapter> table;

  @FXML
  private TableColumn<Chapter, String> startCol, endCol, titleCol;
        
  public void initialize() {
    BooleanBinding blankTextFields = timeTextField.textProperty().isEmpty()
    .or(matchTextField.textProperty().isEmpty());
    loadButton.disableProperty().bind(blankTextFields);
    startCol.setCellValueFactory(new PropertyValueFactory<>("start"));
    startCol.setCellFactory(TextFieldTableCell.forTableColumn());
    startCol.setOnEditCommit(e -> e.getRowValue().setStart(e.getNewValue()));
    endCol.setCellValueFactory(new PropertyValueFactory<>("end"));
    endCol.setCellFactory(TextFieldTableCell.forTableColumn());
    endCol.setOnEditCommit(e -> e.getRowValue().setEnd(e.getNewValue()));
    titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
    titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
    titleCol.setOnEditCommit(e -> e.getRowValue().setTitle(e.getNewValue()));
    addNewButton.setOnAction(e -> list.add(new Chapter("HH:mm:ss.SSS", "HH:mm:ss.SSS", "who vs. who")));
    deleteButton.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedItems()));
    deleteButton.setOnAction(e -> {
      list.remove(table.getSelectionModel().getSelectedIndex());
      table.getSelectionModel().clearSelection();
    });
    clearButton.disableProperty().bind(Bindings.isEmpty(list));
    clearButton.setOnAction(e -> list.clear());
    generateChapterButton.disableProperty().bind(Bindings.isEmpty(list));
    generateChapterButton.setOnAction(e -> generateChapterFile(e));
    table.setItems(list);
    table.setPlaceholder(new Label("Please Load some data or Add New"));
    moveUpButton.disableProperty().bind(Bindings.size(list).lessThan(2)
    .or(Bindings.isEmpty(table.getSelectionModel().getSelectedItems())));
    moveDownButton.disableProperty().bind(Bindings.size(list).lessThan(2)
    .or(Bindings.isEmpty(table.getSelectionModel().getSelectedItems())));
    timestampVBox.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedItems()));
    setSpinner(hSpinner, "%02d", 23);
    setSpinner(mSpinner, "%02d", 59);
    setSpinner(sSpinner, "%02d", 59);
    setSpinner(msSpinner, "%03d", 999);
  }

  public void setSpinner(Spinner<Integer> spinner, String format, int maxValue) {
    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxValue));
    spinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
    StringConverter<Integer> converter = new StringConverter<>() {
      @Override
      public String toString(Integer n) {
        return String.format(format, n);
      }

      @Override
      public Integer fromString(String s) {
        try {
          return Integer.parseInt(s);
        } catch (NumberFormatException e) {
          return 0; // Fallback value
        }
      }
    };
    spinner.getEditor().setTextFormatter(new TextFormatter<>(converter, 0));
    spinner.setOnScroll(event -> {
      if (event.getDeltaY() > 0) {
        spinner.increment();
      } else if (event.getDeltaY() < 0) {
        spinner.decrement();
      }
    });
    spinner.focusedProperty().addListener((obs, ov, nv) -> {
      javafx.application.Platform.runLater(() -> {
        if (spinner.getEditor().getText().length() > 0) {
          spinner.getEditor().selectAll();
        }
      });
    });
  }

  @FXML
  private void copyStart() {
    list.get(table.getSelectionModel().getSelectedIndex()).setStart(
      hSpinner.getEditor().getText() + ":" + mSpinner.getEditor().getText() + ":"
      + sSpinner.getEditor().getText() + "." +msSpinner.getEditor().getText()
    );
  }

  @FXML
  private void copyEnd() {
    list.get(table.getSelectionModel().getSelectedIndex()).setEnd(
      hSpinner.getEditor().getText() + ":" + mSpinner.getEditor().getText() + ":"
      + sSpinner.getEditor().getText() + "." +msSpinner.getEditor().getText()
    );
  }

  @FXML
  private void resetAllTo0() {
    hSpinner.getValueFactory().setValue(0);
    mSpinner.getValueFactory().setValue(0);
    sSpinner.getValueFactory().setValue(0);
    msSpinner.getValueFactory().setValue(0);
  }

  public static boolean isValidTime(String timeString) {
    if (timeString.isBlank() || timeString == null) {
      return false;
    }
    try {
      LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private void generateChapterFile(ActionEvent event) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setResizable(true);
    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("clapfx.png"));
    alert.setTitle("FFMPEGFX");
    alert.setContentText("""
        Somthing wrong with your data.
        Check the table and make sure any time values are 
        formatted correctly and there are no blank title.
        """);
    alert.setHeaderText("Generate chapter file");
    // check if the observablelist data is valid
    // boolean isOK = true;
    for (Chapter chapter: list) {
      if (!isValidTime(chapter.getStart()) || !isValidTime(chapter.getEnd()) || chapter.getTitle().isBlank()) {
        // isOK = false;
        alert.showAndWait();
        return;
      }
      // if (!isValidTime(chapter.getStart())) return;
      // if (!isValidTime(chapter.getEnd())) return;
      // if (chapter.getTitle().isBlank()) return;
    }
    // System.out.println(isOK ? "it's ok" : "it's not ok");

    alert.setAlertType(AlertType.CONFIRMATION);
    alert.setContentText("Are you sure you want to continue?");
    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Save File");
      fileChooser.setInitialFileName("something.ffmetadata");
      File initialDir = new File(preferences.get("lastUsedDir", System.getProperty("user.home")));
      if (!initialDir.exists()) {
        initialDir = new File(System.getProperty("user.home"));
      }
      fileChooser.setInitialDirectory(initialDir);
      fileChooser.getExtensionFilters().addAll(allFilter, ffFilter);
      fileChooser.setSelectedExtensionFilter(ffFilter);
      // Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      File file = fileChooser.showSaveDialog(stage);
      if (file != null) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
          writer.println(";FFMETADATA1");
          long sum = 0;
          boolean startAt0 = startAt0CheckBox.isSelected();
          for (Chapter chapter: list) {
            long startL = UfcController.convertDurationToMillis(chapter.getStart());
            long endL = UfcController.convertDurationToMillis(chapter.getEnd());
            writer.println();
            writer.println("[CHAPTER]");
            writer.println("TIMEBASE=1/1000");
            writer.println("START=" + String.valueOf(startAt0 ? sum : startL));
            sum += endL - startL;
            writer.println("END=" + String.valueOf(startAt0 ? sum - 1 : endL));
            writer.println("TITLE=" + chapter.getTitle());
          }
          System.out.println(file.getAbsolutePath() + " generated successfully!");
          alert.setHeaderText("Process completed!");
          alert.setContentText("The following file has been generated:\n" + file.getAbsolutePath());
          alert.showAndWait();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    
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

  private boolean isXML(File file) {
    try (FileInputStream inputStream = new FileInputStream(file)) {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true); // Generally good practice

      SAXParser saxParser = factory.newSAXParser();
      XMLReader xmlReader = saxParser.getXMLReader();

      // A simple handler that does nothing, as we only care if parsing completes
      xmlReader.setContentHandler(new DefaultHandler()); 

      xmlReader.parse(new InputSource(inputStream));
      return true;
    } catch (SAXException | ParserConfigurationException | IOException e) {
      // An exception occurred during parsing
      e.printStackTrace();
      return false;
    }
  }

  @FXML
  private void selectTimeFile(ActionEvent event) throws IOException {
    selectFile(
      event, 
      "Select .mlt or .edl file", 
      List.of(allFilter, edlFilter, mltFilter), 
      mltFilter, 
      timeTextField
    );
  }

  @FXML
  private void selectMatchFile(ActionEvent event) throws IOException {
    selectFile(
      event, 
      "Select .txt file containing list of matches", 
      List.of(allFilter, txtFilter), 
      txtFilter, 
      matchTextField
    );
  }

  @FXML
  private void loadTable() throws IOException {
    File timeFile = new File(timeTextField.getText());
    File matchFile = new File(matchTextField.getText());
    List<String> allLines = new ArrayList<>();
    if (Arrays.asList("mlt", "edl").contains(FilenameUtils.getExtension(timeTextField.getText()))) {
      allLines = Files.readAllLines(timeFile.toPath());
    } else {
      return;
    }

    List<List<String>> timeList = new ArrayList<>();
    if (allLines.getFirst().contains("xml")) {
      try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(timeTextField.getText());
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        NodeList playlistNodeList = root.getElementsByTagName("playlist");
        for (int i = 0; i < playlistNodeList.getLength(); i++) {
          if (playlistNodeList.item(i).getAttributes().getNamedItem("id").getNodeValue().equals("playlist0")) {
            NodeList entryNodeList = ((Element) playlistNodeList.item(i)).getElementsByTagName("entry");
            for (int j = 0; j < entryNodeList.getLength(); j++) {
              Element entryElement = (Element) entryNodeList.item(j);
              String[] startEnd = {entryElement.getAttribute("in"), entryElement.getAttribute("out")};
              timeList.add(Arrays.asList(startEnd));
            }
          }
        }
      } catch (ParserConfigurationException | SAXException e) {
        e.printStackTrace();
        return;
      }
    } else {
      allLines.forEach(line -> {
        if (!line.isBlank()) {
          timeList.add(Arrays.asList(line.replace(",", ".").split("\\t")));
        }
      });
    }

    // timeList.forEach(timeLine -> System.out.println(String.join("\t", timeLine)));

    List<String> matches = Files.readAllLines(matchFile.toPath());
    if (matches.size() != timeList.size()) {
      System.out.println("matches=" + matches.size() + ",times=" + timeList.size());
      return;
    }

    list.clear();
    for(int i = 0; i < timeList.size(); i++) {
      // timeList.set(i, Arrays.asList(timeList.get(i).get(0), timeList.get(i).get(1), matches.get(i)));
      list.add(new Chapter(timeList.get(i).get(0), timeList.get(i).get(1), matches.get(i)));
    }

    // list.forEach(item -> System.out.println(item));


  }

  @FXML
  private void moveUp() {
    ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();
    if (selectedIndices.isEmpty()) return;

    ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
    FXCollections.sort(indicesCopy); // ascending

    // If any selected item is at the top, do nothing
    if (indicesCopy.get(0) == 0) return;

    for (int index : indicesCopy) {
        Chapter current = list.get(index);
        list.set(index, list.get(index - 1));
        list.set(index - 1, current);

        table.getSelectionModel().clearSelection(index);
        table.getSelectionModel().select(index - 1);
    }
  }

  @FXML
  private void moveDown() {
    ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();
    if (selectedIndices.isEmpty()) return;

    ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
    FXCollections.sort(indicesCopy, (a, b) -> b - a); // descending

    // If any selected item is at the bottom, do nothing
    if (indicesCopy.get(0) == list.size() - 1) return;

    for (int index : indicesCopy) {
        Chapter current = list.get(index);
        list.set(index, list.get(index + 1));
        list.set(index + 1, current);

        table.getSelectionModel().clearSelection(index);
        table.getSelectionModel().select(index + 1);
    }
  }

  public static class Chapter {
    private final SimpleStringProperty start;
    private final SimpleStringProperty end;
    private final SimpleStringProperty title;

    private Chapter(String startTime, String endTime, String matchTitle) {
      this.start = new SimpleStringProperty(startTime);
      this.end = new SimpleStringProperty(endTime);
      this.title = new SimpleStringProperty(matchTitle);
    }

    public String getStart() {
      return start.get();
    }

    public void setStart(String startTime) {
      start.set(startTime);
    }

    public String getEnd() {
      return end.get();
    }

    public void setEnd(String endTime) {
      end.set(endTime);
    }

    public String getTitle() {
      return title.get();
    }

    public void setTitle(String matchTitle) {
      title.set(matchTitle);
    }
    
    public StringProperty titleProperty() {
      return title;
    }

    public StringProperty startProperty() {
      return start;
    }

    public StringProperty endProperty() {
      return end;
    }
  }
}
