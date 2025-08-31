package xyz.felixcraft.felixskin;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.felixcraft.felixskin.network.SkinSyncHandler;

public class FelixSkinServer implements DedicatedServerModInitializer {
    public static final String MOD_ID = "felixskin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Network channels for skin synchronization
    public static final Identifier SKIN_SYNC_CHANNEL = new Identifier(MOD_ID, "skin_sync");
    public static final Identifier SKIN_REQUEST_CHANNEL = new Identifier(MOD_ID, "skin_request");
    
    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing FelixSkin server mod...");
        
        // Register network handlers
        ServerPlayNetworking.registerGlobalReceiver(SKIN_REQUEST_CHANNEL, SkinSyncHandler::handleSkinRequest);
        
        LOGGER.info("FelixSkin server mod initialized successfully!");
    }
}

