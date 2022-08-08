package com.cokes86.cokesaddon.util.nms;

import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;

public class NMSUtil {
    private static final INMS INSTANCE;

    static {
        try {
            INSTANCE = Class.forName("com.cokes86.cokesaddon.util.nms." + ServerVersion.getName() + ".NMSImpl")
                    .asSubclass(INMS.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }
}
