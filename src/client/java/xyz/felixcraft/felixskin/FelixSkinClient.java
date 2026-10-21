package xyz.felixcraft.felixskin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.felixcraft.felixskin.skin.SkinManager;
import xyz.felixcraft.felixskin.gui.SkinManagerScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.List;

public class FelixSkinClient implements ClientModInitializer {
    public static final String MOD_ID = "felixskin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static KeyBinding openSkinManagerKey;
    private static SkinManager skinManager;
    private static boolean savedSkinsLoaded = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing FelixSkin mod v1.2.3...");

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

        // Register tick event to check for key press and load saved skins once the client is ready.
        // Drag-and-drop is handled by SkinManagerScreen#filesDragged, which vanilla's own GLFW
        // drop callback forwards to; replacing that callback would break resource pack dragging.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!savedSkinsLoaded) {
                savedSkinsLoaded = true;
                skinManager.loadSavedSkinsWhenReady();
            }

            if (openSkinManagerKey.wasPressed()) {
                client.setScreen(new SkinManagerScreen(Text.translatable("gui.felixskin.title")));
            }
        });

        LOGGER.info("FelixSkin mod initialized successfully!");
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
            LOGGER.info("FelixSkin v1.2.3 compatibility check completed");
            LOGGER.info("Compatible with Minecraft 1.20.1 and Fabric Loader 0.16.12+");

        } catch (Exception e) {
            LOGGER.warn("Could not perform compatibility check", e);
        }
    }

    public static SkinManager getSkinManager() {
        return skinManager;
    }
}
