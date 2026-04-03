package com.rinkuswap;

import com.rinkuswap.screen.RinkuSwapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.MaceItem;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/*
 * ╔══════════════════════════════════════════════════════════╗
 * ║               RINKU SWAP — How It Works                 ║
 * ║                                                          ║
 * ║  STUN SLAM MODE (Default — Fast):                       ║
 * ║  • Tum gir rahe ho (falling velocity detected)          ║
 * ║  • Left click dabao                                     ║
 * ║  • MOD: 0-tick mein mace slot pe switch karta hai       ║
 * ║  • Mace ka SLAM/BREACH damage fully register hota hai   ║
 * ║  • Attack ke baad INSTANTLY waapis original slot        ║
 * ║                                                          ║
 * ║  NORMAL MODE:                                           ║
 * ║  • Left click hold karo → mace slot                    ║
 * ║  • Choddo → waapis original slot                        ║
 * ║                                                          ║
 * ║  KEY: R → Settings screen                               ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Environment(EnvType.CLIENT)
public class RinkuSwapClient implements ClientModInitializer {

    private static KeyBinding openSettingsKey;

    // Swap state
    private static int     originalSlot  = -1;
    private static boolean swapped       = false;
    private static boolean wasAttacking  = false;

    // Stun slam tracking
    private static boolean slamPending   = false;   // swap hua, attack abhi hona baaki
    private static int     slamResetTick = 0;        // kitne ticks baad waapis jao
    private static Vec3d   lastVelocity  = Vec3d.ZERO;

    // Falling threshold — 1.21.11 mein mace slam ke liye yeh speed chahiye
    private static final double FALL_VELOCITY_THRESHOLD = -0.1;
    // Stun slam ke baad kitne ticks mein waapis? (1 tick = fastest safe)
    private static final int    SLAM_RETURN_TICKS = 1;

    @Override
    public void onInitializeClient() {
        RinkuSwapConfig.load();

        // R key → settings
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rinkuswap.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.rinkuswap"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        RinkuSwapMod.LOGGER.info("[Rinku Swap] Ready! R dabao settings ke liye. Bhai STUN SLAM time!");
    }

    private void tick(MinecraftClient client) {
        if (client.player == null) return;

        // Settings screen
        while (openSettingsKey.wasPressed()) {
            client.setScreen(new RinkuSwapScreen(null));
        }

        // Koi screen khuli hai → reset
        if (client.currentScreen != null) {
            doReset(client);
            wasAttacking = false;
            return;
        }

        // Feature off
        if (!RinkuSwapConfig.swapEnabled) {
            wasAttacking = false;
            return;
        }

        ClientPlayerEntity player = client.player;
        PlayerInventory    inv    = player.getInventory();
        boolean            isAtk  = client.options.attackKey.isPressed();
        int                maceSlot = RinkuSwapConfig.maceSlot;

        // ──────────────────────────────────────────────
        //  STUN SLAM MODE — fast fall swap
        // ──────────────────────────────────────────────
        if (RinkuSwapConfig.stunSlamMode) {

            Vec3d vel = player.getVelocity();
            boolean isFalling = vel.y < FALL_VELOCITY_THRESHOLD;

            // Slam return countdown
            if (slamPending && slamResetTick > 0) {
                slamResetTick--;
                if (slamResetTick == 0) {
                    // Waapis original slot
                    if (originalSlot != -1) {
                        inv.selectedSlot = originalSlot;
                        client.player.sendMessage(
                                Text.literal("§6[Rinku Swap] §cSTUN SLAM! §fWaapis slot " + (originalSlot + 1)),
                                true
                        );
                    }
                    slamPending  = false;
                    swapped      = false;
                    originalSlot = -1;
                }
            }

            // Left click + falling + not already swapped → FAST SWAP to mace
            if (isAtk && !wasAttacking && !swapped && !slamPending) {
                if (isFalling && inv.selectedSlot != maceSlot) {
                    // ⚡ INSTANT 0-tick swap to mace
                    originalSlot      = inv.selectedSlot;
                    inv.selectedSlot  = maceSlot;
                    swapped           = true;
                    slamPending       = true;
                    slamResetTick     = SLAM_RETURN_TICKS;

                    client.player.sendMessage(
                            Text.literal("§6[Rinku Swap] §aMACE SWAP! §7Slam incoming..."),
                            true
                    );
                } else if (!isFalling && inv.selectedSlot != maceSlot) {
                    // Normal click, not falling — still swap (hold mode)
                    originalSlot     = inv.selectedSlot;
                    inv.selectedSlot = maceSlot;
                    swapped          = true;
                }
            }

            // Click released in non-falling normal swap
            if (!isAtk && swapped && !slamPending) {
                inv.selectedSlot = originalSlot;
                client.player.sendMessage(
                        Text.literal("§6[Rinku Swap] §fWaapis slot " + (originalSlot + 1)),
                        true
                );
                swapped      = false;
                originalSlot = -1;
            }

            // Player ne khud scroll kiya → graceful reset
            if (swapped && !slamPending && inv.selectedSlot != maceSlot) {
                swapped      = false;
                originalSlot = -1;
            }

            lastVelocity = vel;

        // ──────────────────────────────────────────────
        //  NORMAL MODE — hold to swap
        // ──────────────────────────────────────────────
        } else {

            if (isAtk && !wasAttacking && !swapped) {
                if (inv.selectedSlot != maceSlot) {
                    originalSlot     = inv.selectedSlot;
                    inv.selectedSlot = maceSlot;
                    swapped          = true;
                    client.player.sendMessage(
                            Text.literal("§6[Rinku Swap] §aSlot " + (maceSlot + 1) + " active"),
                            true
                    );
                }
            }

            if (!isAtk && swapped) {
                inv.selectedSlot = originalSlot;
                client.player.sendMessage(
                        Text.literal("§6[Rinku Swap] §fWaapis slot " + (originalSlot + 1)),
                        true
                );
                swapped      = false;
                originalSlot = -1;
            }

            if (swapped && inv.selectedSlot != maceSlot) {
                swapped = false; originalSlot = -1;
            }
        }

        wasAttacking = isAtk;
    }

    private void doReset(MinecraftClient client) {
        if ((swapped || slamPending) && originalSlot != -1 && client.player != null) {
            client.player.getInventory().selectedSlot = originalSlot;
        }
        swapped      = false;
        slamPending  = false;
        originalSlot = -1;
        slamResetTick = 0;
    }
}
