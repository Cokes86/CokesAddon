package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.Material;

@AbilityManifest(name = "코크스테스트",rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL)
@Beta
@Materials(materials = {Material.IRON_INGOT, Material.GOLD_INGOT})
public class Test extends CokesAbility implements ActiveHandler {
    public Test(AbstractGame.Participant participant) throws IllegalStateException {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !getPlayer().isSneaking()) {
            if (rainstarParticle.isRunning()) rainstarParticle.stop(false);
            else rainstarParticle.start();
        } else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && getPlayer().isSneaking()) {
            if (xParticle.isRunning()) xParticle.stop(false);
            else xParticle.start();
        }
        return false;
    }

    private final AbilityTimer xParticle = new AbilityTimer() {
        Location mylocation;
        @Override
        protected void onStart() {
            super.onStart();
            mylocation = LocationUtil.floorY(getPlayer().getLocation().clone()).add(0,0.1,0);
        }

        @Override
        protected void run(int count) {
            super.run(count);

            xParticleShow(mylocation, 3);
            for (double a = 0.1; a<1.6; a+=0.1) {
                xParticleShow(mylocation.clone().add(a/2,0,0),3-a);
                xParticleShow(mylocation.clone().add(0,0,a/2),3-a);
                xParticleShow(mylocation.clone().add(-a/2,0,0),3-a);
                xParticleShow(mylocation.clone().add(0,0,-a/2),3-a);
            }
        }

        private void xParticleShow(Location center, double radious) {
            double quarter = Math.PI / 2;

            for (int interation = 0; interation < 2; interation++) {
                double angle = Math.toRadians(45) + interation*quarter;
                double nextAngle = Math.toRadians(45) + (interation+2)*quarter;

                double x = Math.cos(angle) * radious;
                double z = Math.sin(angle) * radious;
                double x2 = Math.cos(nextAngle) * radious;
                double z2 = Math.sin(nextAngle) * radious;

                double deltaX = x2 - x;
                double deltaZ = z2 - z;
                for (double d = 0.0D; d < 1.0D; d += 0.07D) {
                    center.add(x + deltaX * d, 0.0D, z + deltaZ * d);
                    ParticleLib.REDSTONE.spawnParticle(center, RGB.BLACK);
                    center.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
                }
            }
        }
    }.setPeriod(TimeUnit.TICKS, 30).register();

    private final AbilityTimer rainstarParticle = new AbilityTimer() {
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
                    ParticleLib.REDSTONE.spawnParticle(mylocation, RGB.AQUA);
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
                        ParticleLib.REDSTONE.spawnParticle(mylocation, RGB.AQUA);
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
