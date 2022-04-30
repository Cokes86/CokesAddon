package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.arrow.ArrowUtil;
import com.google.common.base.Strings;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableMap;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

@AbilityManifest(name="카지노", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 우클릭시 랜덤한 효과 하나를 영구히 부여합니다. $[COOLDOWN]",
        "이미 부여한 효과는 다시 나오지 않습니다.",
        "철괴 좌클릭으로 자신에게 부여된 효과를 알 수 있습니다."
})
public class Casino extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> COOLDOWN = Config.of(Casino.class, "쿨타임", 60, Config.Condition.COOLDOWN);

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
            Damages.damageMagic(getPlayer(), null, true, 1);
        }
    }.setInitialDelay(TimeUnit.SECONDS, 4).setPeriod(TimeUnit.SECONDS, 4).register();
    private final AbilityTimer aim = new AbilityTimer() {
        @Override
        protected void run(int count) {
            Player target = getFarthestEntity(getPlayer().getLocation(), predicate);
            if (target != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Vector direction = target.getEyeLocation().toVector().subtract(getPlayer().getEyeLocation().toVector());
                    NMS.rotateHead(player, getPlayer(), LocationUtil.getYaw(direction), LocationUtil.getPitch(direction));
                }
            }
        }
    }.setInitialDelay(TimeUnit.SECONDS, 10).setPeriod(TimeUnit.SECONDS, 10).register();
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
                if (effects.containsValue(false)) {
                    return infoTimer.start();
                }
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

    @SubscribeEvent(childs = {EntityDamageByBlockEvent.class})
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && e.getCause().equals(EntityDamageEvent.DamageCause.FALL) && effects.get(Effects.IGNORE_FALL)) {
            e.setCancelled(true);
            getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
            SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
        } else if (e.getEntity().equals(getPlayer()) && (e.getCause().equals(EntityDamageEvent.DamageCause.FIRE) || e.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)) && effects.get(Effects.FIRE_RESISTANCE)) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        Entity attacker = e.getDamager();
        if (attacker instanceof Arrow) {
            Arrow arrow = (Arrow) attacker;
            if (arrow.getShooter() instanceof Entity) {
                attacker = (Entity) arrow.getShooter();
            }
        }

        if (e.getEntity().equals(getPlayer()) && effects.get(Effects.RESISTANCE)) {
            e.setDamage(e.getDamage()-1);
        }
        if (attacker.equals(getPlayer())) {
            if (effects.get(Effects.DAMAGE_INCREMENT)) {
                e.setDamage(e.getDamage()+1);
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
            e.setAmount(e.getAmount() * 1.2);
        }
    }

    @SubscribeEvent
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter().equals(getPlayer()) && effects.get(Effects.NO_CRITICAL) && NMS.isArrow(event.getEntity())) {
            ArrowUtil.of(event.getEntity()).setCritical(false);
        }
    }

    private Player getFarthestEntity(Location center, Predicate<Entity> predicate) {
        double distance = Double.MIN_VALUE;
        Player current = null;

        Location centerLocation = center.clone();
        if (center.getWorld() == null)
            return null;
        for (Entity e : center.getWorld().getEntities()) {
            if (e instanceof Player) {
                Player entity = (Player) e;
                double compare = centerLocation.distanceSquared(entity.getLocation());
                if (compare > distance && (predicate == null || predicate.test(entity))) {
                    distance = compare;
                    current = entity;
                }
            }
        }

        return current;
    }

    private enum Effects {
        DAMAGE_INCREMENT("주는 대미지 1 증가"),
        WITHER("4초마다 1의 고정 마법 대미지 부여"),
        RESISTANCE("받는 대미지 1 감소"),
        HEAL("체력 2 즉시 회복"),
        TWIST("10초마다 시야 뒤틀림"),
        REGAIN("회복량 0.2배 증가"),
        MAX_HEALTH_DOWN("최대 체력 2 감소"),
        BLEED("4회 타격 시 출혈 부여"),
        COOLDOWN_UP("쿨타임 50% 증가"),
        IGNORE_FALL("낙하대미지 무시"),
        STUN("스턴 2초 부여"),
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
                            Healths.setHealth(getPlayer(), getPlayer().getHealth() + 2);
                            break;
                        case TWIST:
                            aim.start();
                            break;
                        case MAX_HEALTH_DOWN:
                            AttributeUtil.setMaxHealth(getPlayer(), defaultMaxHealth - 2);
                            break;
                        case COOLDOWN_UP:
                            cooldown = new Cooldown((int)(COOLDOWN.getValue() * 1.5), CooldownDecrease._75);
                            break;
                        case STUN:
                            Stun.apply(getParticipant(), TimeUnit.SECONDS, 2);
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
            String text = "§c".concat(Strings.repeat("☑",hit)).concat(Strings.repeat("☐", 3-hit));
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

            if (hit == 4) {
                stop(false);
                Bleed.apply(participant, TimeUnit.SECONDS, 1);
            }
        }
    }
}