package com.ahthek;

import java.io.IOException;
import java.util.Arrays;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LayoutController extends Application {
  private boolean isDarkMode = true;
  @FXML
  private BorderPane borderPane;

  @FXML
  private MenuItem edlToBat, batCombine, move, ufc, chapter, simple;

  @FXML
  private ToggleButton toggle;

  @FXML
  private FontIcon cssIcon;

  private void setCenter(String fxml, MenuItem menuItem) throws IOException {
    Parent newCenter = FXMLLoader.load(getClass().getResource(fxml + ".fxml"));
    borderPane.setCenter(newCenter);
    BorderPane.setAlignment(newCenter, Pos.TOP_LEFT);
    Arrays.asList(edlToBat, batCombine, move, ufc, chapter, simple)
    .forEach(item -> item.setDisable(item.equals(menuItem)));
  }

  @FXML
  private void chapter() throws IOException {
    setCenter("chapter", chapter);
  }

  @FXML
  private void edlToBat() throws IOException {
    setCenter("main", edlToBat);
  }

  @FXML
  private void batCombine() throws IOException {
    setCenter("batCombine", batCombine);
  }

  @FXML
  private void ufc() throws IOException {
    setCenter("ufc", ufc);
  }

  @FXML
  private void move() throws IOException {
    setCenter("move", move);
  }

  @FXML
  private void simple() throws IOException {
    setCenter("simple", simple);
  }

  @FXML
  private void darkMode(ActionEvent event) throws IOException {
    Node sourceNode = (Node) event.getSource();
    Scene scene = sourceNode.getScene();
    if (isDarkMode) {
        cssIcon.setIconLiteral("fas-moon");
        scene.getStylesheets().remove("style.css");
      } else {
        cssIcon.setIconLiteral("fas-sun");
        scene.getStylesheets().add("style.css");
      }
      isDarkMode = !isDarkMode;
  }

  private static Scene scene;

  @Override
  public void start(Stage stage) throws IOException {
    scene = new Scene(loadFXML("layout"), 640, 640);
    scene.getStylesheets().add("style.css");
    
    stage.setScene(scene);
    stage.setResizable(false);
    stage.setTitle("FFMPEGFX");
    stage.getIcons().add(new Image("clapfx.png"));

    stage.show();
  }

  static void setRoot(String fxml) throws IOException {
    scene.setRoot(loadFXML(fxml));
  }

  private static Parent loadFXML(String fxml) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(LayoutController.class.getResource(fxml + ".fxml"));
    return fxmlLoader.load();
  }

  public static void main(String args[]) {
    launch();
  }
}
