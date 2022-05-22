package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AbilityManifest(name = "져스틴", rank = AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브§8 -§c 두가지 인격§f: 약 $[PERIOD] 간격으로 자신의 인격이 뒤바뀝니다.",
        "  이 주기는 불안정한 인격을 다루기에 최대 50%까지 오차가 발생합니다.",
        "  인격에 따라 각기 다른 효과를 부여받습니다.",
        "  [§b1인격§f] -",
        "  [§52인격§f] 타 플레이어에게 받는 근거리 대미지가 §c$[TWO_PERSON_DAMAGE_INCREASE]% 증가합니다.",
        "§7검으로 공격 시§8 -§c슬래시§f: 인격에 따라 각기 다른 효과를 가집니다.",
        "  [§b1인격§f] 상대방에게 주는 대미지가 $[DAMAGE]% 감소합니다.",
        "  [§52인격§f] 상대방에게 §4흑심 카운터§f를 1개 부여합니다. (최대 $[MAX_COUNTER]개)",
        "§7검 우클릭§8 - §c이거나 받아라§f: 인격에 따라 각기 다른 효과를 가집니다.",
        "  [§b1인격§f] §c슬래시§f 이후 §c0.5초 §f이내에 사용 시, 적을 §b밀쳐내고§f 감소했던 대미지를 줍니다.",
        "  [§52인격§f] §4흑심 카운터§f를 가지고 있던 플레이어에게 개당 $[GET_THIS_DAMAGE]의 §b관통 마법 대미지§f를 줍니다. $[GET_THIS_COOLDOWN]",
        "§7철괴 우클릭§8 - §c탈출§f: 자신의 인격을 강제로 변경합니다. $[ESCAPE_COOLDOWN]",
        "  이때, 바뀐 인격은 더욱 불안정해 주기가 §c반으로 감소합니다."
}, summarize = {
    "져스틴은 2개의 인격을 가집니다. 인격은 약 $[PERIOD]의 주기로 바뀝니다.",
    "1인격 상태에서는 §7검으로 공격 시 §c대미지가 $[DAMAGE]% 감소§f하지만",
    "0.5초 이내 §7검을 들고 우클릭§f 하면 적을 밀쳐내면서 §b감소했던 대미지를 입힙니다.",
    "2인격 상태에서는 §7검으로 공격 시§f 상대에게 §4흑심 카운터§f를 1개씩, 최대 $[MAX_COUNT]개 남깁니다.",
    "이후 §7검을 들고 우클릭§f 하면 흑심 카운터 개당 $[GET_THIS_DAMAGE]의 §b관통 마법 대미지§f를 줍니다.",
    "공통적으로 §7철괴를 우클릭§f하면 인격이 바뀌지만, 다음 주기가 반으로 감소합니다."
})
public class Justin extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> PERIOD = Config.of(Justin.class, "period", 45, Config.Condition.TIME,
        "# 인격 변경 주기",
        "# 기본값 : 45 (초)");
    private static final Config<Integer> MAX_COUNTER = Config.of(Justin.class, "max-count", 10, FunctionalInterfaceUnit.upper(1),
        "# 흑심카운터 최대치",
        "# 기본값 : 10");
    private static final Config<Integer> GET_THIS_COOLDOWN = Config.of(Justin.class, "get-this-cooldown", 60, Config.Condition.COOLDOWN,
        "# 이거나 받아라 2인격 쿨타임",
        "# 기본값 : 60 (초)");
    private static final Config<Integer> ESCAPE_COOLDOWN = Config.of(Justin.class, "escape-cooldown", 45, Config.Condition.COOLDOWN,
        "# 탈출 쿨타임",
        "# 기본값 : 45 (초)");
    private static final Config<Double> DAMAGE = Config.of(Justin.class, "giving-damage-decrement", 30.0, FunctionalInterfaceUnit.positive(),
        "# 슬래시 1인격 주는 대미지 감소량",
        "# 기본값 : 30.0 (%)");
    private static final Config<Float> GET_THIS_DAMAGE = Config.of(Justin.class, "fix-damage", 2.0f, FunctionalInterfaceUnit.positive(),
        "# 이거나 받아라 2인격 고정 대미지량",
        "# 기본값 : 2.0");
    private static final Config<Double> TWO_PERSON_DAMAGE_INCREASE = Config.of(Justin.class, "receive-damage-increment", 20.0, FunctionalInterfaceUnit.positive(),
        "# 두가지 인격 2인격 받는 대미지 증가량",
        "# 기본값 : 20.0 (%)");

    private static final Set<Material> swords = CokesUtil.getSwords();
    private NormalTimer normalTimer = new NormalTimer(getPlayer(), 0);
    private boolean madness = false;
    private final Random r = new Random();
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();
    private final Map<AbstractGame.Participant, DarknessStack> stackMap = new ConcurrentHashMap<>();
    private final Cooldown getThisCooldown = new Cooldown(GET_THIS_COOLDOWN.getValue());
    private final Cooldown escapeCooldown = new Cooldown(ESCAPE_COOLDOWN.getValue());
    private final AbilityTimer justinChangeTimer = new AbilityTimer(PERIOD.getValue()*20) {
        @Override
        protected void onStart() {
            int back = r.nextInt(PERIOD.getValue()*10) - PERIOD.getValue()*5;
            setCount(getMaximumCount() + back);
        }

        @Override
        protected void run(int count) {
            channel.update(madness ? "§52인격" : "§b1인격");

            if (count % 20 == 0) {
                switch (count / 20) {
                    case 3: case 2: case 1: {
                        SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.C));
                    }
                }
            }
        }

        @Override
        protected void onEnd() {
            madness = !madness;
            start();
            SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.C));
        }
    }.setPeriod(TimeUnit.TICKS, 1).setBehavior(RestrictionBehavior.PAUSE_RESUME);

    public Justin(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (swords.contains(material) && clickType.equals(ClickType.RIGHT_CLICK)) {
            if (!madness) {
                if (normalTimer.isRunning()) {
                    normalTimer.getDamageable().damage(normalTimer.getDamage(), getPlayer());
                    Location playerLocation = getPlayer().getLocation().clone();
                    normalTimer.getDamageable().setVelocity(normalTimer.getDamageable().getLocation().toVector().subtract(playerLocation.toVector()).normalize().multiply(4.5).setY(0));
                    normalTimer.stop(false);
                    return true;
                }
            } else {
                if (!stackMap.isEmpty() && !getThisCooldown.isCooldown()) {
                    for (DarknessStack stack : stackMap.values()) {
                        stack.startDamage();
                    }
                    stackMap.clear();
                }
            }
        } else if (material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.RIGHT_CLICK) && !escapeCooldown.isCooldown()) {
            justinChangeTimer.stop(false);
            justinChangeTimer.setCount(justinChangeTimer.getCount()/2);
            return escapeCooldown.start();
        }
        return false;
    }

    @Override
    public boolean usesMaterial(Material material) {
        return super.usesMaterial(material) || swords.contains(material);
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getDamager() != null && e.getDamager().equals(getPlayer()) && swords.contains(getPlayer().getInventory().getItemInMainHand().getType()) && !e.getEntity().equals(getPlayer())) {
            if (!madness) {
                if (normalTimer.isRunning()) {
                    normalTimer.stop(false);
                    if (e.getEntity().equals(normalTimer.getDamageable())) {
                        return;
                    }
                }
                double damage = e.getDamage();
                double decrease = DAMAGE.getValue()/100.0;
                e.setDamage(damage * ( 1 - decrease ));
                normalTimer = new NormalTimer((Damageable) e.getEntity(), damage*decrease);
                normalTimer.start();
            } else {
                if (e.getCause() == DamageCause.MAGIC) return;
                if (getGame().isGameStarted() && e.getEntity() instanceof Player) {
                    if (getGame().isParticipating(e.getEntity().getUniqueId())) {
                        final AbstractGame.Participant victim = getGame().getParticipant(e.getEntity().getUniqueId());
                        if (!stackMap.containsKey(victim)) {
                            stackMap.put(victim, new DarknessStack(victim));
                        } else {
                            stackMap.get(victim).addStack();
                        }
                    }
                }
            }
        }

        if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
            e.setDamage(e.getDamage()*TWO_PERSON_DAMAGE_INCREASE.getValue()/100.0);
        }
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            justinChangeTimer.start();
        }
    }

    class NormalTimer extends AbilityTimer {
        private final Damageable damageable;
        private final double damage;

        public NormalTimer(Damageable damageable, double damage){
            super(10);
            this.damageable = damageable;
            this.damage = damage;
            setPeriod(TimeUnit.TICKS, 1);
        }

        public Damageable getDamageable() {
            return damageable;
        }

        public double getDamage() {
            return damage;
        }
    }

    private class DarknessStack extends AbilityTimer {
        private final AbstractGame.Participant target;
        private final IHologram hologram;
        private final int maxCounter = MAX_COUNTER.getValue();
        private int stack;

        private DarknessStack(AbstractGame.Participant target) {
            super();
            this.setPeriod(TimeUnit.TICKS, 1);
            this.target = target;
            final Player targetPlayer = target.getPlayer();
            this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
            this.hologram.setText(Strings.repeat("§4⚛", stack));
            this.hologram.display(getPlayer());
            this.stack = 1;
            this.start();
            target.getPlayer().sendMessage("[져스틴] 누군가가 §4흑심§f을 남기기 시작했습니다.");
        }

        @Override
        protected void run(int arg0) {
            this.hologram.setText(Strings.repeat("§4⚛", stack));
            final Player targetPlayer = target.getPlayer();
            hologram.teleport(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ(), targetPlayer.getLocation().getYaw(), 0);
        }

        @Override
        protected void onEnd() {
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            hologram.hide(getPlayer());
            hologram.unregister();
        }

        private void addStack() {
            if (stack < maxCounter) {
                stack++;
            }
        }

        public void startDamage() {
            new AbilityTimer(stack) {
                @Override
                protected void run(int count) {
                    Damages.damageMagic(target.getPlayer(), getPlayer(), true, GET_THIS_DAMAGE.getValue());
                    target.getPlayer().setNoDamageTicks(0);
                }
            }.setPeriod(TimeUnit.TICKS, 10).start();
        }
    }
}
