package com.mycompany.javafxapplication1;

import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.File;

public class MySQLDB {

    private static final String DATABASE_NAME = "cwk_db";
    private static final String USERS_TABLE = "Users";
    private static final String FILES_TABLE = "Files";
    private static final String PERMISSIONS_TABLE = "file_permissions";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "nour123";
    private static final String MYSQL_URL = "jdbc:mysql://host.docker.internal:3306/" + DATABASE_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Connection conn;

    

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(MYSQL_URL, USERNAME, PASSWORD);
            conn.setAutoCommit(true);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

   public boolean validateUser(String username, String password) {
    String query = "SELECT id, password_hash, salt, role FROM Users WHERE LOWER(username) = LOWER(?)";
    try (Connection conn = getMySQLConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String storedHash = rs.getString("password_hash");
            String salt = rs.getString("salt");
            String hashedInputPassword = hashPassword(password, salt);
            if (storedHash.equals(hashedInputPassword)) {
                int userId = rs.getInt("id");
                String role = rs.getString("role");
                
                // Store user data in SessionManager
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
        System.err.println("Error validating user in MySQL: " + e.getMessage());
    }
    return false;
}

public void createTables() {
    String usersTable = "CREATE TABLE IF NOT EXISTS " + USERS_TABLE + " ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "username VARCHAR(255) UNIQUE NOT NULL, "
            + "password_hash TEXT NOT NULL, "
            + "salt TEXT NOT NULL, "
            + "role ENUM('standard', 'admin') NOT NULL DEFAULT 'standard', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    String filesTable = "CREATE TABLE IF NOT EXISTS " + FILES_TABLE + " ("
            + "file_id INT AUTO_INCREMENT PRIMARY KEY, "
            + "filename VARCHAR(255) NOT NULL, "
            + "owner VARCHAR(255) NOT NULL, "
            + "size BIGINT NOT NULL, "
            + "file_path VARCHAR(255) NOT NULL, "
            + "permissions VARCHAR(20) DEFAULT 'private', "
            + "upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (owner) REFERENCES Users(username) ON DELETE CASCADE)";

    String permissionsTable = "CREATE TABLE IF NOT EXISTS " + PERMISSIONS_TABLE + " ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "file_id INT NOT NULL, "
            + "user_id INT NOT NULL, "
            + "permission ENUM('read', 'write', 'owner') NOT NULL, "
            + "FOREIGN KEY (file_id) REFERENCES Files(file_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE)";

    try (Connection conn = getMySQLConnection(); Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(usersTable);
        stmt.executeUpdate(filesTable);
        stmt.executeUpdate(permissionsTable);
        System.out.println("Database tables created successfully.");
    } catch (SQLException e) {
        System.err.println("Table creation error: " + e.getMessage());
    }
}


public boolean addFile(File file) {
    String sql = "INSERT INTO " + FILES_TABLE + " (filename, owner, size, file_path, upload_date) VALUES (?, ?, ?, ?, NOW())";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, file.getName());
        pstmt.setString(2, SessionManager.getCurrentUser());
        pstmt.setLong(3, file.length());
        pstmt.setString(4, file.getAbsolutePath());
        
        int rowsInserted = pstmt.executeUpdate();
        if (rowsInserted > 0) {
            System.out.println(" File successfully stored in MySQL: " + file.getName());
            return true;
        } else {
            System.err.println("MySQL file insert failed for: " + file.getName());
        }
    } catch (SQLException e) {
        System.err.println(" MySQL Insert Error: " + e.getMessage());
    }
    return false;
}
public boolean checkUserPermission(String username, String fileName, String requiredPermission) {
    String sql = "SELECT permission FROM file_permissions "
               + "WHERE user_id = (SELECT id FROM Users WHERE username = ?) "
               + "AND file_id = (SELECT file_id FROM Files WHERE filename = ?)";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, username);
        pstmt.setString(2, fileName);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            String userPermission = rs.getString("permission");
            if (userPermission.equals(requiredPermission) || userPermission.equals("owner")) {
                return true;
            }
        }
    } catch (SQLException e) {
        System.err.println("Error checking user permission: " + e.getMessage());
    }
    return false;
}


  public boolean insertFileToMySQL(String fileName, String filePath, long fileSize) {
    int userId = getCurrentUserId(); 
    if (userId == -1) {
        System.err.println(" Error: No valid user ID found. File upload aborted.");
        return false;
    }

    String insertSQL = "INSERT INTO Files (user_id, filename, owner, file_path, size, permissions, upload_date) VALUES (?, ?, ?, ?, ?, 'private', NOW())";

    try (Connection conn = getMySQLConnection();
         PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

        String owner = SessionManager.getCurrentUser();
        insertStmt.setInt(1, userId);
        insertStmt.setString(2, fileName);
        insertStmt.setString(3, owner);
        insertStmt.setString(4, filePath);
        insertStmt.setLong(5, fileSize);

        int rowsInserted = insertStmt.executeUpdate();
        if (rowsInserted > 0) {
            System.out.println("File successfully stored in MySQL: " + fileName);
            return true;
        } else {
            System.err.println(" MySQL file insert failed: No rows affected.");
        }
    } catch (SQLException e) {
        System.err.println(" MySQL Insert Error: " + e.getMessage());
    }
    return false;
}



public int getCurrentUserId() {
    String currentUsername = SessionManager.getCurrentUser();
    if (currentUsername == null) {
        System.err.println(" Error: No user is currently logged in.");
        return -1;
    }

    String sqlMySQL = "SELECT id FROM users WHERE username = ?";
    String sqlSQLite = "SELECT id FROM users WHERE username = ?";
    String insertUserSQL = "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)";

    try (Connection mysqlConn = getMySQLConnection();
         Connection sqliteConn = DB.getSQLiteConnection();
         PreparedStatement stmtMySQL = mysqlConn.prepareStatement(sqlMySQL);
         PreparedStatement stmtSQLite = sqliteConn.prepareStatement(sqlSQLite);
         PreparedStatement insertStmt = mysqlConn.prepareStatement(insertUserSQL)) {

        
        stmtSQLite.setString(1, currentUsername);
        ResultSet rsSQLite = stmtSQLite.executeQuery();
        if (!rsSQLite.next()) {
            System.err.println(" Error: User " + currentUsername + " does not exist in SQLite.");
            return -1;
        }
        int sqliteUserId = rsSQLite.getInt("id");
        rsSQLite.close();

        
        stmtMySQL.setString(1, currentUsername);
        ResultSet rsMySQL = stmtMySQL.executeQuery();

        if (rsMySQL.next()) {
            int mysqlUserId = rsMySQL.getInt("id");

            if (mysqlUserId != sqliteUserId) {
                System.err.println(" User ID mismatch! Updating MySQL user ID to match SQLite.");
                String updateSQL = "UPDATE users SET id = ? WHERE id = ?";
                PreparedStatement updateStmt = mysqlConn.prepareStatement(updateSQL);
                updateStmt.setInt(1, sqliteUserId);
                updateStmt.setInt(2, mysqlUserId);
                updateStmt.executeUpdate();
                updateStmt.close();
            }
            return sqliteUserId;
        }

        rsMySQL.close();

        
        System.out.println(" User " + currentUsername + " not found in MySQL. Recreating...");
        insertStmt.setInt(1, sqliteUserId); 
        insertStmt.setString(2, currentUsername);
        insertStmt.setString(3, "hashedpassword"); 
        insertStmt.setString(4, "admin"); 
        insertStmt.executeUpdate();
        System.out.println(" User " + currentUsername + " added to MySQL with ID: " + sqliteUserId);
        return sqliteUserId;
    } catch (SQLException e) {
        System.err.println(" Error retrieving user ID: " + e.getMessage());
    }
    return -1;
}




public boolean deleteFile(String username, String fileName) {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        conn = getMySQLConnection();

       
        String checkOwnerSQL = "SELECT user_id, file_path FROM Files WHERE filename = ?";
        stmt = conn.prepareStatement(checkOwnerSQL);
        stmt.setString(1, fileName);
        rs = stmt.executeQuery();

        if (!rs.next()) {
            System.err.println(" Error: File not found.");
            return false;
        }

        int ownerId = rs.getInt("user_id");
        String filePath = rs.getString("file_path");
        rs.close();
        stmt.close();

        
        int currentUserId = getCurrentUserId();

        
        if (!SessionManager.isAdmin() && currentUserId != ownerId) {
            System.err.println(" Permission Denied: Only the file owner or admin can delete files.");
            return false;
        }

        
        String deleteSQL = "DELETE FROM Files WHERE filename = ?";
        stmt = conn.prepareStatement(deleteSQL);
        stmt.setString(1, fileName);
        int rowsAffected = stmt.executeUpdate();
        stmt.close();

        
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println(" File deleted from system: " + filePath);
            } else {
                System.err.println(" Error: Could not delete file from system.");
            }
        }

        return rowsAffected > 0;
    } catch (SQLException e) {
        System.err.println(" MySQL Delete Error: " + e.getMessage());
    } finally {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
    return false;
}




public boolean userExists(String username) {
    if (conn == null) {
        System.err.println("Database connection is NULL! Reconnecting...");
        connectToDatabase();
    }

    if (conn == null) {
        System.err.println("Failed to establish a database connection.");
        return false;
    }

    String sql = "SELECT COUNT(*) FROM Users WHERE username = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    } catch (SQLException e) {
        System.err.println("User existence check error: " + e.getMessage());
    }
    return false;
}


public boolean addUser(String username, String password, String role) {
    if (userExists(username)) {
        return false;
    }

    String salt = generateSalt();
    String hashedPassword = hashPassword(password, salt);

    String sql = "INSERT INTO Users (username, password_hash, salt, role) VALUES (?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, username);
        pstmt.setString(2, hashedPassword);
        pstmt.setString(3, salt);
        pstmt.setString(4, role);
        return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("User insertion error: " + e.getMessage());
    }
    return false;
}
public String fetchUserRole(String username) {
    String sql = "SELECT role FROM Users WHERE username = ?";
    try (Connection conn = getMySQLConnection();
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

public boolean updateUserPassword(String username, String newPassword) {
    if (!userExists(username)) {
        return false;
    }

    String salt = generateSalt();
    String hashedPassword = hashPassword(newPassword, salt);
    String sql = "UPDATE Users SET password_hash = ?, salt = ? WHERE username = ?";

    try (Connection conn = getMySQLConnection()) {
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
            System.err.println("MySQL password update error: " + e.getMessage());
            return false;
        } finally {
            conn.setAutoCommit(true);  // Ensure auto-commit is re-enabled
        }
    } catch (SQLException e) {
        System.err.println("MySQL connection error: " + e.getMessage());
    }
    return false;
}




public boolean deleteUser(String username) {
    String sql = "DELETE FROM Users WHERE username = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, username);
        return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("User deletion error: " + e.getMessage());
    }
    return false;
}

  public List<FileData> getAllFiles() {
    List<FileData> files = new ArrayList<>();
    String sql = "SELECT filename, owner, size, file_path, upload_date, permissions FROM Files"; 

    try (Connection conn = getMySQLConnection();
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
        System.err.println(" Error retrieving files from MySQL: " + e.getMessage());
    }
    return files;
}


    public boolean setFilePermission(String fileName, String username, String permission) {
        String sql = "INSERT INTO " + PERMISSIONS_TABLE + " (file_id, user_id, permission) VALUES "
                + "((SELECT file_id FROM " + FILES_TABLE + " WHERE filename = ?), "
                + "(SELECT id FROM " + USERS_TABLE + " WHERE username = ?), ?) "
                + "ON DUPLICATE KEY UPDATE permission = VALUES(permission)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, username);
            pstmt.setString(3, permission);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error setting file permission in MySQL: " + e.getMessage());
        }
        return false;
    }

    public boolean retrieveFile(String fileName, File saveFile) {
        System.err.println("File retrieval not implemented yet.");
        return false;
    }

    private String hashPassword(String password, String salt) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes());
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("Error hashing password", e);
    }
}


    private boolean verifyPassword(String inputPassword, String salt, String storedPassword) {
        return hashPassword(inputPassword, salt).equals(storedPassword);
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public Connection getMySQLConnection() {
    try {
        return DriverManager.getConnection(MYSQL_URL, USERNAME, PASSWORD);
    } catch (SQLException e) {
        System.err.println("Error connecting to MySQL: " + e.getMessage());
        return null;
    }
    }
}

