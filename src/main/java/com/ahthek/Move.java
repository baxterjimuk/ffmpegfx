package com.ahthek;

import java.io.IOException;

import javafx.fxml.FXML;

public class Move {
  
  @FXML
  private void generateBatFile() throws IOException {



    System.out.println("generateBatFile for moving completed!");
  }

  @FXML
  private void switchToBat() throws IOException {
    MainApp.setRoot("batCombine");
  }

  @FXML
  private void switchToUfc() throws IOException {
    MainApp.setRoot("ufc");
  }

  @FXML
  private void swtichToMain() throws IOException {
    MainApp.setRoot("main");
  }
}
