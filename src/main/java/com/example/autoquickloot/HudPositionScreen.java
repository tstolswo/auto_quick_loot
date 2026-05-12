package com.example.autoquickloot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A simple drag-to-position screen for the Auto Quick Loot HUD notice.
 * Opened via the "Edit HUD Position" keybinding. Saves the chosen X/Y
 * back to the Forge client config on close.
 */
public class HudPositionScreen extends Screen {

    // Mirror constants from the main class so both render identically
    private static final String HUD_TEXT    = AutoQuickLoot.HUD_TEXT;
    private static final int    HUD_COLOR   = AutoQuickLoot.HUD_COLOR;
    private static final int    HUD_PADDING = AutoQuickLoot.HUD_PADDING;

    private int  noticeX, noticeY;
    private boolean dragging    = false;
    private int  dragOffsetX, dragOffsetY;

    public HudPositionScreen() {
        super(Component.literal("Edit HUD Position"));
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        int cfgX = Config.HUD_X.get();
        int cfgY = Config.HUD_Y.get();
        if (cfgX >= 0 && cfgY >= 0) {
            noticeX = cfgX;
            noticeY = cfgY;
        } else {
            resetToDefault();
        }

        // Done button
        this.addRenderableWidget(
            Button.builder(Component.literal("Done"), btn -> this.onClose())
                  .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                  .build());

        // Reset button
        this.addRenderableWidget(
            Button.builder(Component.literal("Reset to Default"), btn -> resetToDefault())
                  .bounds(this.width / 2 - 65, this.height - 56, 130, 20)
                  .build());
    }

    private void resetToDefault() {
        int textWidth  = this.font.width(HUD_TEXT)  + HUD_PADDING * 2;
        int textHeight = this.font.lineHeight        + HUD_PADDING * 2;
        noticeX = this.width  - textWidth  - 10;
        noticeY = this.height - textHeight - 10;
    }

    @Override
    public void onClose() {
        // Persist the chosen position into the Forge config
        Config.HUD_X.set(noticeX);
        Config.HUD_Y.set(noticeY);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // keep the game running while repositioning
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Instruction hint at top
        graphics.drawCenteredString(this.font,
            Component.literal("§7Drag the notice to reposition it, then press Done or Esc."),
            this.width / 2, 12, 0xFFFFFF);

        // --- Draw the draggable notice ---
        int textWidth  = this.font.width(HUD_TEXT);
        int textHeight = this.font.lineHeight;
        boolean active = dragging || isOverNotice(mouseX, mouseY);

        // Background pill — slightly brighter when hovered/dragged
        graphics.fill(
            noticeX,
            noticeY,
            noticeX + textWidth  + HUD_PADDING * 2,
            noticeY + textHeight + HUD_PADDING * 2,
            active ? 0xCC333333 : 0x88000000
        );

        // Highlight border when interactive
        if (active) {
            int right  = noticeX + textWidth  + HUD_PADDING * 2;
            int bottom = noticeY + textHeight + HUD_PADDING * 2;
            graphics.hLine(noticeX, right,  noticeY,       0xAAFFFFFF);
            graphics.hLine(noticeX, right,  bottom - 1,    0xAAFFFFFF);
            graphics.vLine(noticeX,         noticeY, bottom, 0xAAFFFFFF);
            graphics.vLine(right - 1,       noticeY, bottom, 0xAAFFFFFF);
        }

        // Label
        graphics.drawString(this.font, HUD_TEXT,
            noticeX + HUD_PADDING, noticeY + HUD_PADDING,
            HUD_COLOR, false);

        // Render buttons on top
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

    private boolean isOverNotice(int mx, int my) {
        int w = this.font.width(HUD_TEXT) + HUD_PADDING * 2;
        int h = this.font.lineHeight      + HUD_PADDING * 2;
        return mx >= noticeX && mx <= noticeX + w
            && my >= noticeY && my <= noticeY + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverNotice((int) mouseX, (int) mouseY)) {
            dragging    = true;
            dragOffsetX = (int) mouseX - noticeX;
            dragOffsetY = (int) mouseY - noticeY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (dragging && button == 0) {
            int w = this.font.width(HUD_TEXT) + HUD_PADDING * 2;
            int h = this.font.lineHeight      + HUD_PADDING * 2;
            noticeX = (int) Math.max(0, Math.min(mouseX - dragOffsetX, this.width  - w));
            noticeY = (int) Math.max(0, Math.min(mouseY - dragOffsetY, this.height - h));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
