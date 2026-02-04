package com.ahthek;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MoveUpDownFinal extends Application {

    @Override
    public void start(Stage primaryStage) {
        ObservableList<String> items = FXCollections.observableArrayList(
                "Item 1", "Item 2", "Item 3", "Item 4"
        );

        ListView<String> listView = new ListView<>(items);
        listView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Move Up button
        Button moveUpButton = new Button("Move Selection Up");
        moveUpButton.setOnAction(e -> {
            ObservableList<Integer> selectedIndices = listView.getSelectionModel().getSelectedIndices();
            if (selectedIndices.isEmpty()) return;

            ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
            FXCollections.sort(indicesCopy); // ascending

            // If any selected item is at the top, do nothing
            if (indicesCopy.get(0) == 0) return;

            for (int index : indicesCopy) {
                String current = items.get(index);
                items.set(index, items.get(index - 1));
                items.set(index - 1, current);

                listView.getSelectionModel().clearSelection(index);
                listView.getSelectionModel().select(index - 1);
            }
        });

        // Move Down button
        Button moveDownButton = new Button("Move Selection Down");
        moveDownButton.setOnAction(e -> {
            ObservableList<Integer> selectedIndices = listView.getSelectionModel().getSelectedIndices();
            if (selectedIndices.isEmpty()) return;

            ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
            FXCollections.sort(indicesCopy, (a, b) -> b - a); // descending

            // If any selected item is at the bottom, do nothing
            if (indicesCopy.get(0) == items.size() - 1) return;

            for (int index : indicesCopy) {
                String current = items.get(index);
                items.set(index, items.get(index + 1));
                items.set(index + 1, current);

                listView.getSelectionModel().clearSelection(index);
                listView.getSelectionModel().select(index + 1);
            }
        });

        HBox buttonBox = new HBox(10, moveUpButton, moveDownButton);
        VBox root = new VBox(10, listView, buttonBox);

        Scene scene = new Scene(root, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ListView Reorder Example");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
