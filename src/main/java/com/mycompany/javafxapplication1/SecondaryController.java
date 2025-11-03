package com.mycompany.javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SecondaryController {

    @FXML private TableView<FileData> dataTableView;
    
    @FXML private TableColumn<FileData, String> fileNameColumn;
    @FXML private TableColumn<FileData, String> ownerColumn;
    @FXML private TableColumn<FileData, Long> fileSizeColumn;
    @FXML private TableColumn<FileData, String> filePathColumn;
    @FXML private TableColumn<FileData, String> uploadDateColumn;
    @FXML private Button secondaryButton;
    @FXML private Button refreshBtn;
    @FXML private Button uploadFileBtn;
    @FXML private Button downloadFileBtn;
    @FXML private Button deleteFileBtn;
    @FXML private Button shareFileBtn;

    private final MySQLDB mysqlDatabase = new MySQLDB();
    private final DB sqliteDatabase = new DB();

    public void initialise(String username) {
        if (username != null && !username.isEmpty()) {
            sqliteDatabase.saveSession(username);
        }
        refreshFileList();
    }

    @FXML
    public void initialize() {
        if (fileNameColumn == null || ownerColumn == null || fileSizeColumn == null || filePathColumn == null || uploadDateColumn == null) {
            System.err.println("FXML components are not loaded properly. Check fx:id values in secondary.fxml.");
            return;
        }
        fileNameColumn.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        ownerColumn.setCellValueFactory(cellData -> cellData.getValue().ownerProperty());
        fileSizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty().asObject());
        filePathColumn.setCellValueFactory(cellData -> cellData.getValue().filePathProperty());
        uploadDateColumn.setCellValueFactory(cellData -> cellData.getValue().uploadDateProperty());
        refreshFileList();
    }

    @FXML
    private void switchToPrimary() {
        try {
            Stage stage = (Stage) secondaryButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Primary View");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to switch to Primary View.");
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshFileList() {
        try {
            ObservableList<FileData> fileList = FXCollections.observableArrayList();
            fileList.addAll(mysqlDatabase.getAllFiles());
            fileList.addAll(sqliteDatabase.getAllFiles());
            dataTableView.setItems(fileList);
            System.out.println("File list refreshed: " + fileList.size() + " files loaded.");
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh file list.");
            e.printStackTrace();
        }
    }
    @FXML
private void handleDownloadFile() {
    FileData selectedFile = dataTableView.getSelectionModel().getSelectedItem();
    if (selectedFile == null) {
        showAlert("Download Error", "Please select a file to download.");
        return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save File");
    fileChooser.setInitialFileName(selectedFile.getFileName());
    File saveFile = fileChooser.showSaveDialog(null);

    if (saveFile != null) {
        boolean success = sqliteDatabase.retrieveFile(selectedFile.getFileName(), saveFile);
        if (success) {
            showAlert("Success", "File downloaded successfully.");
        } else {
            showAlert("Download Error", "Failed to download file.");
        }
    }
}


@FXML
private void handleShareFile() {
    FileData selectedFile = dataTableView.getSelectionModel().getSelectedItem();
    if (selectedFile == null) {
        showAlert("Share Error", "Please select a file to share.");
        return;
    }

    String recipientUsername = showInputDialog("Enter the username to share with:");
    if (recipientUsername != null && !recipientUsername.trim().isEmpty()) {
        boolean success = sqliteDatabase.updateFilePermission(selectedFile.getFileName(), "public");
        if (success) {
            showAlert("Success", "File shared successfully.");
            refreshFileList();
        } else {
            showAlert("Share Error", "Failed to share file.");
        }
    }
}

@FXML
private void handleUpdatePermissions() {
    if (!SessionManager.isAdmin()) {
        showAlert("Access Denied", "Only admins can update file permissions.");
        return;
    }

    FileData selectedFile = dataTableView.getSelectionModel().getSelectedItem();
    if (selectedFile == null) {
        showAlert("Permission Error", "Please select a file to update permissions.");
        return;
    }

    String newPermission = showInputDialog("Enter new permission (public/private):");
    if (newPermission != null && (newPermission.equalsIgnoreCase("public") || newPermission.equalsIgnoreCase("private"))) {
        boolean success = sqliteDatabase.updateFilePermission(selectedFile.getFileName(), newPermission);
        if (success) {
            showAlert("Success", "File permissions updated successfully.");
            refreshFileList();
        } else {
            showAlert("Error", "Failed to update file permissions.");
        }
    } else {
        showAlert("Invalid Input", "Please enter either 'public' or 'private'.");
    }
}




   @FXML
private void handleUploadFile() {
    try {
        String currentUser = SessionManager.getCurrentUser();
        int userId = SessionManager.getCurrentUserId();

        if (currentUser == null || currentUser.isEmpty() || userId == -1) {
            showAlert("Upload Error", "User not logged in. Please log in first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            System.out.println("Attempting to upload file: " + selectedFile.getAbsolutePath());
            boolean successSQLite = sqliteDatabase.addFile(userId, selectedFile.getName(), selectedFile.getAbsolutePath(), selectedFile.length());

            if (successSQLite) {
                showAlert("Success", "File uploaded successfully.");
                refreshFileList();
            } else {
                showAlert("Upload Error", "Failed to upload file.");
            }
        }
    } catch (Exception e) {
        showAlert("Error", "Unexpected error during file upload.");
        e.printStackTrace();
    }
}




    @FXML
private void handleDeleteFile() {
    try {
        FileData selectedFile = dataTableView.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showAlert("Delete Error", "Please select a file to delete.");
            return;
        }

        String currentUser = SessionManager.getCurrentUser();
        boolean isOwner = selectedFile.getOwner().equals(currentUser);
        boolean isAdmin = SessionManager.isAdmin();

        if (!isOwner && !isAdmin) {
            showAlert("Access Denied", "You can only delete your own files.");
            return;
        }

        boolean deletedFromMySQL = mysqlDatabase.deleteFile(currentUser, selectedFile.getFileName());
        boolean deletedFromSQLite = sqliteDatabase.deleteFile(currentUser, selectedFile.getFileName());

        if (deletedFromMySQL && deletedFromSQLite) {
            new File(selectedFile.getFilePath()).delete();
            showAlert("Success", "File deleted successfully.");
            refreshFileList();
        } else {
            showAlert("Delete Error", "Failed to delete file.");
        }
    } catch (Exception e) {
        showAlert("Error", "Unexpected error during file deletion.");
        e.printStackTrace();
    }
}


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String showInputDialog(String prompt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Input");
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        return dialog.showAndWait().orElse(null);
    }

    private int fetchUserId(String currentUser) {
    String sql = "SELECT id FROM users WHERE username = ?";
    try (Connection conn = sqliteDatabase.connect();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, currentUser);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
    } catch (SQLException e) {
        System.err.println("Error fetching user ID: " + e.getMessage());
    }
    return -1;
}

}
