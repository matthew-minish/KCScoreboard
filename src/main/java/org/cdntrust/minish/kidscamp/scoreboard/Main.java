package org.cdntrust.minish.kidscamp.scoreboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    // Scoreboard controller
    Controller controller;

    @Override
    public void start(Stage primaryStage) throws Exception{
        controller = new Controller();
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/scoreboard.fxml")));
        root.getStylesheets().addAll(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
        primaryStage.setTitle("KC Scoreboard");
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
