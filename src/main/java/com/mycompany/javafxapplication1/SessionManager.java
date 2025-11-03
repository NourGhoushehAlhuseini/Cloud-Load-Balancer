package com.mycompany.javafxapplication1;

public class SessionManager {

    private static String currentUser;
    private static int currentUserId = -1;
    private static String currentRole = "standard";

    public static void setCurrentUser(String user, int userId, String role) {
    currentUser = user;
    currentUserId = userId;
    currentRole = role.toLowerCase(); // Ensure case consistency
    System.out.println("User logged in: " + currentUser + " | Role: " + currentRole);
}


    public static String getCurrentUser() {
        return currentUser;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static boolean isAdmin() {
        return currentRole != null && currentRole.equalsIgnoreCase("admin");
    }

    public static boolean isLoggedIn() {
        return currentUser != null && currentUserId != -1;
    }

    public static void logout() {
        currentUser = null;
        currentUserId = -1;
        currentRole = "standard";
    }
}
