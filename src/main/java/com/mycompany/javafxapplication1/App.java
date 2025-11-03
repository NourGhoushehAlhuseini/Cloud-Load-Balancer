package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
@Override
public void start(Stage stage) {
    System.out.println("Starting JavaFX Application...");
    
    try {
        DB myObj = new DB();
        myObj.createTables();
        System.out.println("Database initialized successfully.");

        System.out.println("Loading FXML...");
        System.out.println("FXML file path: " + getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
        Parent root = loader.load();
        System.out.println("FXML Loaded Successfully.");

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Primary View - JavaFX");
        stage.show();
        stage.toFront();
        System.out.println("GUI Loaded Successfully.");
    } catch (IOException e) {
        System.err.println("Error loading JavaFX Application: " + e.getMessage());
        e.printStackTrace();
    } catch (Exception e) {
        Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Unexpected Error Occurred!", e);
    }
}
}