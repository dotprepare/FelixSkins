package xyz.felixcraft.felixskin.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.felixcraft.felixskin.FelixSkinClient;
import xyz.felixcraft.felixskin.config.FelixSkinConfig;

import java.util.UUID;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerNameMixin {

    @Inject(method = "method_3167", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        UUID playerUuid = player.getUuid();

        // Check if this is the local player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null && client.getSession().getProfile() != null) {
            UUID localPlayerUuid = client.getSession().getProfile().getId();
            if (playerUuid.equals(localPlayerUuid)) {
                FelixSkinConfig config = FelixSkinClient.getSkinManager().getConfig();
                String nickname = config.getPlayerNickname(playerUuid);

                if (nickname != null && !nickname.trim().isEmpty()) {
                    cir.setReturnValue(Text.literal(nickname));
                    return;
                }
            }
        }
    }

    @Inject(method = "method_3166", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetName(CallbackInfoReturnable<Text> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        UUID playerUuid = player.getUuid();

        // Check if this is the local player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null && client.getSession().getProfile() != null) {
            UUID localPlayerUuid = client.getSession().getProfile().getId();
            if (playerUuid.equals(localPlayerUuid)) {
                FelixSkinConfig config = FelixSkinClient.getSkinManager().getConfig();
                String nickname = config.getPlayerNickname(playerUuid);

                if (nickname != null && !nickname.trim().isEmpty()) {
                    cir.setReturnValue(Text.literal(nickname));
                    return;
                }
            }
        }
    }
}