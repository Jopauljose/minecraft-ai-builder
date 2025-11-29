package com.aiextractor.export;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.models.ExtractedStructure;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exports structures to JSON files
 */
public class StructureExporter {

    private final StructureExtractorPlugin plugin;
    private final Gson gson;
    private final Gson gsonPretty;
    private final AtomicInteger exportCounter;

    public StructureExporter(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.gsonPretty = new GsonBuilder().setPrettyPrinting().create();
        this.exportCounter = new AtomicInteger(countExistingExports());
    }

    /**
     * Count existing export files to continue numbering
     */
    private int countExistingExports() {
        File exportDir = plugin.getExportDirectory();
        int maxNum = 0;
        
        if (exportDir.exists()) {
            File[] worldDirs = exportDir.listFiles(File::isDirectory);
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    File[] jsonFiles = worldDir.listFiles((dir, name) -> name.endsWith(".json"));
                    if (jsonFiles != null) {
                        for (File jsonFile : jsonFiles) {
                            String name = jsonFile.getName();
                            // Try to extract number from structure_XXX.json pattern
                            if (name.startsWith("structure_")) {
                                try {
                                    String numPart = name.substring(10, name.length() - 5);
                                    int num = Integer.parseInt(numPart);
                                    maxNum = Math.max(maxNum, num);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }
            }
        }
        
        return maxNum;
    }

    /**
     * Export a structure asynchronously
     */
    public CompletableFuture<ExportResult> exportAsync(ExtractedStructure structure) {
        CompletableFuture<ExportResult> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                ExportResult result = exportSync(structure);
                future.complete(result);
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Export a structure synchronously (call from async context)
     */
    public ExportResult exportSync(ExtractedStructure structure) {
        try {
            // Get world directory
            String worldName = structure.getMetadata().getSourceWorld();
            if (worldName == null) {
                worldName = "unknown";
            }
            
            File worldDir = new File(plugin.getExportDirectory(), sanitizeFileName(worldName));
            if (!worldDir.exists()) {
                worldDir.mkdirs();
            }
            
            // Generate filename
            String fileName;
            if (structure.getName() != null && !structure.getName().isEmpty() && 
                !structure.getName().startsWith("structure_")) {
                fileName = sanitizeFileName(structure.getName()) + ".json";
            } else {
                int num = exportCounter.incrementAndGet();
                fileName = String.format("structure_%03d.json", num);
                structure.setName(String.format("structure_%03d", num));
            }
            
            File outputFile = new File(worldDir, fileName);
            
            // Avoid overwriting
            int suffix = 1;
            while (outputFile.exists()) {
                String baseName = fileName.substring(0, fileName.length() - 5);
                outputFile = new File(worldDir, baseName + "_" + suffix + ".json");
                suffix++;
            }
            
            // Write JSON
            boolean prettyPrint = plugin.getConfig().getBoolean("export.pretty-json", true);
            String json = prettyPrint ? gsonPretty.toJson(structure) : gson.toJson(structure);
            
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(json);
            }
            
            return new ExportResult(true, outputFile, structure.getBlockCount());
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to export structure: " + e.getMessage());
            return new ExportResult(false, null, 0, e.getMessage());
        }
    }

    /**
     * Sanitize a string for use as a filename
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                   .replaceAll("_+", "_")
                   .toLowerCase();
    }

    /**
     * Get export statistics
     */
    public ExportStats getStats() {
        File exportDir = plugin.getExportDirectory();
        int totalFiles = 0;
        long totalSize = 0;
        int worldCount = 0;
        
        if (exportDir.exists()) {
            File[] worldDirs = exportDir.listFiles(File::isDirectory);
            if (worldDirs != null) {
                worldCount = worldDirs.length;
                for (File worldDir : worldDirs) {
                    File[] jsonFiles = worldDir.listFiles((dir, name) -> name.endsWith(".json"));
                    if (jsonFiles != null) {
                        totalFiles += jsonFiles.length;
                        for (File f : jsonFiles) {
                            totalSize += f.length();
                        }
                    }
                }
            }
        }
        
        return new ExportStats(totalFiles, totalSize, worldCount);
    }

    /**
     * Result of an export operation
     */
    public static class ExportResult {
        private final boolean success;
        private final File outputFile;
        private final int blockCount;
        private final String error;

        public ExportResult(boolean success, File outputFile, int blockCount) {
            this.success = success;
            this.outputFile = outputFile;
            this.blockCount = blockCount;
            this.error = null;
        }

        public ExportResult(boolean success, File outputFile, int blockCount, String error) {
            this.success = success;
            this.outputFile = outputFile;
            this.blockCount = blockCount;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public File getOutputFile() {
            return outputFile;
        }

        public int getBlockCount() {
            return blockCount;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Export statistics
     */
    public static class ExportStats {
        private final int totalFiles;
        private final long totalSizeBytes;
        private final int worldCount;

        public ExportStats(int totalFiles, long totalSizeBytes, int worldCount) {
            this.totalFiles = totalFiles;
            this.totalSizeBytes = totalSizeBytes;
            this.worldCount = worldCount;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public long getTotalSizeBytes() {
            return totalSizeBytes;
        }

        public String getTotalSizeFormatted() {
            if (totalSizeBytes < 1024) {
                return totalSizeBytes + " B";
            } else if (totalSizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", totalSizeBytes / 1024.0);
            } else {
                return String.format("%.1f MB", totalSizeBytes / (1024.0 * 1024.0));
            }
        }

        public int getWorldCount() {
            return worldCount;
        }
    }
}
