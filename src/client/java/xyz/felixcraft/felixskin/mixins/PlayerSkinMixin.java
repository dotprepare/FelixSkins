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

    // Note: isSlim injection removed due to method signature incompatibility in MC 1.20.1
    // The skin texture injection should be sufficient for basic functionality
}
