package com.rinkuswap.screen;

import com.rinkuswap.RinkuSwapConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RinkuSwapScreen extends Screen {

    private final Screen parent;

    public RinkuSwapScreen(Screen parent) {
        super(Text.literal("Rinku Swap — Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // ── 9 slot buttons (mace kahan hai?) ──
        int btnW   = 44;
        int gap    = 5;
        int totalW = 9 * btnW + 8 * gap;
        int startX = cx - totalW / 2;
        int rowY   = cy - 20;

        for (int i = 0; i < 9; i++) {
            final int idx = i;
            boolean sel   = (RinkuSwapConfig.maceSlot == i);
            String label  = sel ? ("[" + (i + 1) + "]") : ("" + (i + 1));

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(label),
                    b -> { RinkuSwapConfig.maceSlot = idx; RinkuSwapConfig.save(); rebuild(); }
            ).dimensions(startX + i * (btnW + gap), rowY, btnW, 22).build());
        }

        // ── Stun Slam mode toggle ──
        String slamLabel = RinkuSwapConfig.stunSlamMode
                ? "Mode: STUN SLAM ⚡ (Fast Fall Swap)"
                : "Mode: Normal Swap";
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(slamLabel),
                b -> { RinkuSwapConfig.stunSlamMode = !RinkuSwapConfig.stunSlamMode; RinkuSwapConfig.save(); rebuild(); }
        ).dimensions(cx - 120, rowY + 32, 240, 22).build());

        // ── Feature ON/OFF ──
        String tog = RinkuSwapConfig.swapEnabled ? "Feature: ON  ✔" : "Feature: OFF ✘";
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(tog),
                b -> { RinkuSwapConfig.swapEnabled = !RinkuSwapConfig.swapEnabled; RinkuSwapConfig.save(); rebuild(); }
        ).dimensions(cx - 90, rowY + 62, 180, 22).build());

        // ── Back ──
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("< Waapis Jao"),
                b -> this.client.setScreen(parent)
        ).dimensions(cx - 70, rowY + 94, 140, 22).build());
    }

    private void rebuild() { this.clearChildren(); this.init(); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                "⚡ RINKU SWAP ⚡", cx, cy - 78, 0xFFAA00);

        // Subtitle
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Mace kis slot mein hai? Woh slot select karo:", cx, cy - 62, 0xAAAAAA);

        ctx.drawCenteredTextWithShadow(textRenderer,
                "[ brackets ] = abhi selected", cx, cy - 50, 0x55FFFF);

        // Divider
        ctx.fill(cx - 190, cy - 40, cx + 190, cy - 39, 0x55FFFFFF);

        // Status
        boolean en = RinkuSwapConfig.swapEnabled;
        boolean slam = RinkuSwapConfig.stunSlamMode;
        String status = "Mace: Slot " + (RinkuSwapConfig.maceSlot + 1)
                + "  |  " + (slam ? "STUN SLAM" : "Normal")
                + "  |  " + (en ? "§aON" : "§cOFF");
        ctx.drawCenteredTextWithShadow(textRenderer, status, cx, cy + 18, 0xFFFFFF);

        // Stun Slam description
        if (RinkuSwapConfig.stunSlamMode) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "Gir rahe ho + left click → mace swap → BOOM stun slam → waapis",
                    cx, cy + 32, 0x55FF55);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "Left click → mace slot pe jao → click chodo → waapis",
                    cx, cy + 32, 0xAAAAAA);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void close() { this.client.setScreen(parent); }
}
