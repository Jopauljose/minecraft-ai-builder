package com.aibuild.utils;

import com.aibuild.models.Structure;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class JsonParser {

    /**
     * Parse a JSON response into a Structure object
     * @param jsonObject The JSON object from the backend
     * @return The parsed Structure
     */
    public static Structure parseStructure(JsonObject jsonObject) {
        // Parse size array
        JsonArray sizeArray = jsonObject.getAsJsonArray("size");
        int[] size = new int[3];
        for (int i = 0; i < 3 && i < sizeArray.size(); i++) {
            size[i] = sizeArray.get(i).getAsInt();
        }

        // Parse palette
        Map<String, String> palette = new HashMap<>();
        JsonObject paletteObj = jsonObject.getAsJsonObject("palette");
        for (Map.Entry<String, JsonElement> entry : paletteObj.entrySet()) {
            palette.put(entry.getKey(), entry.getValue().getAsString());
        }

        // Parse layers
        Map<Integer, String[][]> layers = new HashMap<>();
        JsonObject layersObj = jsonObject.getAsJsonObject("layers");
        for (Map.Entry<String, JsonElement> layerEntry : layersObj.entrySet()) {
            int layerIndex = Integer.parseInt(layerEntry.getKey());
            JsonArray layerArray = layerEntry.getValue().getAsJsonArray();
            
            String[][] layer = new String[layerArray.size()][];
            for (int z = 0; z < layerArray.size(); z++) {
                JsonArray rowArray = layerArray.get(z).getAsJsonArray();
                layer[z] = new String[rowArray.size()];
                for (int x = 0; x < rowArray.size(); x++) {
                    layer[z][x] = rowArray.get(x).getAsString();
                }
            }
            layers.put(layerIndex, layer);
        }

        Structure structure = new Structure(size, palette, layers);
        
        // Set name if present
        if (jsonObject.has("name")) {
            structure.setName(jsonObject.get("name").getAsString());
        }

        return structure;
    }

    /**
     * Parse a JSON string into a Structure object
     * @param jsonString The JSON string from the backend
     * @return The parsed Structure
     */
    public static Structure parseStructure(String jsonString) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        return parseStructure(jsonObject);
    }
}