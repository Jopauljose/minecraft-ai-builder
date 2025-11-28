package com.aibuild.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendClient {
    private final String backendUrl;
    private final Gson gson;

    public BackendClient(String backendUrl) {
        this.backendUrl = backendUrl;
        this.gson = new Gson();
    }

    public JsonObject generateStructure(String prompt, int maxDim) {
        try {
            URL url = new URL(backendUrl + "/generate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("prompt", prompt);
            requestBody.addProperty("max_dim", maxDim);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(gson.toJson(requestBody).getBytes());
                os.flush();
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return gson.fromJson(response.toString(), JsonObject.class);
            } else {
                Bukkit.getLogger().severe("Backend error: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error communicating with backend: " + e.getMessage());
        }
        return null;
    }
}