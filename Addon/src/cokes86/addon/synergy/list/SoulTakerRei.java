package cokes86.addon.synergy.list;

import cokes86.addon.synergy.CokesSynergy;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "레이<소울테이커>", rank = AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8- §c소울 커팅§r: 상대방을 공격할 때 주는 대미지가 $[DAMAGE]만큼 상승합니다.",
        "  추가로 자신이 가진 §e흡수 체력§r 한 칸 만큼 대미지가 1씩 상승합니다.",
        "§7플레이어 사망 §8- §c리멤버§r: 플레이어가 사망한 자리에 영혼이 남아 돌아다닙니다.",
        "  영혼 근처 $[RANGE]블럭에 다가갈 경우 영혼을 흡수하여",
        "  자신의 §e흡수 체력§r이 2만큼 증가합니다.",
        "§7철괴 우클릭 §8- §c이맨서페이션§r: $[DURATION]동안 주는 대미지가 $[ADDITIONAL]만큼 증가합니다.",
        "  대신, 공격을 시도할 때 마다 자신 최대 체력의 $[COST]%만큼 지불합니다. $[EMANCIPATION_COOL]",
        "§7사망 위기 §8- §c어아우절§r: 사망을 무시하고 §c리멤버§r를 통해 흡수한 영혼의 수를 모두 소모해",
        "  체력이 영혼당 1씩 회복됩니다. $[AROUSAL_COOL]",
        "  §c이맨서페이션§r 지속시간 도중 발동되었다면, 0.5배 더 회복합니다."
})
@Beta
public class SoulTakerRei extends CokesSynergy implements ActiveHandler {
    public static final Config<Integer> DAMAGE = new Config<>(SoulTakerRei.class, "추가대미지", 3, a -> a > 0),
            RANGE = new Config<>(SoulTakerRei.class, "리멤버_흡수_범위", 5, a -> a > 0),
            DURATION = new Config<>(SoulTakerRei.class, "이맨서페이션_지속시간", 20, Config.Condition.TIME),
            ADDITIONAL = new Config<>(SoulTakerRei.class, "이맨서페이션_추가대미지", 2, a -> a > 0),
            EMANCIPATION_COOL = new Config<>(SoulTakerRei.class, "이맨서페이션_쿨타임", 60, Config.Condition.COOLDOWN),
            AROUSAL_COOL = new Config<>(SoulTakerRei.class, "어아우절_쿨타임", 60, Config.Condition.COOLDOWN);
    public static final Config<Double> COST = new Config<>(SoulTakerRei.class, "이맨서페이션_코스트", 5.0, a -> a > 0);
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();

    private int soul = 0;

    private final Cooldown emancipation_cool = new Cooldown(EMANCIPATION_COOL.getValue(), "이맨서페이션"),
            arousal_cool = new Cooldown(AROUSAL_COOL.getValue(), "어아우절");
    private final Duration duration = new Duration(DURATION.getValue(), emancipation_cool) {
        @Override
        protected void onDurationProcess(int i) {
            ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0,1,0),0,0.5,0, 10, MaterialX.REDSTONE_BLOCK);
        }
    };

    public SoulTakerRei(AbstractGame.Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !emancipation_cool.isCooldown() && !duration.isDuration()) {
            return duration.start();
        }
        return false;
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {
        if (!e.getParticipant().equals(getParticipant())) {
            int x = e.getParticipant().getPlayer().getLocation().getBlockX();
            int y = e.getParticipant().getPlayer().getLocation().getBlockY();
            int z = e.getParticipant().getPlayer().getLocation().getBlockZ();
            getPlayer().sendMessage("X "+x+" Y "+y+" Z "+z+"에 영혼이 생성되었습니다.");
            new SoulRemember(e.getParticipant().getPlayer().getLocation()).start();
        }
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer())) {
            if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && arousal_cool.isRunning() && soul > 0) {
                e.setDamage(0);
                Healths.setHealth(getPlayer(), soul * (1+ (duration.isRunning() ? 0.5 : 0)));
                arousal_cool.start();

                soul = 0;
                channel.update(null);
            }
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) e.getDamager();
            if (arrow.getShooter() instanceof Entity) {
                damager = (Entity) arrow.getShooter();
            }
        }

        if (damager.equals(getPlayer())) {
            double add = DAMAGE.getValue() + (int)(NMS.getAbsorptionHearts(getPlayer())/2) + (duration.isRunning() ? ADDITIONAL.getValue() : 0);
            e.setDamage(e.getDamage() + add);

            if (duration.isRunning()) {
                final double maxHealth = AttributeUtil.getMaxHealth(getPlayer()), health = getPlayer().getHealth();
                final float absorption = NMS.getAbsorptionHearts(getPlayer());
                final double damage = maxHealth * COST.getValue() / 100.0f;
                if (getPlayer().getGameMode().equals(GameMode.SURVIVAL) || getPlayer().getGameMode().equals(GameMode.ADVENTURE)) {
                    if (absorption >= damage) {
                        NMS.setAbsorptionHearts(getPlayer(), (float) (absorption - damage));
                    } else {
                        final double temp = damage - absorption;
                        if (health > temp) {
                            if (absorption != 0) NMS.setAbsorptionHearts(getPlayer(), 0);
                            getPlayer().setHealth(Math.max(0.0, health - temp));
                        }
                    }
                }
            }
        }
    }

    private class SoulRemember extends AbilityTimer {
        private final Location location;

        public SoulRemember(Location location) {
            this.location = location;
            this.setPeriod(TimeUnit.TICKS, 1);
        }

        public void run(int cnt) {
            ParticleLib.REDSTONE.spawnParticle(location.clone().add(0,0.5,0), RGB.BLACK);

            if (LocationUtil.getNearbyEntities(Player.class, location, RANGE.getValue(), 255, null).contains(getPlayer())) {
                NMS.setAbsorptionHearts(getPlayer(), NMS.getAbsorptionHearts(getPlayer())+2);
                stop(true);
                channel.update("§8흡수한 영혼: "+ (++soul));
            }
        }
    }
}
