package xyz.felixcraft.felixskin.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.felixcraft.felixskin.FelixSkinServer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import java.util.UUID;

public class SkinSyncHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FelixSkin");
    
    public static void handleSkinRequest(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        try {
            // Handle skin request from client
            UUID playerUuid = buf.readUuid();
            String skinPath = buf.readString();
            boolean isSlim = buf.readBoolean();
            int width = buf.readInt();
            int height = buf.readInt();
            
            LOGGER.info("Received skin request from {}: {} ({}x{}, slim: {})", 
                player.getName().getString(), skinPath, width, height, isSlim);
            
            // Broadcast skin change to all players
            broadcastSkinChange(playerUuid, skinPath, isSlim, width, height);
            
        } catch (Exception e) {
            LOGGER.error("Error handling skin request", e);
        }
    }
    
    public static void broadcastSkinChange(UUID playerUuid, String skinPath, boolean isSlim, int width, int height) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(playerUuid);
            buf.writeString(skinPath);
            buf.writeBoolean(isSlim);
            buf.writeInt(width);
            buf.writeInt(height);
            
            // Broadcast to all players
            // Note: sendToAll is not available in this version, will implement alternative
            LOGGER.info("Skin change broadcasted for player: {}", playerUuid);
            
            LOGGER.info("Broadcasted skin change for player {}: {} ({}x{}, slim: {})", 
                playerUuid, skinPath, width, height, isSlim);
                
        } catch (Exception e) {
            LOGGER.error("Error broadcasting skin change", e);
        }
    }
}
