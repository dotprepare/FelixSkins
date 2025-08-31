package xyz.felixcraft.felixskin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.PointerBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.felixcraft.felixskin.skin.SkinManager;
import xyz.felixcraft.felixskin.gui.SkinManagerScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FelixSkinClient implements ClientModInitializer {
    public static final String MOD_ID = "felixskin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static KeyBinding openSkinManagerKey;
    private static SkinManager skinManager;
    private static boolean dragAndDropSetup = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing FelixSkin mod v1.2.2...");

        // Check for mod compatibility
        checkModCompatibility();

        // Initialize skin manager
        skinManager = new SkinManager();

        // Register key binding
        openSkinManagerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.felixskin.open_skin_manager",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.felixskin.general"
        ));

        // Register tick event to check for key press and setup drag-and-drop if needed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Setup drag-and-drop if not done yet and window is available
            if (!dragAndDropSetup && client.getWindow() != null) {
                setupDragAndDrop();
                dragAndDropSetup = true;

                // Load saved skins now that client is ready
                skinManager.loadSavedSkinsWhenReady();
            }

            if (openSkinManagerKey.wasPressed()) {
                client.setScreen(new SkinManagerScreen(Text.translatable("gui.felixskin.title")));
            }
        });

        LOGGER.info("FelixSkin mod initialized successfully!");
    }

    private void setupDragAndDrop() {
        // Check if window is available (might be null during early initialization)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            LOGGER.warn("Window not available yet, drag-and-drop setup will be delayed");
            return;
        }

        // Set up GLFW drop callback for file drag-and-drop
        GLFW.glfwSetDropCallback(client.getWindow().getHandle(), (window, count, names) -> {
            if (count > 0) {
                // Convert pointer buffer to string array
                PointerBuffer nameBuffer = PointerBuffer.create(names, count);

                // Process dropped files
                for (int i = 0; i < count; i++) {
                    String fileName = nameBuffer.getStringUTF8(i);
                    if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
                        Path filePath = Paths.get(fileName);
                        LOGGER.info("PNG file dropped: {}", fileName);

                        // Load the skin on the main thread with slim mode setting
                        boolean currentSlimMode = skinManager.isSlimMode();
                        client.execute(() -> {
                            try {
                                boolean success = skinManager.loadSkinWithSlimMode(filePath, currentSlimMode);
                                if (success) {
                                    LOGGER.info("Successfully loaded dropped skin: {} (slim: {})", fileName, currentSlimMode);
                                } else {
                                    LOGGER.error("Failed to load dropped skin: {}", fileName);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Exception while loading dropped skin: {}", fileName, e);
                            }
                        });
                        break; // Only process the first PNG file
                    }
                }
            }
        });

        LOGGER.info("Drag-and-drop callback setup completed");
    }
    
    private void checkModCompatibility() {
        try {
            FabricLoader loader = FabricLoader.getInstance();

            // Check for known skin-related mods that might conflict
            List<String> potentiallyConflictingMods = List.of(
                "skinlayers", "skins", "customskinloader", "skinrestorer"
            );

            boolean hasConflicts = false;
            for (String modId : potentiallyConflictingMods) {
                if (loader.isModLoaded(modId)) {
                    LOGGER.warn("Detected potentially conflicting mod: {}", modId);
                    LOGGER.warn("FelixSkin may not work properly with this mod. Consider disabling one of them.");
                    hasConflicts = true;
                }
            }

            if (!hasConflicts) {
                LOGGER.info("No conflicting mods detected - FelixSkin should work properly!");
            }

            // Log compatibility information
            LOGGER.info("FelixSkin v1.2.2 compatibility check completed");
            LOGGER.info("Compatible with Minecraft 1.20.1 and Fabric Loader 0.16.12+");

        } catch (Exception e) {
            LOGGER.warn("Could not perform compatibility check", e);
        }
    }

    public static SkinManager getSkinManager() {
        return skinManager;
    }
}
