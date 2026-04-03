package me.rinku.rinkuswap;

import me.rinku.rinkuswap.screen.RinkuSwapScreen;
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
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * ╔══════════════════════════════════════════════════════════╗
 * ║               RINKU SWAP — Main Logic                   ║
 * ║                                                          ║
 * ║  Based on ATRSwap by zqilyt — rewritten for 1.21.11     ║
 * ║  New feature: ADJUSTABLE SWAP SPEED (0.05s → 1.00s)    ║
 * ║                                                          ║
 * ║  HOW IT WORKS:                                          ║
 * ║  1. U key dabao → original slot + swapping slot set karo║
 * ║  2. R key dabao → Settings screen (speed change karo)   ║
 * ║  3. Kisi enemy pe left click karo                       ║
 * ║  4. Mod INSTANTLY swapping slot pe jaata hai            ║
 * ║  5. swapSpeedTicks ke baad WAPAS original slot          ║
 * ║                                                          ║
 * ║  Speed Examples:                                        ║
 * ║   1 tick  = 0.05s = FASTEST (stun slam ke liye best)   ║
 * ║   4 ticks = 0.20s = Fast                               ║
 * ║  10 ticks = 0.50s = Medium                             ║
 * ║  20 ticks = 1.00s = Slow                               ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Environment(EnvType.CLIENT)
public class RinkuSwapClient implements ClientModInitializer {

    public static final String MOD_ID = "rinkuswap";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    // ── Keybinds ──
    private static KeyBinding configureKey;   // U → slots configure karo (chat mein)
    private static KeyBinding settingsKey;    // R → settings screen

    // ── State (ATRSwap logic se liya, speed feature add kiya) ──
    private enum ConfigState { NONE, AWAITING_ORIGINAL_SLOT, AWAITING_SWAPPING_SLOT }
    private ConfigState configState        = ConfigState.NONE;
    private boolean     isCurrentlySwapped = false;
    private int         ticksRemaining     = 0;   // kitne ticks baad wapas jao
    private int         slotToReturnTo     = 0;

    @Override
    public void onInitializeClient() {
        RinkuSwapConfig.load();

        // U → slot configure (chat se)
        configureKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rinkuswap.configure",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.rinkuswap"
        ));

        // R → settings screen (speed + slots)
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rinkuswap.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.rinkuswap"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        LOGGER.info("[Rinku Swap] Ready! U = slots set karo | R = settings screen");
    }

    private void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        // Player nahi hai → sab reset
        if (player == null) {
            if (configState != ConfigState.NONE) {
                configState = ConfigState.NONE;
                LOGGER.info("[Rinku Swap] Player left world, config reset.");
            }
            return;
        }

        // ── Settings screen open ──
        while (settingsKey.wasPressed()) {
            client.setScreen(new RinkuSwapScreen(null));
        }

        // ── Configure key (U) → slot setup via chat ──
        while (configureKey.wasPressed()) {
            if (!RinkuSwapConfig.enabled) {
                player.sendMessage(Text.literal("§6[Rinku Swap] §cFeature band hai! R dabao settings ke liye."), false);
                return;
            }
            configState = ConfigState.AWAITING_ORIGINAL_SLOT;
            player.sendMessage(Text.literal(
                    "§6[Rinku Swap] §fTera §eMAIN slot §fkaunsa hai? §71-9 key dabao."), false);
        }

        // ── Numeric key input during config ──
        if (configState != ConfigState.NONE) {
            handleConfigInput(client, player);
            return;
        }

        // ── Feature off ──
        if (!RinkuSwapConfig.enabled) return;

        // ── Koi GUI khuli hai → skip ──
        if (client.currentScreen != null) return;

        PlayerInventory inv = player.getInventory();

        // ── Swap wapas countdown ──
        if (isCurrentlySwapped) {
            if (ticksRemaining > 0) {
                ticksRemaining--;
            }
            if (ticksRemaining <= 0) {
                // Wapas original slot pe jao
                inv.selectedSlot = slotToReturnTo;
                isCurrentlySwapped = false;
                player.sendMessage(
                        Text.literal("§6[Rinku Swap] §fWaapis slot §e" + (slotToReturnTo + 1)),
                        true
                );
            }
            return;
        }

        // ── Attack check → swap trigger ──
        if (shouldAttemptSwap(inv, client)) {
            performSwap(inv, player);
        }
    }

    /**
     * Swap karo original → swapping slot, speed ke hisaab se wapas aao
     */
    private void performSwap(PlayerInventory inv, ClientPlayerEntity player) {
        slotToReturnTo     = inv.selectedSlot;
        inv.selectedSlot   = RinkuSwapConfig.swappingSlot;
        isCurrentlySwapped = true;
        ticksRemaining     = RinkuSwapConfig.swapSpeedTicks; // ← SPEED SETTING YAHAN LAGTI HAI

        String speedStr = RinkuSwapConfig.speedLabel(RinkuSwapConfig.swapSpeedTicks);
        player.sendMessage(
                Text.literal("§6[Rinku Swap] §aSwap! §7(" + speedStr + ") §fSlot §e"
                        + (RinkuSwapConfig.swappingSlot + 1)),
                true
        );
    }

    /**
     * Kya swap karna chahiye?
     * - Player original slot pe hona chahiye
     * - Attack button press hona chahiye
     */
    private boolean shouldAttemptSwap(PlayerInventory inv, MinecraftClient client) {
        if (inv.selectedSlot != RinkuSwapConfig.originalSlot) return false;
        return client.options.attackKey.isPressed();
    }

    /**
     * Chat se slot configure karo (1-9 keys)
     */
    private void handleConfigInput(MinecraftClient client, ClientPlayerEntity player) {
        // Check karo kaunsi number key dabayi
        for (int i = 0; i < 9; i++) {
            // GLFW_KEY_1 = 49, GLFW_KEY_2 = 50, ...
            long handle = client.getWindow().getHandle();
            if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_1 + i)) {
                processNumericInput(client, player, i);
                // Ek hi key process karo
                return;
            }
        }

        // ESC = cancel
        if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE)) {
            configState = ConfigState.NONE;
            player.sendMessage(Text.literal("§6[Rinku Swap] §cConfiguration cancel kar di."), false);
        }
    }

    private void processNumericInput(MinecraftClient client, ClientPlayerEntity player, int slotIndex) {
        if (configState == ConfigState.AWAITING_ORIGINAL_SLOT) {

            RinkuSwapConfig.originalSlot = slotIndex;
            configState = ConfigState.AWAITING_SWAPPING_SLOT;
            player.sendMessage(Text.literal(
                    "§6[Rinku Swap] §fMain slot: §eSlot " + (slotIndex + 1)
                    + "\n§fAb §eSWAP slot §fkaunsa? §71-9 key dabao."), false);

        } else if (configState == ConfigState.AWAITING_SWAPPING_SLOT) {

            if (slotIndex == RinkuSwapConfig.originalSlot) {
                player.sendMessage(Text.literal(
                        "§6[Rinku Swap] §cSwap slot alag hona chahiye! Dobara try karo."), false);
                return;
            }

            RinkuSwapConfig.swappingSlot = slotIndex;
            RinkuSwapConfig.save();
            configState = ConfigState.NONE;

            player.sendMessage(Text.literal(
                    "§6[Rinku Swap] §aDone! §fMain: §eSlot " + (RinkuSwapConfig.originalSlot + 1)
                    + " §f→ Swap: §eSlot " + (RinkuSwapConfig.swappingSlot + 1)
                    + "\n§fSpeed: §e" + RinkuSwapConfig.speedLabel(RinkuSwapConfig.swapSpeedTicks)
                    + " §7| R dabao speed change karne ke liye"), false);
        }
    }
}
