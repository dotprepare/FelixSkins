package xyz.felixcraft.felixskin.mixins;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.felixcraft.felixskin.FelixSkinClient;
import xyz.felixcraft.felixskin.skin.SkinManager;

import java.util.UUID;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {
    
    // Inject at the beginning of getSkinTexture method (using obfuscated name for Minecraft 1.20.1)
    @Inject(method = "method_3117", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetSkinTexture(CallbackInfoReturnable<Identifier> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        UUID playerUuid = player.getUuid();

        // Apply to all players (local and remote)
        SkinManager skinManager = FelixSkinClient.getSkinManager();

        if (skinManager.hasCustomSkin(playerUuid)) {
            Identifier customTexture = skinManager.getPlayerTexture(playerUuid);
            if (customTexture != null) {
                cir.setReturnValue(customTexture);
                return;
            }
        }
    }

    // Inject at the beginning of getModel method so the slim setting picks the Alex/Steve arm model
    @Inject(method = "method_3121", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetModel(CallbackInfoReturnable<String> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        SkinManager.SkinData skinData = FelixSkinClient.getSkinManager().getPlayerSkin(player.getUuid());

        if (skinData != null && textureIsRegistered(player.getUuid())) {
            cir.setReturnValue(skinData.isSlim() ? "slim" : "default");
        }
    }

    private static boolean textureIsRegistered(UUID playerUuid) {
        return FelixSkinClient.getSkinManager().getPlayerTexture(playerUuid) != null;
    }
}
