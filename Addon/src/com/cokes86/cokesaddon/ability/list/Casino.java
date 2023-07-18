package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.decorate.Lite;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import com.google.common.base.Strings;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

@AbilityManifest(name="카지노", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 우클릭시 랜덤한 효과 하나를 영구히 부여합니다. $[COOLDOWN]",
        "이미 부여한 효과는 다시 나오지 않습니다.",
        "철괴 좌클릭으로 자신에게 부여된 효과를 알 수 있습니다."
})
@Lite
public class Casino extends CokesAbility implements ActiveHandler {
    //쿨타임
    private static final Config<Integer> COOLDOWN = Config.of(Casino.class, "cooldown", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 쿨타임", "# 기본값: 60 (초)");

    //각종 효과 세부내역
    private static final Config<Double> DAMAGE_INCREMENT_VALUE = Config.of(Casino.class, "effects.damage-increment", 0.5, FunctionalInterfaces.positive(),
            "# 효과 중 대미지 증가량 수치", "# 기본값: 0.5");
    private static final Config<Double> DAMAGE_DECREMENT_VALUE = Config.of(Casino.class, "effects.damage-decrement", 0.5, FunctionalInterfaces.positive(),
            "# 효과 중 대미지 감소량 수치", "# 기본값: 0.5");
    private static final Config<Integer> WITHER_PERIOD = Config.of(Casino.class, "effects.wither-period", 4, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 효과 중 위더 대미지 주기", "# 기본값: 4 (초)");
    private static final Config<Integer> TWIST_PERIOD = Config.of(Casino.class, "effects.twist-period", 10, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 효과 중 시야 뒤틀림 주기", "# 기본값: 10 (초)");
    private static final Config<Integer> HEAL_VALUE = Config.of(Casino.class, "effects.heal", 2, FunctionalInterfaces.positive(),
            "# 효과 중 체력 즉시 회복량", "# 기본값: 2");
    private static final Config<Integer> MAX_HEALTH_DECREMENT = Config.of(Casino.class, "effects.max-health-decrement", 2, FunctionalInterfaces.positive(),
            "# 효과 중 최대 체력 감소량", "# 기본값: 2");
    private static final Config<Double> REGAIN_INCREMENT = Config.of(Casino.class, "effects.regain-increment", 0.2, FunctionalInterfaces.positive(),
            "# 효과 중 주기적인 체력 회복 증가량", "# 기본값: 0.2 (배)");
    private static final Config<Integer> BLEED_ATTACK_PREDICATE = Config.of(Casino.class, "effects.bleed-attack-predicate", 4, FunctionalInterfaces.positive(),
            "# 효과 중 출혈 부여 조건", "# 기본값: 4 (회)");
    private static final Config<Integer> BLEED_DURATION = Config.of(Casino.class, "effects.bleed-duration", 1, FunctionalInterfaces.positive(),
            "# 효과 중 출혈 시간", "# 기본값: 1 (초)");
    private static final Config<Integer> COOLDOWN_INCREMENT = Config.of(Casino.class, "effects.cooldown-increment", 50, FunctionalInterfaces.positive(),
            "# 효과 중 쿨타임 증가량", "# 기본값: 50 (%)");
    private static final Config<Integer> STUN_DURATION = Config.of(Casino.class, "effects.stun-duration", 2, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 효과 중 스턴 지속시간", "# 기본값: 2 (초)");

    private final double defaultMaxHealth = Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
    private Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._75);
    private final Map<Effects, Boolean> effects = new HashMap<>(ImmutableMap.<Effects, Boolean>builder()
            .put(Effects.DAMAGE_INCREMENT, false)
            .put(Effects.WITHER, false)
            .put(Effects.RESISTANCE, false)
            .put(Effects.HEAL, false)
            .put(Effects.TWIST, false)
            .put(Effects.REGAIN, false)
            .put(Effects.MAX_HEALTH_DOWN, false)
            .put(Effects.BLEED, false)
            .put(Effects.COOLDOWN_UP, false)
            .put(Effects.IGNORE_FALL, false)
            .put(Effects.STUN, false)
            .put(Effects.FIRE_RESISTANCE, false)
            .put(Effects.NO_CRITICAL, false).build());
    private final Map<AbstractGame.Participant, HitBleedTimer> hit = new HashMap<>();

    private final AbilityTimer wither = new AbilityTimer() {
        @Override
        protected void run(int count) {
            NMSUtil.damageWither(getPlayer(), 1);
        }
    }.setInitialDelay(TimeUnit.SECONDS, WITHER_PERIOD.getValue()).setPeriod(TimeUnit.SECONDS, WITHER_PERIOD.getValue()).register();
    private final AbilityTimer aim = new AbilityTimer() {
        @Override
        protected void run(int count) {
            ArrayList<Participant> participants = new ArrayList<>(getGame().getParticipants());
            participants.remove(getParticipant());
            Player target = new Random().pick(participants).getPlayer();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Vector direction = target.getEyeLocation().toVector().subtract(getPlayer().getEyeLocation().toVector());
                NMS.rotateHead(player, getPlayer(), LocationUtil.getYaw(direction), LocationUtil.getPitch(direction));
            }
        }
    }.setInitialDelay(TimeUnit.SECONDS, TWIST_PERIOD.getValue()).setPeriod(TimeUnit.SECONDS, TWIST_PERIOD.getValue()).register();
    private final AbilityInfoTimer infoTimer = new AbilityInfoTimer();

    public Casino(AbstractGame.Participant arg0) {
        super(arg0);
    }

    private final Predicate<Entity> predicate = entity -> {
        if (entity == null || entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())
                    || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                    || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
                return false;
            }
            if (getGame() instanceof Teamable) {
                final Teamable teamGame = (Teamable) getGame();
                final AbstractGame.Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
                return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
            }
        }
        return true;
    };

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
            AttributeUtil.setMaxHealth(getPlayer(), defaultMaxHealth);
        } else {
            if (effects.get(Effects.MAX_HEALTH_DOWN)) {
                AttributeUtil.setMaxHealth(getPlayer(), defaultMaxHealth - 2);
            }
        }
    }

    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT) {
            if (clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !infoTimer.isRunning()) {
                if (!effects.containsValue(false)) {
                    getPlayer().sendMessage("§c[!]§f 모든 효과를 다 받았습니다.");
                    return false;
                }
                return infoTimer.start();
            } else if (clickType == ClickType.LEFT_CLICK) {
                getPlayer().sendMessage("[Casino] 효과 목록");
                StringJoiner joiner = new StringJoiner(", ");
                for (Map.Entry<Effects, Boolean> entry : effects.entrySet()) {
                    joiner.add((entry.getValue() ? "§e" : "§7") + entry.getKey().getName());
                }
                getPlayer().sendMessage(joiner.toString());
            }
        }
        return false;
    }

    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && e.getCause().equals(EntityDamageEvent.DamageCause.FALL) && effects.get(Effects.IGNORE_FALL)) {
            e.setCancelled(true);
            getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
            SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
        } else if (e.getEntity().equals(getPlayer()) && (e.getCause().equals(EntityDamageEvent.DamageCause.FIRE) || e.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)) && effects.get(Effects.FIRE_RESISTANCE)) {
            e.setCancelled(true);
        }

        Entity attacker = e.getDamager();
        if (attacker == null) return;
        if (NMS.isArrow(attacker)) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Entity) {
                attacker = (Entity) projectile.getShooter();
            }
        }

        if (e.getEntity().equals(getPlayer()) && effects.get(Effects.RESISTANCE)) {
            e.setDamage(e.getDamage()- DAMAGE_DECREMENT_VALUE.getValue());
        }
        if (attacker.equals(getPlayer())) {
            if (effects.get(Effects.DAMAGE_INCREMENT)) {
                e.setDamage(e.getDamage() + DAMAGE_INCREMENT_VALUE.getValue());
            }
            if (effects.get(Effects.BLEED)) {
                if (predicate.test(e.getEntity()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
                    AbstractGame.Participant p = getGame().getParticipant(e.getEntity().getUniqueId());
                    if (!hit.containsKey(p)) hit.put(p, new HitBleedTimer(p));
                    hit.get(p).addHit();
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        if (e.getEntity().equals(getPlayer()) && effects.get(Effects.REGAIN)) {
            e.setAmount(e.getAmount() * (1 + REGAIN_INCREMENT.getValue()));
        }
    }

    @SubscribeEvent
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter().equals(getPlayer()) && effects.get(Effects.NO_CRITICAL) && NMS.isArrow(event.getEntity())) {
            NMSUtil.setCritical(event.getEntity(), false);
        }
    }

    private enum Effects {
        DAMAGE_INCREMENT("주는 대미지 "+ DAMAGE_INCREMENT_VALUE + " 증가"),
        WITHER(WITHER_PERIOD+"마다 1의 시듦 대미지 부여"),
        RESISTANCE("받는 대미지 "+DAMAGE_DECREMENT_VALUE+" 감소"),
        HEAL("체력 "+HEAL_VALUE+" 즉시 흭득"),
        TWIST(TWIST_PERIOD+"마다 시야 뒤틀림"),
        REGAIN("회복량 "+REGAIN_INCREMENT+"배 증가"),
        MAX_HEALTH_DOWN("최대 체력 "+MAX_HEALTH_DECREMENT+" 감소"),
        BLEED(BLEED_ATTACK_PREDICATE+"회 타격 시 출혈 "+BLEED_DURATION+"부여"),
        COOLDOWN_UP("쿨타임 "+COOLDOWN_INCREMENT+"% 증가"),
        IGNORE_FALL("낙하대미지 무시"),
        STUN("스턴 "+STUN_DURATION+" 부여"),
        FIRE_RESISTANCE("화염저항 영구히 부여"),
        NO_CRITICAL("화살 크리티컬 삭제");

        private final String name;
        Effects(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private class AbilityInfoTimer extends AbilityTimer {
        private final ChatColor[] chatColors = {
                ChatColor.YELLOW,
                ChatColor.RED,
                ChatColor.GOLD,
                ChatColor.LIGHT_PURPLE,
                ChatColor.DARK_PURPLE,
                ChatColor.AQUA
        };
        private final Note E = Note.natural(0, Note.Tone.E), G = Note.natural(1, Note.Tone.G);
        private final Random random = new Random();
        private final List<Effects> list = new ArrayList<>(effects.size());
        private AbilityTimer check = new AbilityTimer() {};

        public AbilityInfoTimer() {
            super(15);
            setPeriod(TimeUnit.TICKS, 2);
        }

        @Override
        public boolean isRunning() {
            return super.isRunning() || check.isRunning();
        }

        @Override
        protected void onStart() {
            list.clear();
            for (Effects effect : effects.keySet()) {
                if (!effects.get(effect)) list.add(effect);
            }
        }

        @Override
        public void run(int count) {
            final String title = chatColors[random.nextInt(chatColors.length)] + list.get(random.nextInt(list.size())).getName();
            NMS.sendTitle(getPlayer(), title, "", 0, 6, 0);
            SoundLib.PIANO.playInstrument(getPlayer(), G);
            SoundLib.PIANO.playInstrument(getPlayer(), E);
        }

        @Override
        protected void onEnd() {
            final Effects result = list.get(random.nextInt(list.size()));
            final ChatColor color = chatColors[random.nextInt(chatColors.length)];
            final String title = color + result.getName();
            NMS.sendTitle(getPlayer(), title, "", 0, 6, 0);
            SoundLib.PIANO.playInstrument(getPlayer(), G);
            SoundLib.PIANO.playInstrument(getPlayer(), E);

            check = new AbilityTimer(3) {
                @Override
                protected void run(int count) {
                    final String title = color + result.getName();
                    NMS.sendTitle(getPlayer(), title, "", 0, 21, 0);
                }

                @Override
                protected void onEnd() {
                    effects.put(result, true);
                    NMS.clearTitle(getPlayer());
                    getPlayer().sendMessage("[Casino] §e"+result.getName()+ " §f효과를 얻었습니다.");

                    switch (result) {
                        case WITHER:
                            wither.start();
                            break;
                        case HEAL:
                            CokesUtil.vampirePlayer(getPlayer(), HEAL_VALUE.getValue());
                            break;
                        case TWIST:
                            aim.start();
                            break;
                        case MAX_HEALTH_DOWN:
                            AttributeUtil.setMaxHealth(getPlayer(), defaultMaxHealth - MAX_HEALTH_DECREMENT.getValue());
                            break;
                        case COOLDOWN_UP:
                            cooldown = new Cooldown((int)(COOLDOWN.getValue() * (1+ COOLDOWN_INCREMENT.getValue()/100.0)), CooldownDecrease._75);
                            break;
                        case STUN:
                            Stun.apply(getParticipant(), TimeUnit.SECONDS, STUN_DURATION.getValue());
                            break;
                        default:
                        	break;
                    }
                    cooldown.start();
                }
            };
            check.start();
        }
    }

    private class HitBleedTimer extends AbilityTimer {
        private final AbstractGame.Participant participant;
        private final IHologram hologram;
        private int hit;

        public HitBleedTimer(AbstractGame.Participant participant) {
            this.participant = participant;
            this.hologram = NMS.newHologram(participant.getPlayer().getWorld(), participant.getPlayer().getLocation().clone().add(0,2.2,0));
            this.hologram.setText("§c".concat(Strings.repeat("☑",hit)).concat(Strings.repeat("☐", 3-hit)));
            this.hit = 0;
            this.setPeriod(TimeUnit.TICKS, 1);
        }

        public void onStart() {
            hologram.display(getPlayer());
        }

        public void run(int time) {
            hologram.teleport(participant.getPlayer().getLocation().clone().add(0,2.2,0));
        }

        public void onCountSet() {
            String text = "§c".concat(Strings.repeat("☑",hit)).concat(Strings.repeat("☐", BLEED_ATTACK_PREDICATE.getValue()-hit));
            hologram.setText(text);
        }

        public void onEnd() {
            this.hologram.hide(getPlayer());
        }

        public void onSilentEnd() {
            this.hologram.hide(getPlayer());
        }

        public void addHit() {
            hit++;

            if (hit == 1) {
                start();
            }

            setCount(10);

            if (hit == BLEED_ATTACK_PREDICATE.getValue()) {
                stop(false);
                Bleed.apply(participant, TimeUnit.SECONDS, BLEED_DURATION.getValue());
            }
        }
    }
}
