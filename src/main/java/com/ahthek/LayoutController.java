package com.ahthek;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LayoutController extends Application {
  @FXML
  private BorderPane borderPane;

  @FXML
  private AnchorPane batCombineAP, mainAP, moveAP, ufcAP;

  @FXML
  private MenuItem edlToBat, batCombine, move, ufc;

  @FXML
  private void edlToBat() throws IOException {
    Parent newCenter = FXMLLoader.load(getClass().getResource("main.fxml"));
    borderPane.setCenter(newCenter);
    BorderPane.setAlignment(newCenter, Pos.TOP_LEFT);
    edlToBat.setDisable(true);
    batCombine.setDisable(false);
    move.setDisable(false);
    ufc.setDisable(false);
  }

  @FXML
  private void batCombine() throws IOException {
    Parent newCenter = FXMLLoader.load(getClass().getResource("batCombine.fxml"));
    borderPane.setCenter(newCenter);
    BorderPane.setAlignment(newCenter, Pos.TOP_LEFT);
    edlToBat.setDisable(false);
    batCombine.setDisable(true);
    move.setDisable(false);
    ufc.setDisable(false);
  }

  @FXML
  private void ufc() throws IOException {
    Parent newCenter = FXMLLoader.load(getClass().getResource("ufc.fxml"));
    borderPane.setCenter(newCenter);
    BorderPane.setAlignment(newCenter, Pos.TOP_LEFT);
    edlToBat.setDisable(false);
    batCombine.setDisable(false);
    move.setDisable(false);
    ufc.setDisable(true);
  }

  @FXML
  private void move() throws IOException {
    Parent newCenter = FXMLLoader.load(getClass().getResource("move.fxml"));
    borderPane.setCenter(newCenter);
    BorderPane.setAlignment(newCenter, Pos.TOP_LEFT);
    edlToBat.setDisable(false);
    batCombine.setDisable(false);
    move.setDisable(true);
    ufc.setDisable(false);
  }

  private static Scene scene;

  @Override
  public void start(Stage stage) throws IOException {

    scene = new Scene(loadFXML("layout"), 640, 640);
    // scene.getStylesheets().add("style.css");
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
    FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource(fxml + ".fxml"));
    return fxmlLoader.load();
  }

  public static void main(String args[]) {
    launch();
  }
}
