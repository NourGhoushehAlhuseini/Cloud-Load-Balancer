package com.mycompany.javafxapplication1;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoadBalancer {
    private static final String LOAD_BALANCER_URL = "http://172.19.0.7:8080";

    public LoadBalancer() {}

    // Retrieve and decrypt file from Load Balancer
    public boolean retrieveAndDecryptFile(FileData fileData, File saveFile) {
        String payload = "fileName=" + fileData.getFileName();
        return sendRequest("/download", payload);
    }

    // Store file chunks via Load Balancer
    public boolean storeChunks(Object chunks) {
        String payload = "chunkData=" + chunks.toString();
        return sendRequest("/upload", payload);
    }

    // Generic HTTP request sender to communicate with Load Balancer
    private boolean sendRequest(String endpoint, String payload) {
        try {
            URL url = new URL(LOAD_BALANCER_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Response from Load Balancer: " + responseCode);

            return responseCode == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
