package xyz.felixcraft.felixskin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FelixSkinConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("FelixSkin");
    private static final String CONFIG_FILE = "felixskin_config.json";
    
    private final File configFile;
    private final Gson gson;
    private ConfigData configData;
    
    public static class ConfigData {
        public Map<String, SkinConfig> playerSkins = new HashMap<>();
        public boolean enableHDSupport = true;
        public int maxSkinSize = 4096; // Maximum skin dimension
        public boolean autoSaveSkins = true;
        public String defaultSkinPath = "";

        public ConfigData() {}
    }
    
    public static class SkinConfig {
        public String skinPath;
        public String skinName;
        public boolean isSlim;
        public long lastUsed;
        public int width;
        public int height;
        public String textureId;
        public String nickname;

        public SkinConfig() {}

        public SkinConfig(String skinPath, String skinName, boolean isSlim, int width, int height) {
            this.skinPath = skinPath;
            this.skinName = skinName;
            this.isSlim = isSlim;
            this.width = width;
            this.height = height;
            this.lastUsed = System.currentTimeMillis();
            this.nickname = "";
        }

        public SkinConfig(String skinPath, String skinName, boolean isSlim, int width, int height, String nickname) {
            this.skinPath = skinPath;
            this.skinName = skinName;
            this.isSlim = isSlim;
            this.width = width;
            this.height = height;
            this.lastUsed = System.currentTimeMillis();
            this.nickname = nickname != null ? nickname : "";
        }
    }
    
    public FelixSkinConfig() {
        // Use proper Minecraft config directory
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "felixskin");
        this.configFile = new File(configDir, CONFIG_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    configData = gson.fromJson(reader, ConfigData.class);
                    LOGGER.info("Config loaded successfully");
                }
            } else {
                configData = new ConfigData();
                saveConfig();
                LOGGER.info("New config created");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
            configData = new ConfigData();
        }
    }
    
    public void saveConfig() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(configData, writer);
                LOGGER.info("Config saved successfully");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
    
    public void savePlayerSkin(UUID playerUuid, String skinPath, String skinName, boolean isSlim, int width, int height) {
        String playerId = playerUuid.toString();
        SkinConfig skinConfig = new SkinConfig(skinPath, skinName, isSlim, width, height);
        configData.playerSkins.put(playerId, skinConfig);
        saveConfig();
        LOGGER.info("Saved skin config for player: {}", playerId);
    }
    
    public SkinConfig getPlayerSkin(UUID playerUuid) {
        String playerId = playerUuid.toString();
        return configData.playerSkins.get(playerId);
    }
    
    public boolean hasPlayerSkin(UUID playerUuid) {
        String playerId = playerUuid.toString();
        return configData.playerSkins.containsKey(playerId);
    }
    
    public void removePlayerSkin(UUID playerUuid) {
        String playerId = playerUuid.toString();
        configData.playerSkins.remove(playerId);
        saveConfig();
        LOGGER.info("Removed skin config for player: {}", playerId);
    }
    
    public boolean isHDSupportEnabled() {
        return configData.enableHDSupport;
    }
    
    public int getMaxSkinSize() {
        return configData.maxSkinSize;
    }
    
    public boolean isAutoSaveEnabled() {
        return configData.autoSaveSkins;
    }
    
    public String getDefaultSkinPath() {
        return configData.defaultSkinPath;
    }
    
    public void setDefaultSkinPath(String path) {
        configData.defaultSkinPath = path;
        saveConfig();
    }
    
    public void setHDSupport(boolean enabled) {
        configData.enableHDSupport = enabled;
        saveConfig();
    }
    
    public void setMaxSkinSize(int size) {
        configData.maxSkinSize = size;
        saveConfig();
    }
    
    public void setAutoSave(boolean enabled) {
        configData.autoSaveSkins = enabled;
        saveConfig();
    }

    public String getPlayerNickname(UUID playerUuid) {
        SkinConfig skinConfig = getPlayerSkin(playerUuid);
        return skinConfig != null ? skinConfig.nickname : "";
    }

    public void setPlayerNickname(UUID playerUuid, String nickname) {
        String playerId = playerUuid.toString();
        SkinConfig skinConfig = configData.playerSkins.get(playerId);
        if (skinConfig != null) {
            skinConfig.nickname = nickname != null ? nickname : "";
            saveConfig();
            LOGGER.info("Updated nickname for player: {}", playerId);
        }
    }

    public void savePlayerSkinWithNickname(UUID playerUuid, String skinPath, String skinName, boolean isSlim, int width, int height, String nickname) {
        String playerId = playerUuid.toString();
        SkinConfig skinConfig = new SkinConfig(skinPath, skinName, isSlim, width, height, nickname);
        configData.playerSkins.put(playerId, skinConfig);
        saveConfig();
        LOGGER.info("Saved skin config with nickname for player: {}", playerId);
    }

}

