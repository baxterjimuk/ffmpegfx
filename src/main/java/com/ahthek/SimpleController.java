package com.ahthek;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class SimpleController {
  // need to create a class for important ffmpeg property

  @FXML
  private ComboBox<VideoCodec> vcodecComboBox;

  public enum VideoCodec {
    H265("libx265"),
    H264("libx264"),
    COPY("copy"),
    NOVIDEO("vn");

    private final String codec;

    VideoCodec(String thecodec) {
      this.codec = thecodec;
    }

    @Override
    public String toString() {
      return codec;
    }
  }

  @FXML
  public void initialize() {
    vcodecComboBox.getItems().setAll(VideoCodec.values());
    vcodecComboBox.getSelectionModel().select(VideoCodec.COPY);
  }
}
