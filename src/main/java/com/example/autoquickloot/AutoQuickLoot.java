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
    // Shared state
    // -------------------------------------------------------------------------
    private static final WeakHashMap<Screen, Boolean> triggered = new WeakHashMap<>();

    /** Keybinding that triggers loot when held while a loot screen is open (original behaviour). */
    public static KeyMapping quickLootKey;

    /** Keybinding that toggles the automatic loot mode on/off. */
    public static KeyMapping toggleKey;

    /** Whether toggle mode is currently active. */
    public static volatile boolean toggleEnabled = false;

    // -------------------------------------------------------------------------
    // HUD label
    // -------------------------------------------------------------------------
    private static final String HUD_TEXT    = "Auto Quick Loot is enabled";
    private static final int    HUD_COLOR   = 0x55FF55; // Minecraft §a green
    private static final int    HUD_PADDING = 2;        // extra padding around the text box

    // -------------------------------------------------------------------------
    // Constructor — register config
    // -------------------------------------------------------------------------
    public AutoQuickLoot() {
        Config.register();

        // Honour the "enabled by default" config option
        toggleEnabled = Config.TOGGLE_ENABLED_BY_DEFAULT.get();
    }

    // =========================================================================
    // CLIENT EVENT BUS (game events)
    // =========================================================================
    @Mod.EventBusSubscriber(modid = "autoquickloot", value = {Dist.CLIENT})
    public static class ClientEvents {

        // ---------------------------------------------------------------------
        // Key input — toggle on/off
        // ---------------------------------------------------------------------
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (toggleKey == null) return;

            // Only react on key press (action == 1), not hold or release
            if (event.getAction() != GLFW.GLFW_PRESS) return;

            if (toggleKey.getKey().getValue() == event.getKey()) {
                toggleEnabled = !toggleEnabled;
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

            // Determine whether we should act:
            //   • toggle mode is active, OR
            //   • the original hold-key is currently held down
            boolean holdKeyPressed = false;
            if (quickLootKey != null) {
                long window = mc.getWindow().getWindow();
                int keyCode = quickLootKey.getKey().getValue();
                holdKeyPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            }

            if (!toggleEnabled && !holdKeyPressed) return;

            // Already processed this screen instance?
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

            // Verify the MnS quick-loot button is present on this screen
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

            // Fire the loot packet and close the screen
            Packets.sendToServer((MyPacket) new BackPackLootMenuPacket(BackPackLootMenuPacket.Mode.DROP));
            Minecraft.getInstance().setScreen(null);
            triggered.put(screen, true);
        }

        // ---------------------------------------------------------------------
        // HUD overlay — "Auto Quick Loot is enabled" notice
        // ---------------------------------------------------------------------
        @SubscribeEvent
        public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
            // Only draw after the chat overlay so we sit on top of most UI
            if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;

            if (!toggleEnabled) return;
            if (!Config.HUD_ENABLED.get()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return; // don't draw while a GUI screen is open

            GuiGraphics graphics  = event.getGuiGraphics();
            Font        font      = mc.font;
            int         scrWidth  = mc.getWindow().getGuiScaledWidth();
            int         scrHeight = mc.getWindow().getGuiScaledHeight();

            int textWidth  = font.width(HUD_TEXT);
            int textHeight = font.lineHeight;
            int offsetX    = Config.HUD_OFFSET_X.get();
            int offsetY    = Config.HUD_OFFSET_Y.get();

            // Resolve pixel position from anchor
            int x, y;
            switch (Config.getHudAnchor()) {
                case TOP_LEFT:
                    x = offsetX;
                    y = offsetY;
                    break;
                case TOP_RIGHT:
                    x = scrWidth - textWidth - offsetX;
                    y = offsetY;
                    break;
                case BOTTOM_LEFT:
                    x = offsetX;
                    y = scrHeight - textHeight - offsetY;
                    break;
                case BOTTOM_RIGHT:
                default:
                    x = scrWidth  - textWidth  - offsetX;
                    y = scrHeight - textHeight - offsetY;
                    break;
            }

            // Semi-transparent dark background pill for legibility
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
    // MOD EVENT BUS (registration)
    // =========================================================================
    @Mod.EventBusSubscriber(modid = "autoquickloot", value = {Dist.CLIENT}, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            // Original hold-to-loot key (default: Left Ctrl)
            quickLootKey = new KeyMapping(
                    "key.autoquickloot.activate",
                    InputConstants.Type.KEYSYM,
                    341, // GLFW_KEY_LEFT_CONTROL
                    "key.categories.autoquickloot"
            );

            // New toggle key (default: unbound — player assigns in Controls)
            toggleKey = new KeyMapping(
                    "key.autoquickloot.toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.autoquickloot"
            );

            if (quickLootKey != null) event.register(quickLootKey);
            if (toggleKey    != null) event.register(toggleKey);
        }
    }
}
