package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.ability.CokesAbility.Config.Condition;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.annotations.Beta;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@AbilityManifest(name = "케빈", rank = Rank.A, species = Species.HUMAN, explain = {
        "철괴 우클릭 현재 위치에 매설형 위치추적기를 최대 $[MAX_GPS]개까지 설치합니다. $[COOLDOWN]",
        "자신을 제외한 다른 플레이어가 이 위치추적기를 밟을 시 $[DURATION]간 모두에게 위치가 노출됩니다.",
        "또한, 영구적으로 상대방에게 주는 대미지가 $[DAMAGE_INCREMENT] 증가합니다.",
        "이미 설치한 위치추적기의 10블럭 이내에 다른 위치추적기를 설치할 수 없습니다."
})
@Beta
public class Kevin extends CokesAbility implements ActiveHandler {
    private final Config<Integer> MAX_GPS = new Config<>(Kevin.class, "max-gps", 3, PredicateUnit.positive());
    private final Config<Integer> COOLDOWN = new Config<>(Kevin.class, "cooldown", 60, Condition.COOLDOWN);
    private final Config<Integer> DURATION = new Config<>(Kevin.class, "duration", 60, Condition.TIME);
    private final Config<Double> DAMAGE_INCREMENT = new Config<>(Kevin.class, "damage-increment", 2.0, PredicateUnit.positive());

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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
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
