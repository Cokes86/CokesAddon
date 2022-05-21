package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.CokesAbility.Config.Condition;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@AbilityManifest(name = "케빈", rank = Rank.A, species = Species.HUMAN, explain = {
        "철괴 우클릭 시, 반경이 5블럭이 되는 매설형 위치추적기를 설치합니다. (최대 $[MAX_GPS]개)",
        "자신을 제외하고 위치추적기를 밟을 시 해당 플레이어의 위치가 $[duration]간 노출됩니다.",
        "영구적으로 위치추적기를 밟은 플레이어 수 × $[DAMAGE_INCREMENT]의 대미지가 증가합니다.",
        "위치추적기간의 거리는 10블럭을 넘어야만 합니다."
})
public class Kevin extends CokesAbility implements ActiveHandler {
    private final Config<Integer> MAX_GPS = Config.of(Kevin.class, "max-gps", 3, FunctionalInterfaceUnit.positive());
    private final Config<Integer> COOLDOWN = Config.of(Kevin.class, "cooldown", 60, Condition.COOLDOWN);
    private final Config<Integer> DURATION = Config.of(Kevin.class, "duration", 60, Condition.TIME);
    private final Config<Double> DAMAGE_INCREMENT = Config.of(Kevin.class, "damage-increment", 2.0, FunctionalInterfaceUnit.positive());

    private int playerDetected = 0;
    private final List<GPS> gpsList = new ArrayList<>();

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());

    public Kevin(Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            if (gpsList.size() < MAX_GPS.getValue()) {
                Location location = getPlayer().getLocation().clone();
                for (GPS gps : gpsList) {
                    double distance = gps.center.distance(location);
                    if (distance <= 10) {
                        getPlayer().sendMessage("[케빈] 설치한 GPS의 10블럭 이내 위치에 설치할 수 없습니다.");
                        return false;
                    }
                }
                gpsList.add(new GPS(location));
                return cooldown.start();
            } else {
                getPlayer().sendMessage("[케빈] 이미 GPS를 3개 설치하였습니다.");
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getDamager() == null) return;
        Entity damager = e.getDamager();
        if (NMS.isArrow(damager)) {
            Projectile arrow = (Projectile) e.getDamager();
            if (arrow.getShooter() instanceof Entity) {
                damager = (Entity) arrow.getShooter();
            }
        }

        if (damager.equals(getPlayer())) {
            e.setDamage(e.getDamage() + playerDetected * DAMAGE_INCREMENT.getValue());
        }
    }

    class GPS extends AbilityTimer implements Listener {
        private Participant participant = null;
        private final Location center;
        private final BossBar bossBar;
        private int second = 0;
        public GPS(Location location) {
            super();
            this.center = location;
            this.bossBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID);
            setPeriod(TimeUnit.TICKS, 1);
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
            start();
        }

        @Override
        protected void run(int count) {
            if (participant == null) {
                for (Vector vector : Circle.of(5, 100)) {
                    ParticleLib.REDSTONE.spawnParticle(center.clone().add(vector), RGB.WHITE);
                }
                if (!center.getWorld().getWorldBorder().isInside(center)) {
                    getPlayer().sendMessage("[케빈] GPS가 이상한 영향으로 인해 망가졌습니다.");
                    stop(true);
                }
            } else {
                Player player = participant.getPlayer();
                bossBar.setTitle(player.getDisplayName()+"님의 위치 | "+(int)player.getLocation().getX()+ " | "+(int)player.getLocation().getY() + " | "+(int)player.getLocation().getZ());
                if (++second == 20 * DURATION.getValue()) {
                    stop(false);
                }
            }
        }

        @Override
        protected void onEnd() {
            HandlerList.unregisterAll(this);
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(this);
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent e) {
            if (e.getTo().distance(center) <= 5 && getGame().isParticipating(e.getPlayer())) {
                participant = getGame().getParticipant(e.getPlayer());
                playerDetected += 1;
            }
        }
    }
}
