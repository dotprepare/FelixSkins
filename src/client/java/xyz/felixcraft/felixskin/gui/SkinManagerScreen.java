package xyz.felixcraft.felixskin.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.felixcraft.felixskin.FelixSkinClient;
import xyz.felixcraft.felixskin.skin.SkinManager;

import java.nio.file.Path;

public class SkinManagerScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("FelixSkin");
    private static final int DROP_ZONE_WIDTH = 200;
    private static final int DROP_ZONE_HEIGHT = 150;

    private final SkinManager skinManager;
    private Path selectedFile = null;
    private boolean skinLoaded = false;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private CheckboxWidget slimModeCheckbox;

    public SkinManagerScreen(Text title) {
        super(title);
        this.skinManager = FelixSkinClient.getSkinManager();
    }

    @Override
    protected void init() {
        super.init();

        // Center the screen
        int centerX = width / 2;
        int centerY = height / 2;

        // Add title
        this.addDrawableChild(new TextWidget(
            centerX - 80, centerY - 70, 160, 20,
            Text.translatable("gui.felixskin.title").styled(style -> style.withBold(true)),
            textRenderer
        ));

        // Add drag instruction
        this.addDrawableChild(new TextWidget(
            centerX - 100, centerY - 40, 200, 20,
            Text.translatable("gui.felixskin.drag_instruction"),
            textRenderer
        ));

        // Add slim mode checkbox
        slimModeCheckbox = new CheckboxWidget(
            centerX - 60, centerY - 10, 120, 20,
            Text.translatable("gui.felixskin.slim_mode"),
            skinManager.isSlimMode()
        );
        this.addDrawableChild(slimModeCheckbox);

        // Add clear button
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.felixskin.clear"),
            button -> clearSkin()
        ).dimensions(centerX - 50, centerY + 20, 100, 20).build());
    }

    private void clearSkin() {
        try {
            if (client == null || client.getSession() == null || client.getSession().getProfile() == null) {
                setStatus("gui.felixskin.skin_clear_failed", 0xFF5555);
                LOGGER.error("Client session not available for skin clearing");
                return;
            }

            // Get local player UUID
            java.util.UUID playerUuid = client.getSession().getProfile().getId();

            // Clear skin
            skinManager.clearPlayerSkin(playerUuid);

            // Reset state
            selectedFile = null;
            skinLoaded = false;

            setStatus("gui.felixskin.skin_cleared", 0xFFFF55);
            LOGGER.info("Skin cleared for player: {}", playerUuid);
        } catch (Exception e) {
            LOGGER.error("Error clearing skin", e);
            setStatus("gui.felixskin.skin_clear_failed", 0xFF5555);
        }
    }

    private void setStatus(String key, int color) {
        statusMessage = Text.translatable(key).getString();
        statusColor = color;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;

        // Credits at bottom
        int creditsY = height - 40;
        context.drawTextWithShadow(textRenderer, Text.literal("Made by Frame121"), centerX - textRenderer.getWidth("Made by Frame121") / 2, creditsY, 0xFFFFFF);

        // Clickable link
        Text linkText = Text.literal("https://felixcraft.xyz")
            .styled(style -> style.withColor(0x55AAFF).withUnderline(true));
        context.drawTextWithShadow(textRenderer, linkText, centerX - textRenderer.getWidth(linkText) / 2, creditsY + 12, 0x55AAFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle slim mode checkbox click
        if (slimModeCheckbox != null && slimModeCheckbox.isMouseOver(mouseX, mouseY)) {
            boolean newState = !slimModeCheckbox.isChecked();
            slimModeCheckbox.onPress();
            skinManager.setSlimMode(newState);
            LOGGER.info("Slim mode toggled: {}", newState ? "ON (Alex/Slim)" : "OFF (Steve/Wide)");
            return true;
        }

        // Check if credits link was clicked
        int centerX = width / 2;
        int creditsY = height - 28;

        if (mouseX >= centerX - 100 && mouseX <= centerX + 100 &&
            mouseY >= creditsY && mouseY <= creditsY + 12) {
            // Open link in browser
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://felixcraft.xyz"));
            } catch (Exception e) {
                LOGGER.error("Failed to open link", e);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
