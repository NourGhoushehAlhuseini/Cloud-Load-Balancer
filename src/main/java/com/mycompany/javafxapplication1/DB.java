package com.mycompany.javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.sql.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.Paths;


public class DB {






    
private static final String DATABASE_URL = "jdbc:sqlite:" + Paths.get("/home/ntu-user/NetBeansProjects/javaaanour1/cwk (1)/cwk/JavaFXApplication1/comp20081.db").toAbsolutePath().toString();

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DATABASE_URL);
            System.out.println("✅ Connected to SQLite database: " + DATABASE_URL);
        } catch (SQLException e) {
            System.out.println("SQLite Connection Error: " + e.getMessage());
        }
        return conn;
    }






    public void createTables() {
    String usersTable = "CREATE TABLE IF NOT EXISTS users ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT UNIQUE NOT NULL, "
            + "password_hash TEXT NOT NULL, "
            + "salt TEXT NOT NULL, "
            + "role TEXT CHECK(role IN ('standard', 'admin')) DEFAULT 'standard', "
            + "locked INTEGER DEFAULT 0)";

    String filesTable = "CREATE TABLE IF NOT EXISTS files ("
            + "file_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "filename TEXT NOT NULL, "
            + "owner TEXT NOT NULL, "
            + "size BIGINT NOT NULL, "
            + "file_path TEXT NOT NULL, "
            + "permissions TEXT DEFAULT 'private', "
            + "upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (owner) REFERENCES users(username) ON DELETE CASCADE)";

    try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(usersTable);
        stmt.executeUpdate(filesTable);
    } catch (SQLException e) {
        System.err.println("Database setup error: " + e.getMessage());
    }
}
public boolean setFilePermission(String fileName, String username, String permission) {
    String sql = "INSERT INTO file_permissions (file_id, user_id, permission) VALUES ((SELECT file_id FROM files WHERE filename = ?), (SELECT id FROM users WHERE username = ?), ?)";
    try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, fileName);
        pstmt.setString(2, username);
        pstmt.setString(3, permission);

        int rowsInserted = pstmt.executeUpdate();
        conn.commit();
        return rowsInserted > 0;
    } catch (SQLException e) {
        System.err.println("Error setting file permission: " + e.getMessage());
    }
    return false;
}
public static void testSQLiteConnection() {
    try (Connection conn = getSQLiteConnection()) {
        if (conn != null) {
            System.out.println("✅ Connected to SQLite database successfully!");
        } else {
            System.out.println("❌ Failed to connect to SQLite database.");
        }
    } catch (SQLException e) {
        System.err.println("Error connecting to SQLite: " + e.getMessage());
    }
}



public boolean retrieveFile(String fileName, File saveFile) {
    String sql = "SELECT file_path FROM files WHERE filename = ?";
    try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, fileName);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            String filePath = rs.getString("file_path");
            File sourceFile = new File(filePath);
            
            if (!sourceFile.exists()) {
                System.err.println("File not found at: " + filePath);
                return false;
            }

            System.out.println("Downloading file from: " + filePath);
            java.nio.file.Files.copy(sourceFile.toPath(), saveFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } else {
            System.err.println("No file found in SQLite with name: " + fileName);
        }
    } catch (Exception e) {
        System.err.println("File retrieval error: " + e.getMessage());
    }
    return false;
}



   public boolean addUser(String username, String password, String role) {
    if (userExists(username)) {
        return false;
    }

    String salt = generateSalt();
    String hashedPassword = hashPassword(password, salt);
    String sql = "INSERT INTO users (username, password_hash, salt, role) VALUES (?, ?, ?, ?)";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, username);
        pstmt.setString(2, hashedPassword);
        pstmt.setString(3, salt);
        pstmt.setString(4, role);
        return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("SQLite user insertion error: " + e.getMessage());
    }
    return false;
}





    public void saveSession(String username) {
        String sessionTable = "CREATE TABLE IF NOT EXISTS session ("
                + "username TEXT PRIMARY KEY, "
                + "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String sql = "INSERT INTO session (username, last_login) VALUES (?, CURRENT_TIMESTAMP) "
                   + "ON CONFLICT(username) DO UPDATE SET last_login = CURRENT_TIMESTAMP";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sessionTable);
        } catch (SQLException e) {
            System.err.println("Session table creation error: " + e.getMessage());
        }

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Session save error: " + e.getMessage());
        }
    }
public boolean validateUser(String username, String password) {
    String sql = "SELECT id, password_hash, salt, role FROM users WHERE username = ?";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, username);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            String storedHash = rs.getString("password_hash");
            String salt = rs.getString("salt");
            String hashedInputPassword = hashPassword(password, salt);
            if (storedHash.equals(hashedInputPassword)) {
                int userId = rs.getInt("id");
                String role = rs.getString("role");

                // Store user session with role
                SessionManager.setCurrentUser(username, userId, role);
                System.out.println("Login successful for user: " + username + " | Role: " + role);
                return true;
            } else {
                System.err.println("Login failed: Incorrect password for user: " + username);
            }
        } else {
            System.err.println("Login failed: User not found: " + username);
        }
    } catch (SQLException e) {
        System.err.println("SQLite validation error: " + e.getMessage());
    }
    return false;
}


    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("User existence check error: " + e.getMessage());
        }
        return false;
    }
  public String fetchUserRole(String username) {
    String sql = "SELECT role FROM users WHERE username = ?";
    try (Connection conn = connect();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getString("role");
        }
    } catch (SQLException e) {
        System.err.println("Error fetching user role: " + e.getMessage());
    }
    return "standard";
}



    public boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("User deletion error: " + e.getMessage());
        }
        return false;
    }

   public boolean updateUserPassword(String username, String newPassword) {
    String salt = generateSalt();
    String hashedPassword = hashPassword(newPassword, salt);
    String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE username = ?";

    try (Connection conn = connect()) {
        conn.setAutoCommit(false);
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, salt);
            pstmt.setString(3, username);
            int rowsUpdated = pstmt.executeUpdate();
            conn.commit();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("SQLite password update error: " + e.getMessage());
            return false;
        } finally {
            conn.setAutoCommit(true);  // Restore auto-commit in all cases
        }
    } catch (SQLException e) {
        System.err.println("SQLite connection error: " + e.getMessage());
    }
    return false;
}




  public boolean addFile(int userId, String fileName, String originalFilePath, long size) {
String saveDirectory = Paths.get("/home/ntu-user/NetBeansProjects/javaaanour/cwk (1)/cwk/files/").toAbsolutePath().toString();
    File targetDirectory = new File(saveDirectory);

    
    if (!targetDirectory.exists()) {
        targetDirectory.mkdirs();
    }

    File targetFile = new File(saveDirectory + fileName);

    
    File originalFile = new File(originalFilePath);
    if (!originalFile.exists()) {
        System.err.println(" Error: Original file does not exist: " + originalFilePath);
        return false;
    }

    try {
        
        System.out.println("Copying file from: " + originalFilePath + " to " + targetFile.getAbsolutePath());
        java.nio.file.Files.copy(originalFile.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        
        String sql = "INSERT INTO Files (user_id, filename, owner, file_path, size, permissions, upload_date) VALUES (?, ?, ?, ?, ?, 'private', CURRENT_TIMESTAMP)";

        try (Connection conn = getSQLiteConnection()) { 
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, fileName);
                pstmt.setString(3, SessionManager.getCurrentUser());
                pstmt.setString(4, targetFile.getAbsolutePath());
                pstmt.setLong(5, size);

                System.out.println("Executing SQLite INSERT for file: " + fileName);
                System.out.println("User ID: " + userId + ", File Path: " + targetFile.getAbsolutePath());

                int rowsInserted = pstmt.executeUpdate();
                conn.commit();

                if (rowsInserted > 0) {
                    System.out.println(" File successfully stored in SQLite: " + targetFile.getAbsolutePath());

                    
                    MySQLDB mySQLDB = new MySQLDB();
                    boolean insertedToMySQL = mySQLDB.insertFileToMySQL(fileName, targetFile.getAbsolutePath(), size);
                    
                    if (!insertedToMySQL) {
                        System.err.println(" Failed to sync file to MySQL.");
                    }
                    return true;
                } else {
                    System.err.println(" SQLite file insert failed for: " + fileName);
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println(" SQLite insert error: " + e.getMessage());
            }
        }
    } catch (Exception e) {
        System.err.println(" File storage error: " + e.getMessage());
    }
    return false;
}
public int getUserIdFromSQLite(String username) {
    String sql = "SELECT id FROM users WHERE username = ?";
    try (Connection conn = getSQLiteConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
    } catch (SQLException e) {
        System.err.println(" Error retrieving user ID from SQLite: " + e.getMessage());
    }
    return -1;
}


public static Connection getSQLiteConnection() throws SQLException {
    return DriverManager.getConnection(DATABASE_URL);
}




  public boolean deleteFile(String username, String fileName) {
    String checkFileSQL = "SELECT user_id, file_path FROM Files WHERE filename = ?";
    String deleteSQL = "DELETE FROM Files WHERE filename = ?";

    try (Connection conn = getSQLiteConnection();
         PreparedStatement checkStmt = conn.prepareStatement(checkFileSQL);
         PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {

        conn.setAutoCommit(false); 

        checkStmt.setString(1, fileName);
        ResultSet rs = checkStmt.executeQuery();

        if (!rs.next()) {
            System.err.println("Error: File not found in SQLite.");
            return false;
        }

        int ownerId = rs.getInt("user_id");
        String filePath = rs.getString("file_path");
        rs.close();

       
        int currentUserId = getUserIdFromSQLite(username);
        if (currentUserId == -1) {
            System.err.println(" Error: User " + username + " not found in SQLite.");
            return false;
        }

        
        System.out.println(" DEBUG: Attempting to delete file '" + fileName + "' owned by User ID: " + ownerId);
        System.out.println(" DEBUG: Current User '" + username + "' has User ID: " + currentUserId);

      
        if (currentUserId != ownerId) {
            System.err.println(" Permission Denied: User " + username + " (ID: " + currentUserId + 
                               ") is not the owner of file " + fileName + " (Owner ID: " + ownerId + ")");
            return false;
        }

        
        deleteStmt.setString(1, fileName);
        int rowsDeleted = deleteStmt.executeUpdate();
        
        if (rowsDeleted > 0) {
            conn.commit(); 
            
            File file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println(" File deleted from system: " + filePath);
                } else {
                    System.err.println(" Error: Could not delete file from system.");
                }
            } else {
                System.err.println(" Warning: File not found on disk, but deleted from database.");
            }
            return true;
        } else {
            System.err.println(" Error: Failed to delete file from SQLite.");
        }

    } catch (SQLException e) {
        System.err.println(" SQLite Delete Error: " + e.getMessage());
    }
    return false;
}





    public ObservableList<FileData> getAllFiles() {
    ObservableList<FileData> files = FXCollections.observableArrayList();
    String sql = "SELECT filename, owner, size, file_path, upload_date, permissions FROM Files";
    try (Connection conn = connect();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            files.add(new FileData(
                    rs.getString("filename"),
                    rs.getString("owner"),
                    rs.getLong("size"),
                    rs.getString("file_path"),
                    rs.getString("upload_date"),
                    rs.getString("permissions") 
            ));
        }
    } catch (SQLException e) {
        System.err.println("❌ File retrieval error: " + e.getMessage());
    }
    return files;
}



    private String hashPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 10000, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Password hashing error", e);
        }
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
public boolean updateFilePermission(String fileName, String newPermission) {
    String sql = "UPDATE Files SET permissions = ? WHERE filename = ?";
    try (Connection conn = connect()) {
        conn.setAutoCommit(false);
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPermission);
            pstmt.setString(2, fileName);
            int rowsUpdated = pstmt.executeUpdate();
            conn.commit();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Error updating file permission: " + e.getMessage());
        }
    } catch (SQLException e) {
        System.err.println("Database connection error: " + e.getMessage());
    }
    return false;
}




}
