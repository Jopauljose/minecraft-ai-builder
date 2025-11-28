package com.aibuild;

import org.bukkit.plugin.java.JavaPlugin;
import com.aibuild.commands.AIBuildCommand;
import com.aibuild.commands.AIUndoCommand;
import com.aibuild.models.UndoBuffer;
import com.aibuild.services.BackendClient;
import com.aibuild.services.StructureBuilder;

public class AIBuildPlugin extends JavaPlugin {

    private BackendClient backendClient;
    private StructureBuilder structureBuilder;
    private UndoBuffer undoBuffer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String backendUrl = getConfig().getString("backend-url", "http://localhost:3000");
        int maxDim = getConfig().getInt("max-dimension", 32);
        
        this.undoBuffer = new UndoBuffer();
        this.backendClient = new BackendClient(backendUrl);
        this.structureBuilder = new StructureBuilder(this, undoBuffer);
        
        getCommand("aibuild").setExecutor(new AIBuildCommand(this, backendClient, structureBuilder, maxDim));
        getCommand("aiundo").setExecutor(new AIUndoCommand(this, undoBuffer));
        
        getLogger().info("AIBuildPlugin has been enabled.");
        getLogger().info("Backend URL: " + backendUrl);
    }

    @Override
    public void onDisable() {
        getLogger().info("AIBuildPlugin has been disabled.");
    }
    
    public UndoBuffer getUndoBuffer() {
        return undoBuffer;
    }
    
    public BackendClient getBackendClient() {
        return backendClient;
    }
    
    public StructureBuilder getStructureBuilder() {
        return structureBuilder;
    }
}