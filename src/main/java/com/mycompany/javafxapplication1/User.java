package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * User class to represent user data (username and password).
 * It uses StringProperty for JavaFX binding with TableView.
 */
public class User {

    private final StringProperty user;
    private final StringProperty pass;

    /**
     * Constructor that initializes user and pass properties.
     *
     * @param user the username
     * @param pass the password
     */
    public User(String user, String pass) {
        this.user = new SimpleStringProperty(user);
        this.pass = new SimpleStringProperty(pass);
    }

    /**
     * Getter for user (returns the value as a String).
     *
     * @return the username as a String
     */
    public String getUser() {
        return user.get();
    }

    /**
     * Setter for user (updates the value of the user property).
     *
     * @param user the username to set
     */
    public void setUser(String user) {
        this.user.set(user);
    }

    /**
     * Getter for pass (returns the value as a String).
     *
     * @return the password as a String
     */
    public String getPass() {
        return pass.get();
    }

    /**
     * Setter for pass (updates the value of the pass property).
     *
     * @param pass the password to set
     */
    public void setPass(String pass) {
        this.pass.set(pass);
    }

    /**
     * Property getter for user, required for TableView binding.
     *
     * @return the user property for binding
     */
    public StringProperty userProperty() {
        return user;
    }

    /**
     * Property getter for pass, required for TableView binding.
     *
     * @return the pass property for binding
     */
    public StringProperty passProperty() {
        return pass;
    }
}
