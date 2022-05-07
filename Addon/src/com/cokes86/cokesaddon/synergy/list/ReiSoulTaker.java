package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "레이<소울테이커>", rank = AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8- §c소울 커팅§f: 상대방을 공격할 시 주는 대미지가 $[DAMAGE]% 증가합니다.",
        "  추가로 자신이 가진 §e흡수 체력§f $[DEMAND_ABSORPTION]당 대미지가 1 증가합니다.",
        "  퍼센트 대미지가 증가 후 §e흡수 체력§f으로 인한 대미지가 증가합니다.",
        "§7플레이어 사망 §8- §c리멤버§f: 플레이어가 사망한 자리에 영혼이 남아 돌아다닙니다.",
        "  영혼 근처 $[RANGE]블럭에 다가갈 경우 영혼을 흡수하여",
        "  자신의 §e흡수 체력§f이 $[REMEMBER_ABSORPTION]만큼 증가합니다.",
        "§7철괴 우클릭 §8- §c이맨서페이션§f: $[DURATION]동안 주는 대미지가 $[ADDITIONAL]% 증가합니다. 이는 §c소울 커팅§f과 중첩됩니다.",
        "  대신, 공격을 시도할 때 마다 자신 최대 체력의 $[COST]%만큼 지불합니다. $[EMANCIPATION_COOL]",
        "  지속동안 철괴 우클릭 시 자동으로 종료되며",
        "  남은 시간에 비례하여 최대 50%까지 쿨타임이 감소합니다.",
        "§7사망 위기 §8- §c어아우절§f: 사망을 무시하고 §c리멤버§f를 통해 흡수한",
        "  영혼의 수를 모두 소모해 체력이 영혼당 1씩 회복됩니다. $[AROUSAL_COOL]",
        "  §c이맨서페이션§f 지속시간 도중 발동되었다면, 0.5배 더 회복합니다."
})
public class ReiSoulTaker extends CokesSynergy implements ActiveHandler {
    public static final Config<Double> DAMAGE = Config.of(ReiSoulTaker.class, "추가대미지", 15.0, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> RANGE = Config.of(ReiSoulTaker.class, "리멤버_흡수_범위", 5, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> DURATION = Config.of(ReiSoulTaker.class, "이맨서페이션_지속시간", 20, Config.Condition.TIME);
    public static final Config<Double> ADDITIONAL = Config.of(ReiSoulTaker.class, "이맨서페이션_추가대미지", 25.0, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> EMANCIPATION_COOL = Config.of(ReiSoulTaker.class, "이맨서페이션_쿨타임", 60, Config.Condition.COOLDOWN);
    public static final Config<Integer> AROUSAL_COOL = Config.of(ReiSoulTaker.class, "어아우절_쿨타임", 60, Config.Condition.COOLDOWN);
    public static final Config<Integer> REMEMBER_ABSORPTION = Config.of(ReiSoulTaker.class, "리맴버_흡수체력_증가량", 3, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> DEMAND_ABSORPTION = Config.of(ReiSoulTaker.class, "흡수체력_요구량", 2, FunctionalInterfaceUnit.positive());
    public static final Config<Double> COST = Config.of(ReiSoulTaker.class, "이맨서페이션_코스트", 5.0, FunctionalInterfaceUnit.positive());
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();

    private int soul = 0;

    private final Cooldown emancipation_cool = new Cooldown(EMANCIPATION_COOL.getValue(), "이맨서페이션");
    private final Cooldown arousal_cool = new Cooldown(AROUSAL_COOL.getValue(), "어아우절");
    private final Duration duration = new Duration(DURATION.getValue(), emancipation_cool) {
        @Override
        protected void onDurationProcess(int i) {
            ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0,1,0),0,0.5,0, 10, MaterialX.REDSTONE_BLOCK);
        }
    };

    public ReiSoulTaker(AbstractGame.Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !emancipation_cool.isCooldown()) {
            if (duration.isRunning()) {
                double a = duration.getCount() / (double)duration.getMaximumCount();
                duration.stop(true);
                int max_cool = emancipation_cool.getMaximumCount();
                int remainder = (int) (max_cool * (a / 2));
                emancipation_cool.setCount(remainder);
            } else {
                return duration.start();
            }
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

    @SubscribeEvent(childs = {EntityDamageByBlockEvent.class})
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer())) {
            if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && !arousal_cool.isRunning() && soul > 0) {
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
        onEntityDamage(e);
        Entity damager = e.getDamager();
        if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) e.getDamager();
            if (arrow.getShooter() instanceof Entity) {
                damager = (Entity) arrow.getShooter();
            }
        }

        if (damager.equals(getPlayer())) {
            e.setDamage(e.getDamage() * (1 + DAMAGE.getValue()/100.0 + (duration.isRunning() ? ADDITIONAL.getValue()/100.0 : 0)) + (int)(NMS.getAbsorptionHearts(getPlayer())/DEMAND_ABSORPTION.getValue()));

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
                NMS.setAbsorptionHearts(getPlayer(), NMS.getAbsorptionHearts(getPlayer())+REMEMBER_ABSORPTION.getValue());
                stop(true);
                channel.update("§8흡수한 영혼: "+ (++soul));
                SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Note.Tone.E));
            }
        }
    }
}
