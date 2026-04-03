package com.rinkuswap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RinkuSwapMod implements ModInitializer {

    public static final String MOD_ID = "rinkuswap";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Rinku Swap] Mod load ho gaya! Bhai ready hai stun slam ke liye!");
    }
}
