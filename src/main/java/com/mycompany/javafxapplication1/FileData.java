package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.LongProperty;

public class FileData {
    private final SimpleStringProperty fileName;
    private final SimpleStringProperty owner;
    private final SimpleLongProperty size;
    private final SimpleStringProperty filePath;
    private final SimpleStringProperty uploadDate;
    private final SimpleStringProperty permissions; 

    public FileData(String fileName, String owner, long size, String filePath, String uploadDate, String permissions) {
        this.fileName = new SimpleStringProperty(fileName);
        this.owner = new SimpleStringProperty(owner);
        this.size = new SimpleLongProperty(size);
        this.filePath = new SimpleStringProperty(filePath);
        this.uploadDate = new SimpleStringProperty(uploadDate);
        this.permissions = new SimpleStringProperty(permissions); 
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public String getOwner() {
        return owner.get();
    }

    public void setOwner(String owner) {
        this.owner.set(owner);
    }

    public long getSize() {
        return size.get();
    }

    public void setSize(long size) {
        this.size.set(size);
    }

    public String getFilePath() {
        return filePath.get();
    }

    public void setFilePath(String filePath) {
        this.filePath.set(filePath);
    }

    public String getUploadDate() {
        return uploadDate.get();
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate.set(uploadDate);
    }

    public String getPermissions() { 
        return permissions.get();
    }

    public void setPermissions(String permissions) { 
        this.permissions.set(permissions);
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public StringProperty ownerProperty() {
        return owner;
    }

    public LongProperty sizeProperty() {
        return size;
    }

    public StringProperty filePathProperty() {
        return filePath;
    }

    public StringProperty uploadDateProperty() {
        return uploadDate;
    }

    public StringProperty permissionsProperty() { 
        return permissions;
    }
}
