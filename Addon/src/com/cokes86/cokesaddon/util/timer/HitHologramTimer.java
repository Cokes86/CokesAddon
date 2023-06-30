package com.cokes86.cokesaddon.util.timer;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HitHologramTimer extends AbilityTimer {
    private final IHologram hologram;

    public static HitHologramTimer create(@NotNull AbilityBase abilityBase, Location hitLoc, String hologramTitle) {
        return new HitHologramTimer(abilityBase, hitLoc, hologramTitle);
    }

    private HitHologramTimer(@NotNull AbilityBase abilityBase, Location hitLoc, String hologramText) {
        abilityBase.super(TaskType.NORMAL, 30);
        setPeriod(TimeUnit.TICKS, 1);
        Random random = new Random();
        this.hologram = NMS.newHologram(abilityBase.getPlayer().getWorld(),
                hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
                hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25),
                hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5),
                hologramText);
        for (Player player : abilityBase.getPlayer().getWorld().getPlayers()) {
            hologram.display(player);
        }
    }

    @Override
    protected void run(int count) {
        hologram.teleport(hologram.getLocation().add(0, 0.03, 0));
    }

    @Override
    protected void onEnd() {
        onSilentEnd();
    }

    @Override
    protected void onSilentEnd() {
        hologram.unregister();
    }

    public IHologram getHologram() {
        return hologram;
    }

    public void setText(String newText) {
        hologram.setText(newText);
    }
}
