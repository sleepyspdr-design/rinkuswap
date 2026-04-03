package com.rinkuswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class RinkuSwapConfig {

    // Mace kis slot mein hai? 0=slot1 ... 8=slot9. Default: slot 2
    public static int     maceSlot    = 1;
    // Feature on/off
    public static boolean swapEnabled = true;
    // Swap mode: true = STUN SLAM mode (fast, mid-fall swap), false = normal swap
    public static boolean stunSlamMode = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("rinkuswap.json");

    private static class Data {
        int     maceSlot    = 1;
        boolean swapEnabled = true;
        boolean stunSlamMode = true;
    }

    public static void load() {
        File file = CONFIG_FILE.toFile();
        if (!file.exists()) { save(); return; }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                maceSlot     = Math.max(0, Math.min(8, d.maceSlot));
                swapEnabled  = d.swapEnabled;
                stunSlamMode = d.stunSlamMode;
            }
        } catch (Exception e) {
            RinkuSwapMod.LOGGER.error("[Rinku Swap] Config load nahi hua: " + e.getMessage());
            save();
        }
    }

    public static void save() {
        try {
            File file = CONFIG_FILE.toFile();
            file.getParentFile().mkdirs();
            Data d = new Data();
            d.maceSlot    = maceSlot;
            d.swapEnabled  = swapEnabled;
            d.stunSlamMode = stunSlamMode;
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(d, w);
            }
        } catch (Exception e) {
            RinkuSwapMod.LOGGER.error("[Rinku Swap] Config save nahi hua: " + e.getMessage());
        }
    }
}
