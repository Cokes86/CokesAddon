package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
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
        "철괴 우클릭시 아래 효과 중 하나를 영구적으로 얻습니다. $[COOLDOWN]",
        "이미 얻은 효과는 다시 나오지 않습니다.",
        "새로운 효과를 얻었을 경우, 기존 효과를 계속 유지합니다.",
        "철괴 좌클릭으로 자신이 얻은 효과를 알 수 있습니다."
})
public class Casino extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> COOLDOWN = new Config<>(Casino.class, "쿨타임", 60, Config.Condition.COOLDOWN);

    private final double defaultMaxHealth = Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
    private Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._75);
    private final Map<Effects, Boolean> effects = new HashMap<Effects, Boolean>() {
        {
            put(Effects.DAMAGE_UP, false);
            put(Effects.WITHER, false);
            put(Effects.RESISTANCE, false);
            put(Effects.HEAL, false);
            put(Effects.TWIST, false);
            put(Effects.REGAIN, false);
            put(Effects.HEALTH, false);
            put(Effects.BLEED, false);
            put(Effects.COOLDOWN_UP, false);
            put(Effects.FALL, false);
            put(Effects.STUN, false);
            put(Effects.FIRE_RESISTANCE, false);
            put(Effects.PROJECTILE, false);
        }
    };
    private final Map<UUID, Integer> hit = new HashMap<>();

    private final AbilityTimer wither = new AbilityTimer() {
        @Override
        protected void run(int count) {
            Damages.damageMagic(getPlayer(), null, true, 1);
        }
    }.setInitialDelay(TimeUnit.SECONDS, 4).setPeriod(TimeUnit.SECONDS, 4).register(),
    aim = new AbilityTimer() {
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
            if (effects.get(Effects.HEALTH)) {
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

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && e.getCause().equals(EntityDamageEvent.DamageCause.FALL) && effects.get(Effects.FALL)) {
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
            if (effects.get(Effects.DAMAGE_UP)) {
                e.setDamage(e.getDamage()+1);
            }
            if (effects.get(Effects.BLEED)) {
                if (predicate.test(e.getEntity()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
                    AbstractGame.Participant p = getGame().getParticipant(e.getEntity().getUniqueId());
                    int hit = this.hit.getOrDefault(e.getEntity().getUniqueId(), 0) + 1;
                    if (hit >= 4) {
                        Bleed.apply(p, TimeUnit.SECONDS, 1);
                        hit = 0;
                    }
                    this.hit.put(e.getEntity().getUniqueId(), hit);
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
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity().equals(getPlayer()) && effects.get(Effects.PROJECTILE)) {
            event.getProjectile().setVelocity(event.getProjectile().getVelocity().multiply(.75));
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
        DAMAGE_UP("주는 대미지 1 상승"),
        WITHER("영구 위더(4초마다)"),
        RESISTANCE("받는 대미지 1 감소"),
        HEAL("체력 2 회복"),
        TWIST("시야 뒤틀림"),
        REGAIN("회복량 0.2배 증가"),
        HEALTH("최대 체력 2 감소"),
        BLEED("4회 타격 시 출혈 부여"),
        COOLDOWN_UP("쿨타임 50% 증가"),
        FALL("낙하대미지 무시"),
        STUN("스턴 2초 부여"),
        FIRE_RESISTANCE("화염저항"),
        PROJECTILE("발사체 초기속도 25% 감소");

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
                    getPlayer().sendMessage("[Casino] §e"+result.getName()+ " §r효과를 얻었습니다.");

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
                        case HEALTH:
                            Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(defaultMaxHealth - 2);
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
}
