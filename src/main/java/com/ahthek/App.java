package com.ahthek;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/* import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame; */

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {

        /* String videoFilePath = "C:\\Users\\te\\Desktop\\leaf folder\\1080x1920-5s-モンスター 괴물 怪物 ☀️.mp4"; // Replace with your video file path

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFilePath)) {
            grabber.start();

            Frame frame;
            while ((frame = grabber.grab()) != null) {
                // Check if it's a video frame (audio frames also have timestamps)
                if (frame.image != null) {
                    // Get the timestamp in microseconds (default unit for FrameGrabber)
                    long timestamp = grabber.getTimestamp();

                    // Convert to seconds (or other units as needed)
                    double timestampInSeconds = timestamp / 1_000_000.0;

                    System.out.println("Frame grabbed at timestamp (us): " + timestamp);
                    System.out.println("Frame grabbed at timestamp (s): " + timestampInSeconds);
                }
            }

            grabber.stop();
            grabber.close();
        } catch (Exception e) {
            e.printStackTrace();
        } */

        launch();
    }

}