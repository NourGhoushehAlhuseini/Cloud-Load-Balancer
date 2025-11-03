package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class FileManager {

    private static final String FILE_DIRECTORY = "/home/ntu-user/file_storage/";
    private final DB database = new DB();

    public boolean uploadFile(int userId, File file) {
        String fileName = file.getName();
        String filePath = FILE_DIRECTORY + fileName;
        long fileSize = file.length();

        try (Connection conn = database.connect()) {
            Files.copy(file.toPath(), Paths.get(filePath));
            String sql = "INSERT INTO files (user_id, filename, file_path, size) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setString(2, fileName);
            pstmt.setString(3, filePath);
            pstmt.setLong(4, fileSize);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | IOException e) {
            System.err.println("File upload failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteFile(int fileId) {
        try (Connection conn = database.connect()) {
            String getFileQuery = "SELECT file_path FROM files WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(getFileQuery);
            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String filePath = rs.getString("file_path");
                Files.deleteIfExists(Paths.get(filePath));
                String deleteQuery = "DELETE FROM files WHERE id = ?";
                PreparedStatement delStmt = conn.prepareStatement(deleteQuery);
                delStmt.setInt(1, fileId);
                return delStmt.executeUpdate() > 0;
            }
        } catch (SQLException | IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }
        return false;
    }

    public File downloadFile(int fileId) {
        try (Connection conn = database.connect()) {
            String sql = "SELECT file_path FROM files WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new File(rs.getString("file_path"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving file: " + e.getMessage());
        }
        return null;
    }

    public boolean updateFile(int fileId, File newFile) {
        try (Connection conn = database.connect()) {
            String sql = "SELECT file_path FROM files WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String oldFilePath = rs.getString("file_path");
                Files.deleteIfExists(Paths.get(oldFilePath));
                Files.copy(newFile.toPath(), Paths.get(oldFilePath));
                String updateQuery = "UPDATE files SET size = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setLong(1, newFile.length());
                updateStmt.setInt(2, fileId);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException | IOException e) {
            System.err.println("Error updating file: " + e.getMessage());
        }
        return false;
    }

    public static void showAlert(String title, String header, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
