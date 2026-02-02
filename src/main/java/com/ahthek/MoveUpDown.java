package com.ahthek;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MoveUpDown extends Application {

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
            ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
            FXCollections.sort(indicesCopy); // ascending

            for (int index : indicesCopy) {
                if (index > 0) {
                    String current = items.get(index);
                    items.set(index, items.get(index - 1));
                    items.set(index - 1, current);

                    listView.getSelectionModel().clearSelection(index);
                    listView.getSelectionModel().select(index - 1);
                }
            }
        });

        /* // Disable if nothing selected OR any selected item is at the top
        moveUpButton.disableProperty().bind(
            listView.getSelectionModel().selectedIndices.emptyProperty()
                .or(listView.getSelectionModel().selectedIndices
                    .filtered(i -> i == 0).sizeProperty().greaterThan(0))
        ); */

        // Move Down button
        Button moveDownButton = new Button("Move Selection Down");
        moveDownButton.setOnAction(e -> {
            ObservableList<Integer> selectedIndices = listView.getSelectionModel().getSelectedIndices();
            ObservableList<Integer> indicesCopy = FXCollections.observableArrayList(selectedIndices);
            FXCollections.sort(indicesCopy, (a, b) -> b - a); // descending

            for (int index : indicesCopy) {
                if (index < items.size() - 1) {
                    String current = items.get(index);
                    items.set(index, items.get(index + 1));
                    items.set(index + 1, current);

                    listView.getSelectionModel().clearSelection(index);
                    listView.getSelectionModel().select(index + 1);
                }
            }
        });

        /* // Disable if nothing selected OR any selected item is at the bottom
        moveDownButton.disableProperty().bind(
            listView.getSelectionModel().selectedIndices.emptyProperty()
                .or(listView.getSelectionModel().selectedIndices
                    .filtered(i -> i == listView.getItems().size() - 1).sizeProperty().greaterThan(0))
        ); */

        // disable moveUp if the top most item currently in the listview is part of the selection
        // disable moveDown if the bottom most item currently in the listview is part of the selection
        listView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<String>) c -> {
            ObservableList<String> selected = listView.getSelectionModel().getSelectedItems();
            Boolean firstSelected = selected.contains(items.get(0));
            Boolean lastSelected = selected.contains(items.get(items.size() - 1));
            moveUpButton.setDisable(selected.isEmpty() || firstSelected);
            moveDownButton.setDisable(selected.isEmpty() || lastSelected);
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
