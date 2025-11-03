package com.mycompany.javafxapplication1;

import com.mycompany.javafxapplication1.FileData;
import com.mycompany.javafxapplication1.LoadBalancer;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        LoadBalancer lb = new LoadBalancer();

        System.out.println("Testing Load Balancer Connection...");

        boolean uploadSuccess = lb.storeChunks("Test Chunk Data");
        System.out.println(uploadSuccess ? "Upload successful!" : "Upload failed!");

        FileData fileData = new FileData(
            "testFile.txt",
            "admin",
            2048L,
            "/home/ntu-user/NetBeansProjects/javaaanour1/cwk (1)/cwk/files/testFile.txt",
            "2025-02-20",
            "read-write"
        );

        boolean downloadSuccess = lb.retrieveAndDecryptFile(fileData, new File("downloaded_testFile.txt"));
        System.out.println(downloadSuccess ? "Download successful!" : "Download failed!");
    }
}
