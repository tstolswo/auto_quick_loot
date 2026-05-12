/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  com.robertx22.library_of_exile.main.MyPacket
 *  com.robertx22.library_of_exile.main.Packets
 *  com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket
 *  com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket$Mode
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.components.Renderable
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterKeyMappingsEvent
 *  net.minecraftforge.client.event.ScreenEvent$Render$Post
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.lwjgl.glfw.GLFW
 */
package com.example.autoquickloot;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod(value="autoquickloot")
public class AutoQuickLoot {
    private static final WeakHashMap<Screen, Boolean> triggered = new WeakHashMap();
    public static KeyMapping quickLootKey;

    @Mod.EventBusSubscriber(modid="autoquickloot", value={Dist.CLIENT})
    public static class ClientEvents {
        @SubscribeEvent
        public static void onScreenRender(ScreenEvent.Render.Post event) {
            int keyCode;
            Screen screen = event.getScreen();
            if (screen == null) {
                return;
            }
            long window = Minecraft.getInstance().getWindow().getWindow();
            int n = keyCode = quickLootKey != null ? quickLootKey.getKey().getValue() : 341;
            if (GLFW.glfwGetKey((long)window, (int)keyCode) != 1) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            HitResult hit = mc.hitResult;
            if (!(hit instanceof BlockHitResult)) {
                return;
            }
            BlockHitResult blockHit = (BlockHitResult)hit;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();
            String blockId = BuiltInRegistries.BLOCK.getKey((Block)block).toString();
            if (!blockId.equals("lootr:lootr_chest") && !blockId.equals("lootr:lootr_barrel") && !blockId.equals("lootr:lootr_shulker")) {
                return;
            }
            if (triggered.getOrDefault(screen, false).booleanValue()) {
                return;
            }
            boolean foundButton = false;
            if (screen.renderables != null) {
                for (Renderable renderable : screen.renderables) {
                    if (!renderable.getClass().getName().equals("com.robertx22.mine_and_slash.capability.player.container.BackpackQuickLootButton")) continue;
                    foundButton = true;
                    break;
                }
            }
            if (!foundButton) {
                return;
            }
            Packets.sendToServer((MyPacket)new BackPackLootMenuPacket(BackPackLootMenuPacket.Mode.DROP));
            Minecraft.getInstance().setScreen(null);
            triggered.put(screen, true);
        }
    }

    @Mod.EventBusSubscriber(modid="autoquickloot", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            quickLootKey = new KeyMapping("key.autoquickloot.activate", InputConstants.Type.KEYSYM, 341, "key.categories.autoquickloot");
            event.register(quickLootKey);
        }
    }
}

