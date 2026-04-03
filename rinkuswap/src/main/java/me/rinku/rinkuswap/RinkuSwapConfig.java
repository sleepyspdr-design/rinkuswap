package me.rinku.rinkuswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Config — saves: originalSlot, swappingSlot, swapSpeedTicks, enabled
 * File: .minecraft/config/rinkuswap.json
 *
 * Speed:
 *   1  tick  = 0.05s  (fastest)
 *   2  ticks = 0.10s
 *   4  ticks = 0.20s
 *   10 ticks = 0.50s
 *   20 ticks = 1.00s  (slowest)
 */
public class RinkuSwapConfig {

    public static int     originalSlot   = 0;   // 0-indexed. default slot 1
    public static int     swappingSlot   = 1;   // 0-indexed. default slot 2
    public static int     swapSpeedTicks = 1;   // 1 = fastest (0.05s), 20 = slowest (1s)
    public static boolean enabled        = true;

    private static final Gson GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("rinkuswap.json");

    private static class Data {
        int     originalSlot   = 0;
        int     swappingSlot   = 1;
        int     swapSpeedTicks = 1;
        boolean enabled        = true;
    }

    public static void load() {
        File file = CONFIG_FILE.toFile();
        if (!file.exists()) { save(); return; }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                originalSlot   = Math.max(0, Math.min(8, d.originalSlot));
                swappingSlot   = Math.max(0, Math.min(8, d.swappingSlot));
                swapSpeedTicks = Math.max(1, Math.min(20, d.swapSpeedTicks));
                enabled        = d.enabled;
            }
        } catch (Exception e) {
            RinkuSwapClient.LOGGER.error("[Rinku Swap] Config load error: " + e.getMessage());
            save();
        }
    }

    public static void save() {
        try {
            File file = CONFIG_FILE.toFile();
            file.getParentFile().mkdirs();
            Data d = new Data();
            d.originalSlot   = originalSlot;
            d.swappingSlot   = swappingSlot;
            d.swapSpeedTicks = swapSpeedTicks;
            d.enabled        = enabled;
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(d, w);
            }
        } catch (Exception e) {
            RinkuSwapClient.LOGGER.error("[Rinku Swap] Config save error: " + e.getMessage());
        }
    }

    /** ticks → human-readable seconds string */
    public static String speedLabel(int ticks) {
        double seconds = ticks * 0.05;
        return String.format("%.2fs", seconds);
    }
}
