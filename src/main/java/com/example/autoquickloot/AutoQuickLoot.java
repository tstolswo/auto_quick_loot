package com.example.autoquickloot;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket;

import java.util.WeakHashMap;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod(value = "autoquickloot")
public class AutoQuickLoot {

    // -------------------------------------------------------------------------
    // HUD label constants — public so HudPositionScreen can mirror them exactly
    // -------------------------------------------------------------------------
    public static final String HUD_TEXT    = "Auto Quick Loot is enabled";
    public static final int    HUD_COLOR   = 0x55FF55; // Minecraft §a green
    public static final int    HUD_PADDING = 2;

    // -------------------------------------------------------------------------
    // Shared state
    // -------------------------------------------------------------------------
    private static final WeakHashMap<Screen, Boolean> triggered = new WeakHashMap<>();

    /** Original hold-key: trigger loot while held on a screen. */
    public static KeyMapping quickLootKey;

    /** Toggle key: flip auto-loot on/off. */
    public static KeyMapping toggleKey;

    /** Edit HUD position key: open the drag-to-reposition screen. */
    public static KeyMapping hudPositionKey;

    /** Whether toggle mode is currently active. */
    public static volatile boolean toggleEnabled = false;

    // -------------------------------------------------------------------------
    // Constructor — register config
    // -------------------------------------------------------------------------
    public AutoQuickLoot() {
        Config.register();
        toggleEnabled = Config.TOGGLE_ENABLED_BY_DEFAULT.get();
    }

    // =========================================================================
    // CLIENT EVENT BUS
    // =========================================================================
    @Mod.EventBusSubscriber(modid = "autoquickloot", value = {Dist.CLIENT})
    public static class ClientEvents {

        // ---------------------------------------------------------------------
        // Key input
        // ---------------------------------------------------------------------
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS) return;

            int key = event.getKey();

            // Toggle auto-loot on/off
            if (toggleKey != null && toggleKey.getKey().getValue() == key) {
                toggleEnabled = !toggleEnabled;
            }

            // Open HUD position editor
            if (hudPositionKey != null && hudPositionKey.getKey().getValue() == key) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) { // only open when no other screen is up
                    mc.setScreen(new HudPositionScreen());
                }
            }
        }

        // ---------------------------------------------------------------------
        // Screen render — perform the loot action
        // ---------------------------------------------------------------------
        @SubscribeEvent
        public static void onScreenRender(ScreenEvent.Render.Post event) {
            Screen screen = event.getScreen();
            if (screen == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            // Act if toggle is on OR the hold-key is currently pressed
            boolean holdKeyPressed = false;
            if (quickLootKey != null) {
                long window  = mc.getWindow().getWindow();
                int  keyCode = quickLootKey.getKey().getValue();
                holdKeyPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            }
            if (!toggleEnabled && !holdKeyPressed) return;

            if (triggered.getOrDefault(screen, false)) return;

            // Must be looking at a supported Lootr container
            HitResult hit = mc.hitResult;
            if (!(hit instanceof BlockHitResult blockHit)) return;

            BlockPos   pos     = blockHit.getBlockPos();
            BlockState state   = mc.level.getBlockState(pos);
            Block      block   = state.getBlock();
            String     blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

            if (!blockId.equals("lootr:lootr_chest")
                    && !blockId.equals("lootr:lootr_barrel")
                    && !blockId.equals("lootr:lootr_shulker")) {
                return;
            }

            // Verify the MnS quick-loot button is present
            boolean foundButton = false;
            if (screen.renderables != null) {
                for (Renderable renderable : screen.renderables) {
                    if (renderable.getClass().getName()
                            .equals("com.robertx22.mine_and_slash.capability.player.container.BackpackQuickLootButton")) {
                        foundButton = true;
                        break;
                    }
                }
            }
            if (!foundButton) return;

            Packets.sendToServer((MyPacket) new BackPackLootMenuPacket(BackPackLootMenuPacket.Mode.DROP));
            Minecraft.getInstance().setScreen(null);
            triggered.put(screen, true);
        }

        // ---------------------------------------------------------------------
        // HUD overlay
        // ---------------------------------------------------------------------
        @SubscribeEvent
        public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
            if (!toggleEnabled) return;
            if (!Config.HUD_ENABLED.get()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return; // hide while any GUI is open

            GuiGraphics graphics  = event.getGuiGraphics();
            Font        font      = mc.font;
            int         scrWidth  = mc.getWindow().getGuiScaledWidth();
            int         scrHeight = mc.getWindow().getGuiScaledHeight();

            int textWidth  = font.width(HUD_TEXT);
            int textHeight = font.lineHeight;

            // Use saved position, or fall back to bottom-right default
            int cfgX = Config.HUD_X.get();
            int cfgY = Config.HUD_Y.get();
            int x = cfgX >= 0 ? cfgX : scrWidth  - textWidth  - HUD_PADDING - 10;
            int y = cfgY >= 0 ? cfgY : scrHeight - textHeight - HUD_PADDING - 10;

            // Dark background pill
            graphics.fill(
                x - HUD_PADDING,
                y - HUD_PADDING,
                x + textWidth  + HUD_PADDING,
                y + textHeight + HUD_PADDING,
                0x88000000
            );

            // Green label
            graphics.drawString(font, HUD_TEXT, x, y, HUD_COLOR, false);
        }
    }

    // =========================================================================
    // MOD EVENT BUS — registration
    // =========================================================================
    @Mod.EventBusSubscriber(modid = "autoquickloot", value = {Dist.CLIENT}, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            quickLootKey = new KeyMapping(
                    "key.autoquickloot.activate",
                    InputConstants.Type.KEYSYM,
                    341, // GLFW_KEY_LEFT_CONTROL
                    "key.categories.autoquickloot"
            );

            toggleKey = new KeyMapping(
                    "key.autoquickloot.toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.autoquickloot"
            );

            hudPositionKey = new KeyMapping(
                    "key.autoquickloot.editHudPosition",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.autoquickloot"
            );

            if (quickLootKey  != null) event.register(quickLootKey);
            if (toggleKey     != null) event.register(toggleKey);
            if (hudPositionKey != null) event.register(hudPositionKey);
        }
    }
}
