package com.aiextractor;

import com.aiextractor.builder.StructureBuilder;
import com.aiextractor.commands.BuildCommand;
import com.aiextractor.commands.CaptureCommand;
import com.aiextractor.commands.ExtractorCommand;
import com.aiextractor.commands.ScanCommand;
import com.aiextractor.detection.StructureDetector;
import com.aiextractor.export.StructureExporter;
import com.aiextractor.scanner.WorldScanner;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class StructureExtractorPlugin extends JavaPlugin {

    private static StructureExtractorPlugin instance;
    
    private StructureDetector structureDetector;
    private StructureExporter structureExporter;
    private WorldScanner worldScanner;
    private StructureBuilder structureBuilder;
    private File exportDirectory;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Setup export directory
        String exportPath = getConfig().getString("export.directory", "exports");
        exportDirectory = new File(getDataFolder(), exportPath);
        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs();
        }
        
        // Initialize components
        structureDetector = new StructureDetector(this);
        structureExporter = new StructureExporter(this);
        worldScanner = new WorldScanner(this);
        structureBuilder = new StructureBuilder(this);
        
        // Register commands with tab completion
        ScanCommand scanCommand = new ScanCommand(this);
        getCommand("scan").setExecutor(scanCommand);
        getCommand("scan").setTabCompleter(scanCommand);
        
        CaptureCommand captureCommand = new CaptureCommand(this);
        getCommand("capture").setExecutor(captureCommand);
        getCommand("capture").setTabCompleter(captureCommand);
        
        ExtractorCommand extractorCommand = new ExtractorCommand(this);
        getCommand("extractor").setExecutor(extractorCommand);
        getCommand("extractor").setTabCompleter(extractorCommand);
        
        BuildCommand buildCommand = new BuildCommand(this);
        getCommand("build").setExecutor(buildCommand);
        getCommand("build").setTabCompleter(buildCommand);
        
        getLogger().info("StructureExtractor has been enabled!");
        getLogger().info("Export directory: " + exportDirectory.getAbsolutePath());
    }

    @Override
    public void onDisable() {
        // Stop any running scans
        if (worldScanner != null && worldScanner.isScanning()) {
            worldScanner.stopScan();
        }
        
        getLogger().info("StructureExtractor has been disabled!");
    }
    
    public static StructureExtractorPlugin getInstance() {
        return instance;
    }
    
    public StructureDetector getStructureDetector() {
        return structureDetector;
    }
    
    public StructureExporter getStructureExporter() {
        return structureExporter;
    }
    
    public WorldScanner getWorldScanner() {
        return worldScanner;
    }
    
    public StructureBuilder getStructureBuilder() {
        return structureBuilder;
    }
    
    public File getExportDirectory() {
        return exportDirectory;
    }
}
