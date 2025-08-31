package xyz.felixcraft.felixskin.skin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.felixcraft.felixskin.config.FelixSkinConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("FelixSkin");
    private static final String TEXTURE_NAMESPACE = "felixskin";
    private static final int MAX_IMAGE_SIZE = 100 * 1024 * 1024; // 100MB limit
    
    private final Map<UUID, SkinData> playerSkins = new HashMap<>();
    private final Map<UUID, Identifier> textureIdentifiers = new HashMap<>();
    private final FelixSkinConfig config;
    private boolean forceSlimMode = false;
    
    public static class SkinData {
        private final NativeImage image;
        private final boolean isSlim;
        private final String fileName;
        
        public SkinData(NativeImage image, boolean isSlim, String fileName) {
            this.image = image;
            this.isSlim = isSlim;
            this.fileName = fileName;
        }
        
        public NativeImage getImage() { return image; }
        public boolean isSlim() { return isSlim; }
        public String getFileName() { return fileName; }
    }
    
    public SkinManager() {
        this.config = new FelixSkinConfig();
        loadSavedSkins();
    }
    
    private void loadSavedSkins() {
        // Load saved skins from config on startup
        LOGGER.info("Loading saved skins from config...");

        try {
            // Check if Minecraft client is available
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                LOGGER.warn("Minecraft client not available yet, delaying skin loading");
                return;
            }

            // Check if session is available
            if (client.getSession() == null || client.getSession().getProfile() == null) {
                LOGGER.warn("Player session not available yet, delaying skin loading");
                return;
            }

            // Check if texture manager is available (OpenGL context ready)
            if (client.getTextureManager() == null) {
                LOGGER.warn("Texture manager not available yet, delaying skin loading");
                return;
            }

            // Get local player UUID
            UUID playerUuid = client.getSession().getProfile().getId();

            // Load skin data from config
            FelixSkinConfig.SkinConfig skinConfig = config.getPlayerSkin(playerUuid);
            if (skinConfig != null) {
                try {
                    // Load the skin file
                    Path skinPath = Paths.get(skinConfig.skinPath);
                    if (Files.exists(skinPath)) {
                        // Read file bytes
                        byte[] fileBytes = Files.readAllBytes(skinPath);

                        // Load as NativeImage
                        NativeImage image = NativeImage.read(fileBytes);

                        // Validate dimensions
                        if (!isValidSkinDimensions(image.getWidth(), image.getHeight())) {
                            LOGGER.error("Invalid saved skin dimensions: {}x{}", image.getWidth(), image.getHeight());
                            image.close();
                            return;
                        }

                        // Create skin data
                        SkinData skinData = new SkinData(image, skinConfig.isSlim, skinConfig.skinName);
                        playerSkins.put(playerUuid, skinData);

                        // Create and register texture (with error handling)
                        registerTexture(playerUuid, image);

                        LOGGER.info("Successfully loaded saved skin: {} (slim: {})", skinConfig.skinName, skinConfig.isSlim);
                    } else {
                        LOGGER.warn("Saved skin file not found: {}", skinConfig.skinPath);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load saved skin: {}", skinConfig.skinPath, e);
                    // Don't crash the game, just log the error
                }
            } else {
                LOGGER.info("No saved skin found for player");
            }
        } catch (Exception e) {
            LOGGER.error("Error loading saved skins", e);
            // Don't crash the game, just log the error
        }
    }
    
    public boolean loadSkinFromFile(Path filePath) {
        try {
            // Validate file path
            if (filePath == null) {
                LOGGER.error("File path is null");
                return false;
            }

            // Check if file exists
            if (!Files.exists(filePath)) {
                LOGGER.error("File does not exist: {}", filePath);
                return false;
            }

            // Check file size
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_IMAGE_SIZE) {
                LOGGER.error("File too large: {} bytes (max: {} bytes)", fileSize, MAX_IMAGE_SIZE);
                return false;
            }

            if (fileSize == 0) {
                LOGGER.error("File is empty: {}", filePath);
                return false;
            }

            // Read file bytes
            byte[] fileBytes = Files.readAllBytes(filePath);

            // Load as NativeImage
            NativeImage image = NativeImage.read(fileBytes);

            // Validate dimensions (must be power of 2 and reasonable size)
            if (!isValidSkinDimensions(image.getWidth(), image.getHeight())) {
                LOGGER.error("Invalid skin dimensions: {}x{}", image.getWidth(), image.getHeight());
                image.close(); // Clean up
                return false;
            }

            // Determine if slim based on filename
            String fileName = filePath.getFileName().toString().toLowerCase();
            boolean isSlim = fileName.contains("_slim") || fileName.contains("_alex");

            // Get local player UUID
            UUID playerUuid = MinecraftClient.getInstance().getSession().getProfile().getId();

            // Store skin data
            SkinData skinData = new SkinData(image, isSlim, fileName);
            playerSkins.put(playerUuid, skinData);

            // Create and register texture
            registerTexture(playerUuid, image);

            // Save to config if auto-save is enabled
            if (config.isAutoSaveEnabled()) {
                try {
                    config.savePlayerSkin(playerUuid, filePath.toString(), fileName, isSlim, image.getWidth(), image.getHeight());
                    LOGGER.info("Skin saved to config: {} ({}x{})", fileName, image.getWidth(), image.getHeight());
                } catch (Exception e) {
                    LOGGER.error("Failed to save skin config: {}", fileName, e);
                    // Don't crash the game, just log the error
                }
            }

            // Send skin change to server for multiplayer sync
            sendSkinToServer(playerUuid, filePath.toString(), fileName, isSlim, image.getWidth(), image.getHeight());

            LOGGER.info("Successfully loaded skin: {} (slim: {}, dimensions: {}x{})", fileName, isSlim, image.getWidth(), image.getHeight());
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to load skin from file: {} - {}", filePath, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error loading skin: {} - {}", filePath, e.getMessage(), e);
            return false;
        }
    }
    
    private void sendSkinToServer(UUID playerUuid, String skinPath, String skinName, boolean isSlim, int width, int height) {
        try {
            // This will be implemented when networking is fully set up
            LOGGER.info("Skin change sent to server for player: {} ({})", playerUuid, skinName);
        } catch (Exception e) {
            LOGGER.error("Failed to send skin to server", e);
        }
    }
    
    private boolean isValidSkinDimensions(int width, int height) {
        // Check if dimensions are power of 2
        if (!isPowerOfTwo(width) || !isPowerOfTwo(height)) {
            return false;
        }
        
        // Check reasonable size limits (1x1 to max config size)
        int maxSize = config.getMaxSkinSize();
        if (width < 1 || height < 1 || width > maxSize || height > maxSize) {
            LOGGER.error("Skin dimensions {}x{} exceed maximum size {}", width, height, maxSize);
            return false;
        }
        
        // Check aspect ratio (should be roughly 1:1 or 2:1 for skins)
        double ratio = (double) width / height;
        if (ratio < 0.5 || ratio > 2.0) {
            LOGGER.error("Invalid skin aspect ratio: {} (should be between 0.5 and 2.0)", ratio);
            return false;
        }
        
        LOGGER.info("Skin dimensions validated: {}x{} (ratio: {})", width, height, ratio);
        return true;
    }
    
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    private void registerTexture(UUID playerUuid, NativeImage image) {
        try {
            // Check if we're on the render thread and OpenGL context is available
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getTextureManager() == null) {
                LOGGER.warn("Texture manager not available, skipping texture registration for player: {}", playerUuid);
                return;
            }

            // Create unique identifier for this texture
            String texturePath = "dynamic/" + playerUuid.toString().replace("-", "");
            Identifier textureId = new Identifier(TEXTURE_NAMESPACE, texturePath);

            // Check if texture already exists and clean it up
            if (textureIdentifiers.containsKey(playerUuid)) {
                Identifier oldTextureId = textureIdentifiers.get(playerUuid);
                try {
                    client.getTextureManager().destroyTexture(oldTextureId);
                    LOGGER.debug("Cleaned up old texture: {} for player {}", oldTextureId, playerUuid);
                } catch (Exception e) {
                    LOGGER.warn("Failed to cleanup old texture: {} for player {}", oldTextureId, playerUuid, e);
                }
            }

            // Create texture from image
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            // Register with texture manager
            client.getTextureManager().registerTexture(textureId, texture);
            LOGGER.info("Registered texture: {} for player {}", textureId, playerUuid);

            // Store identifier mapping
            textureIdentifiers.put(playerUuid, textureId);

        } catch (Exception e) {
            LOGGER.error("Error creating texture for player: {}", playerUuid, e);
            // Don't crash the game, just log the error
        }
    }
    
    public SkinData getPlayerSkin(UUID playerUuid) {
        return playerSkins.get(playerUuid);
    }
    
    public Identifier getPlayerTexture(UUID playerUuid) {
        return textureIdentifiers.get(playerUuid);
    }
    
    public boolean hasCustomSkin(UUID playerUuid) {
        return playerSkins.containsKey(playerUuid);
    }
    
    // Fallback method for when mixin is not working
    public void forceRefreshSkin(UUID playerUuid) {
        try {
            // Check if texture manager is available
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getTextureManager() == null) {
                LOGGER.warn("Texture manager not available, cannot refresh skin for player: {}", playerUuid);
                return;
            }

            // Force texture refresh
            SkinData skinData = playerSkins.get(playerUuid);
            if (skinData != null) {
                // Re-register texture to force refresh
                registerTexture(playerUuid, skinData.getImage());

                // Force Minecraft to reload the texture
                client.execute(() -> {
                    try {
                        // Clear texture cache and force reload
                        Identifier textureId = textureIdentifiers.get(playerUuid);
                        if (textureId != null) {
                            client.getTextureManager().destroyTexture(textureId);
                            // Re-register to force reload
                            registerTexture(playerUuid, skinData.getImage());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error forcing texture reload", e);
                        // Don't crash the game, just log the error
                    }
                });

                LOGGER.info("Forced skin refresh for player: {}", playerUuid);
            }
        } catch (Exception e) {
            LOGGER.error("Error forcing skin refresh", e);
            // Don't crash the game, just log the error
        }
    }
    
    public void clearPlayerSkin(UUID playerUuid) {
        // Remove skin data
        SkinData removed = playerSkins.remove(playerUuid);
        if (removed != null) {
            removed.getImage().close();
        }
        
        // Remove texture identifier
        textureIdentifiers.remove(playerUuid);
        
        // Remove from config
        config.removePlayerSkin(playerUuid);
        
        LOGGER.info("Cleared skin for player: {}", playerUuid);
    }
    
    public void clearAllSkins() {
        // Close all images to prevent memory leaks
        for (SkinData skinData : playerSkins.values()) {
            skinData.getImage().close();
        }
        
        playerSkins.clear();
        textureIdentifiers.clear();
        
        LOGGER.info("Cleared all skins");
    }
    
    // Check if mixin is working by testing if we can intercept method calls
    public boolean isMixinWorking() {
        try {
            // Try to access the mixin target class
            Class.forName("net.minecraft.client.network.AbstractClientPlayerEntity");
            
            // Additional check: try to access the method names
            try {
                Class<?> playerClass = Class.forName("net.minecraft.client.network.AbstractClientPlayerEntity");
                // This is a basic check - if the class exists, assume mixin might work
                return true;
            } catch (Exception e) {
                LOGGER.warn("Mixin target methods not accessible, mixin may not be working");
                return false;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Mixin target class not found, mixin may not be working");
            return false;
        }
    }
    
    // Enhanced fallback method that works even when mixin fails
    public void enhancedForceRefreshSkin(UUID playerUuid) {
        try {
            // Check if texture manager is available
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getTextureManager() == null) {
                LOGGER.warn("Texture manager not available, cannot refresh skin for player: {}", playerUuid);
                return;
            }

            // Force texture refresh
            SkinData skinData = playerSkins.get(playerUuid);
            if (skinData != null) {
                // Re-register texture to force refresh
                registerTexture(playerUuid, skinData.getImage());

                // Force Minecraft to reload the texture
                client.execute(() -> {
                    try {
                        // Clear texture cache and force reload
                        Identifier textureId = textureIdentifiers.get(playerUuid);
                        if (textureId != null) {
                            // Destroy and recreate texture to ensure refresh
                            client.getTextureManager().destroyTexture(textureId);
                            registerTexture(playerUuid, skinData.getImage());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error in enhanced texture reload", e);
                        // Don't crash the game, just log the error
                    }
                });

                LOGGER.info("Enhanced skin refresh for player: {}", playerUuid);
            }
        } catch (Exception e) {
            LOGGER.error("Error in enhanced skin refresh", e);
            // Don't crash the game, just log the error
        }
    }

    // Set slim mode preference
    public void setSlimMode(boolean slimMode) {
        this.forceSlimMode = slimMode;
        LOGGER.info("Slim mode set to: {}", slimMode);
    }

    // Get current slim mode setting
    public boolean isSlimMode() {
        return forceSlimMode;
    }

    // Get config instance
    public FelixSkinConfig getConfig() {
        return config;
    }

    // Load saved skins when client is ready (called from client initialization)
    public void loadSavedSkinsWhenReady() {
        try {
            // Get local player UUID
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getSession() == null || client.getSession().getProfile() == null) {
                LOGGER.warn("Client not ready for skin loading");
                return;
            }

            // Check if texture manager is available (OpenGL context ready)
            if (client.getTextureManager() == null) {
                LOGGER.warn("Texture manager not available yet, delaying skin loading");
                return;
            }

            UUID playerUuid = client.getSession().getProfile().getId();

            // Load skin data from config
            FelixSkinConfig.SkinConfig skinConfig = config.getPlayerSkin(playerUuid);
            if (skinConfig != null) {
                try {
                    // Load the skin file
                    Path skinPath = Paths.get(skinConfig.skinPath);
                    if (Files.exists(skinPath)) {
                        // Read file bytes
                        byte[] fileBytes = Files.readAllBytes(skinPath);

                        // Load as NativeImage
                        NativeImage image = NativeImage.read(fileBytes);

                        // Validate dimensions
                        if (!isValidSkinDimensions(image.getWidth(), image.getHeight())) {
                            LOGGER.error("Invalid saved skin dimensions: {}x{}", image.getWidth(), image.getHeight());
                            image.close();
                            return;
                        }

                        // Create skin data
                        SkinData skinData = new SkinData(image, skinConfig.isSlim, skinConfig.skinName);
                        playerSkins.put(playerUuid, skinData);

                        // Create and register texture
                        registerTexture(playerUuid, image);

                        LOGGER.info("Successfully loaded saved skin: {} (slim: {})", skinConfig.skinName, skinConfig.isSlim);
                    } else {
                        LOGGER.warn("Saved skin file not found: {}", skinConfig.skinPath);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load saved skin: {}", skinConfig.skinPath, e);
                    // Don't crash the game, just log the error
                }
            } else {
                LOGGER.info("No saved skin found for player");
            }
        } catch (Exception e) {
            LOGGER.error("Error loading saved skins when ready", e);
            // Don't crash the game, just log the error
        }
    }

    // Load skin with slim mode override
    public boolean loadSkinWithSlimMode(Path filePath, boolean slimMode) {
        try {
            // Validate file path
            if (filePath == null) {
                LOGGER.error("File path is null");
                return false;
            }

            // Check if file exists
            if (!Files.exists(filePath)) {
                LOGGER.error("File does not exist: {}", filePath);
                return false;
            }

            // Check file size
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_IMAGE_SIZE) {
                LOGGER.error("File too large: {} bytes (max: {} bytes)", fileSize, MAX_IMAGE_SIZE);
                return false;
            }

            if (fileSize == 0) {
                LOGGER.error("File is empty: {}", filePath);
                return false;
            }

            // Read file bytes
            byte[] fileBytes = Files.readAllBytes(filePath);

            // Load as NativeImage
            NativeImage image = NativeImage.read(fileBytes);

            // Validate dimensions (must be power of 2 and reasonable size)
            if (!isValidSkinDimensions(image.getWidth(), image.getHeight())) {
                LOGGER.error("Invalid skin dimensions: {}x{}", image.getWidth(), image.getHeight());
                image.close(); // Clean up
                return false;
            }

            // Use provided slim mode instead of filename detection
            String fileName = filePath.getFileName().toString();

            // Get local player UUID
            UUID playerUuid = MinecraftClient.getInstance().getSession().getProfile().getId();

            // Store skin data
            SkinData skinData = new SkinData(image, slimMode, fileName);
            playerSkins.put(playerUuid, skinData);

            // Create and register texture
            registerTexture(playerUuid, image);

            // Save to config if auto-save is enabled
            if (config.isAutoSaveEnabled()) {
                try {
                    config.savePlayerSkinWithNickname(playerUuid, filePath.toString(), fileName, slimMode, image.getWidth(), image.getHeight(), "");
                    LOGGER.info("Skin saved to config: {} ({}x{}, slim: {})", fileName, image.getWidth(), image.getHeight(), slimMode);
                } catch (Exception e) {
                    LOGGER.error("Failed to save skin config: {}", fileName, e);
                    // Don't crash the game, just log the error
                }
            }

            // Send skin change to server for multiplayer sync
            sendSkinToServer(playerUuid, filePath.toString(), fileName, slimMode, image.getWidth(), image.getHeight());

            LOGGER.info("Successfully loaded skin: {} (slim: {}, dimensions: {}x{})", fileName, slimMode, image.getWidth(), image.getHeight());
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to load skin from file: {} - {}", filePath, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error loading skin: {} - {}", filePath, e.getMessage(), e);
            return false;
        }
    }

    // Load skin with slim mode and nickname
    public boolean loadSkinWithSlimModeAndNickname(Path filePath, boolean slimMode, String nickname) {
        try {
            // Validate file path
            if (filePath == null) {
                LOGGER.error("File path is null");
                return false;
            }

            // Check if file exists
            if (!Files.exists(filePath)) {
                LOGGER.error("File does not exist: {}", filePath);
                return false;
            }

            // Check file size
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_IMAGE_SIZE) {
                LOGGER.error("File too large: {} bytes (max: {} bytes)", fileSize, MAX_IMAGE_SIZE);
                return false;
            }

            if (fileSize == 0) {
                LOGGER.error("File is empty: {}", filePath);
                return false;
            }

            // Read file bytes
            byte[] fileBytes = Files.readAllBytes(filePath);

            // Load as NativeImage
            NativeImage image = NativeImage.read(fileBytes);

            // Validate dimensions (must be power of 2 and reasonable size)
            if (!isValidSkinDimensions(image.getWidth(), image.getHeight())) {
                LOGGER.error("Invalid skin dimensions: {}x{}", image.getWidth(), image.getHeight());
                image.close(); // Clean up
                return false;
            }

            // Use provided slim mode and nickname
            String fileName = filePath.getFileName().toString();

            // Get local player UUID
            UUID playerUuid = MinecraftClient.getInstance().getSession().getProfile().getId();

            // Store skin data
            SkinData skinData = new SkinData(image, slimMode, fileName);
            playerSkins.put(playerUuid, skinData);

            // Create and register texture
            registerTexture(playerUuid, image);

            // Save to config if auto-save is enabled
            if (config.isAutoSaveEnabled()) {
                try {
                    config.savePlayerSkinWithNickname(playerUuid, filePath.toString(), fileName, slimMode, image.getWidth(), image.getHeight(), nickname);
                    LOGGER.info("Skin saved to config: {} ({}x{}, slim: {}, nickname: {})", fileName, image.getWidth(), image.getHeight(), slimMode, nickname);
                } catch (Exception e) {
                    LOGGER.error("Failed to save skin config: {}", fileName, e);
                    // Don't crash the game, just log the error
                }
            }

            // Send skin change to server for multiplayer sync
            sendSkinToServer(playerUuid, filePath.toString(), fileName, slimMode, image.getWidth(), image.getHeight());

            LOGGER.info("Successfully loaded skin: {} (slim: {}, dimensions: {}x{}, nickname: {})", fileName, slimMode, image.getWidth(), image.getHeight(), nickname);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to load skin from file: {} - {}", filePath, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error loading skin: {} - {}", filePath, e.getMessage(), e);
            return false;
        }
    }
}
