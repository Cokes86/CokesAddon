package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
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

@AbilityManifest(name = "케빈", rank = Rank.S, species = Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §a위치추적기§f: 자신 위치에 반경이 5블럭인 매립형 §a위치추적기§f를 설치합니다. $[COOLDOWN]",
        "  자신을 제외한 플레이어가 §a위치추적기§f를 밟을 시",
        "  $[DURATION]간 그 플레이어의 위치가 전체적으로 공개됩니다.",
        "  §a위치추적기§f가 설치된 지역 10블럭 이내에 추가적으로 설치는 불가능하며",
        "  한 번에 $[MAX_GPS]개까지 매립할 수 있습니다.",
        "§7패시브 §8- §c재킹§f: §a위치추적기§f를 밟은 플레이어 수마다 자신이 상대에게 주는 대미지가 $[DAMAGE_INCREMENT] 증가합니다."
})
public class Kevin extends CokesAbility implements ActiveHandler {
    private final Config<Integer> MAX_GPS = Config.of(Kevin.class, "max-gps", 3, FunctionalInterfaces.positive(),
            "# 위치추적기의 최대 매립 개수",
            "# 기본값: 3 (개)");
    private final Config<Integer> COOLDOWN = Config.of(Kevin.class, "cooldown", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 위치추적기 쿨타임",
            "# 기본값: 60 (초)");
    private final Config<Integer> DURATION = Config.of(Kevin.class, "duration", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 위치추적기로 인해 노출되는 위치 지속시간",
            "# 기본값: 60 (초)");
    private final Config<Double> DAMAGE_INCREMENT = Config.of(Kevin.class, "damage-increment", 1.0, FunctionalInterfaces.positive(),
            "# 재킹으로 인해 사람당 증가할 대미지",
            "# 기본값: 1.0");

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
                getPlayer().sendMessage("[케빈] 이미 GPS를 "+ MAX_GPS +"개 설치하였습니다.");
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
            bossBar.removeAll();
            gpsList.remove(this);
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(this);
            bossBar.removeAll();
            gpsList.remove(this);
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent e) {
            if (e.getTo().distance(center) <= 5 && getGame().isParticipating(e.getPlayer()) && participant == null) {
                participant = getGame().getParticipant(e.getPlayer());
                playerDetected += 1;
                for (Participant participant : getGame().getParticipants()) {
                    bossBar.addPlayer(participant.getPlayer());
                }
            }
        }
    }
}
