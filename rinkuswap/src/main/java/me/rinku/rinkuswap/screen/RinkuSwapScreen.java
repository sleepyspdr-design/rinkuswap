package me.rinku.rinkuswap.screen;

import me.rinku.rinkuswap.RinkuSwapConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Rinku Swap Settings Screen
 *
 * Layout:
 *  ⚡ RINKU SWAP ⚡
 *
 *  Original Slot (tera main slot):
 *  [ 1 ][ 2 ][ 3 ][ 4 ][ 5 ][ 6 ][ 7 ][ 8 ][ 9 ]
 *
 *  Swap Slot (jis pe switch hoga):
 *  [ 1 ][ 2 ][ 3 ][ 4 ][ 5 ][ 6 ][ 7 ][ 8 ][ 9 ]
 *
 *  Swap Speed:  [ << ]  0.05s  [ >> ]
 *
 *  [ Feature: ON ]
 *  [ Waapis Jao  ]
 */
public class RinkuSwapScreen extends Screen {

    private final Screen parent;

    // Speed steps: 1,2,3,4,5,6,7,8,9,10,12,14,16,18,20 ticks
    private static final int[] SPEED_STEPS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20};

    public RinkuSwapScreen(Screen parent) {
        super(Text.literal("Rinku Swap — Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        int btnW   = 40;
        int gap    = 4;
        int totalW = 9 * btnW + 8 * gap;
        int startX = cx - totalW / 2;

        // ── Row 1: Original slot buttons ──
        int row1Y = cy - 42;
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            boolean sel   = (RinkuSwapConfig.originalSlot == i);
            String label  = sel ? ("[" + (i+1) + "]") : ("" + (i+1));
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(label),
                    b -> { RinkuSwapConfig.originalSlot = idx; RinkuSwapConfig.save(); rebuild(); }
            ).dimensions(startX + i * (btnW + gap), row1Y, btnW, 20).build());
        }

        // ── Row 2: Swapping slot buttons ──
        int row2Y = cy - 4;
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            boolean sel   = (RinkuSwapConfig.swappingSlot == i);
            String label  = sel ? ("[" + (i+1) + "]") : ("" + (i+1));
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(label),
                    b -> { RinkuSwapConfig.swappingSlot = idx; RinkuSwapConfig.save(); rebuild(); }
            ).dimensions(startX + i * (btnW + gap), row2Y, btnW, 20).build());
        }

        // ── Speed control: << and >> buttons ──
        int speedY = cy + 28;
        // << decrease speed (more ticks = slower)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("<<"),
                b -> { changeSpeed(-1); rebuild(); }
        ).dimensions(cx - 80, speedY, 30, 20).build());

        // >> increase speed (fewer ticks = faster)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(">>"),
                b -> { changeSpeed(+1); rebuild(); }
        ).dimensions(cx + 50, speedY, 30, 20).build());

        // ── Feature toggle ──
        String tog = RinkuSwapConfig.enabled ? "Feature: ON  ✔" : "Feature: OFF ✘";
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(tog),
                b -> { RinkuSwapConfig.enabled = !RinkuSwapConfig.enabled; RinkuSwapConfig.save(); rebuild(); }
        ).dimensions(cx - 80, speedY + 28, 160, 20).build());

        // ── Back ──
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("< Waapis Jao"),
                b -> this.client.setScreen(parent)
        ).dimensions(cx - 60, speedY + 56, 120, 20).build());
    }

    /** Move through SPEED_STEPS array. direction: -1 = slower, +1 = faster */
    private void changeSpeed(int direction) {
        int cur = RinkuSwapConfig.swapSpeedTicks;
        int idx = 0;
        // Find current index
        for (int i = 0; i < SPEED_STEPS.length; i++) {
            if (SPEED_STEPS[i] == cur) { idx = i; break; }
        }
        // Move
        idx = Math.max(0, Math.min(SPEED_STEPS.length - 1, idx - direction));
        RinkuSwapConfig.swapSpeedTicks = SPEED_STEPS[idx];
        RinkuSwapConfig.save();
    }

    private void rebuild() { this.clearChildren(); this.init(); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "⚡  RINKU SWAP  ⚡", cx, cy - 90, 0xFFAA00);

        // Row labels
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Tera MAIN slot (jahan tu normally rehta hai):", cx, cy - 56, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "SWAP slot (jis pe click pe jaayega):", cx, cy - 18, 0xAAAAAA);

        // Divider
        ctx.fill(cx - 200, cy + 20, cx + 200, cy + 21, 0x44FFFFFF);

        // Speed label in center
        int ticks   = RinkuSwapConfig.swapSpeedTicks;
        String secs = RinkuSwapConfig.speedLabel(ticks);
        String speedStr;
        if (ticks == 1)  speedStr = "§a⚡ FASTEST: " + secs + " §7(1 tick)";
        else if (ticks <= 4) speedStr = "§e" + secs + " §7(" + ticks + " ticks) — Fast";
        else if (ticks <= 10) speedStr = "§6" + secs + " §7(" + ticks + " ticks) — Medium";
        else             speedStr = "§c" + secs + " §7(" + ticks + " ticks) — Slow";

        ctx.drawCenteredTextWithShadow(textRenderer, "Swap Speed:  " + speedStr, cx, cy + 32, 0xFFFFFF);

        // Status bar
        String status = "Original: §eSlot " + (RinkuSwapConfig.originalSlot + 1)
                + "  §fSwap: §eSlot " + (RinkuSwapConfig.swappingSlot + 1)
                + "  §f| §" + (RinkuSwapConfig.enabled ? "aON" : "cOFF");
        ctx.drawCenteredTextWithShadow(textRenderer, status, cx, cy + 92, 0xFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void close() { this.client.setScreen(parent); }
}
