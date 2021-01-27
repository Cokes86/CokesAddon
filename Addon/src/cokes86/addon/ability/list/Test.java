package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.ability.list.disguise.DisguiseUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

@AbilityManifest(name = "코크스테스트",rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL)
@Beta
public class Test extends CokesAbility implements ActiveHandler {
    public Test(AbstractGame.Participant participant) throws IllegalStateException {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !getPlayer().isSneaking()) {
            if (timer.isRunning()) timer.stop(false);
            else timer.start();
        } else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && getPlayer().isSneaking()) {
            DisguiseUtil.changeSkin(getPlayer(), UUID.fromString(""));
        }
        return false;
    }

    private final AbilityTimer timer = new AbilityTimer() {
        Location mylocation;
        @Override
        protected void onStart() {
            super.onStart();
            mylocation = LocationUtil.floorY(getPlayer().getLocation()).add(0,0.1,0);
        }

        @Override
        protected void run(int count) {
            super.run(count);

            for (int iteration = 0; iteration < 5; iteration++) {
                double angle = Math.toRadians(72.0D * iteration);
                double nextAngle = Math.toRadians(72.0D * (iteration + 2));
                double x = Math.cos(angle) * 5.5D;
                double z = Math.sin(angle) * 5.5D;
                double x2 = Math.cos(nextAngle) * 5.5D;
                double z2 = Math.sin(nextAngle) * 5.5D;
                double deltaX = x2 - x;
                double deltaZ = z2 - z;
                for (double d = 0.0D; d < 1.0D; d += 0.03D) {
                    mylocation.add(x + deltaX * d, 0.0D, z + deltaZ * d);
                    ParticleLib.REDSTONE.spawnParticle(mylocation, ParticleLib.RGB.AQUA);
                    mylocation.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
                }

                for (int iteration2 = 0; iteration2 < 5; iteration2++) {
                    double smallAngle = Math.toRadians(72.0D * iteration2);
                    double smallNextAngle = Math.toRadians(72.0D * (iteration2+2));
                    double smallX = Math.cos(smallAngle) * 1.5D;
                    double smallZ = Math.sin(smallAngle) * 1.5D;
                    double smallX2 = Math.cos(smallNextAngle) * 1.5D;
                    double smallZ2 = Math.sin(smallNextAngle) * 1.5D;
                    double sDeltaX = smallX2 - smallX;
                    double sDeltaZ = smallZ2 - smallZ;

                    double sideAngle = Math.toRadians(72.0D * (iteration+1));

                    double locationx = (x + Math.cos(sideAngle) * 5.5D)/2;
                    double locationz = (z + Math.sin(sideAngle) * 5.5D)/2;

                    mylocation.add(locationx, 0.0D, locationz);
                    for (double d2 = 0.0D; d2 < 1.0D; d2 += 0.2D) {
                        mylocation.add(smallX + sDeltaX * d2, 0.0D, smallZ + sDeltaZ * d2);
                        ParticleLib.REDSTONE.spawnParticle(mylocation, ParticleLib.RGB.AQUA);
                        mylocation.subtract(smallX + sDeltaX * d2, 0.0D, smallZ + sDeltaZ * d2);
                    }
                    mylocation.subtract(locationx, 0.0D, locationz);
                }
            }
        }

        @Override
        protected void onEnd() {
            super.onEnd();
        }

        @Override
        protected void onSilentEnd() {
            super.onSilentEnd();
        }
    }.register();
}
