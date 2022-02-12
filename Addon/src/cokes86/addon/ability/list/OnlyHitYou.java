package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "너만 때린다", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8- §c분산 타격§f: 같은 대상을 연속으로 공격할 시 대미지가 1회 타격은 $[FIRST_HIT]%,",
        "  2회 타격은 $[SECOND_HIT]%, 3회 타격은 $[THIRD_HIT]%로 변경합니다.",
        "  다른 이를 공격하거나, 일정 시간이 지날 시 이 수치는 초기화됩니다.",
        "  §c록 온§f 쿨타임동안 이 수치는 각각 $[COOLDOWN_DECREASE]%p 감소합니다.",
        "§7철괴 우클릭 §8- §c록 온§f: 사용 전 마지막에 타격한 플레이어만 공격할 수 있습니다.",
        "  분산 타격의 모든 대미지 수치는 대상 한정 $[LOCK_ON]%p 상승합니다. $[COOLDOWN]",
        "  대상이 사망하거나 다시 사용할 경우 취소됩니다."
})
public class OnlyHitYou extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> COOLDOWN = new Config<>(OnlyHitYou.class, "쿨타임", 60, Config.Condition.COOLDOWN);
    private static final Config<Integer> FIRST_HIT = new Config<>(OnlyHitYou.class, "첫번째_타격_배율(%)", 75, PredicateUnit.positive(Integer.class));
    private static final Config<Integer> SECOND_HIT = new Config<>(OnlyHitYou.class, "두번째_타격_배율(%)", 100, PredicateUnit.positive(Integer.class));
    private static final Config<Integer> THIRD_HIT = new Config<>(OnlyHitYou.class, "세번째_타격_배율(%)", 125, PredicateUnit.positive(Integer.class));
    private static final Config<Integer> LOCK_ON = new Config<>(OnlyHitYou.class, "록_온_추가_배율(%)", 75, PredicateUnit.positive(Integer.class));
    private static final Config<Integer> COOLDOWN_DECREASE = new Config<>(OnlyHitYou.class, "쿨타임_감소_배율(%)", 25, PredicateUnit.positive(Integer.class));

    private final HitTimer passive = new HitTimer();
    private final Cooldown cool = new Cooldown(COOLDOWN.getValue());

    public OnlyHitYou(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cool.isCooldown()) {
            passive.changeLockOn();
            if (!passive.isLockOn()) {
                getPlayer().sendMessage("[§c!§f] 록 온이 해제되었습니다.");
            } else {
                getPlayer().sendMessage("[§c!§f] 록 온!");
            }
            return passive.isLockOn();
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity attacker = e.getDamager();
        if (attacker instanceof Projectile) {
            Projectile projectile = (Projectile) attacker;
            if (projectile.getShooter() instanceof Entity) {
                attacker = (Entity) projectile.getShooter();
            }
        }

        if (attacker.equals(getPlayer()) && e.getEntity() instanceof Player) {
            Player target = (Player) e.getEntity();
            if (!passive.getTargetPlayer().equals(target)) {
                if (!passive.isLockOn()) {
                    passive.setTargetPlayer(target);
                } else {
                    getPlayer().sendMessage("[§c!§f] 록 온 사용 전 마지막에 타격한 플레이어만 공격 가능합니다.");
                    e.setCancelled(true);
                    return;
                }
            }
            passive.addHit();
            double multiply = passive.getMultiply() + (passive.isLockOn() ? LOCK_ON.getValue() : 0) - (cool.isRunning() ? COOLDOWN_DECREASE.getValue() : 0);
            e.setDamage(e.getDamage() * multiply/100.0f);
        }
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {
        if (e.getParticipant().getPlayer().equals(passive.getTargetPlayer()) && passive.isLockOn()) {
            passive.changeLockOn();
            getPlayer().sendMessage("[§c!§f] 대상이 사망하여 록 온이 자동으로 해제됩니다.");
        }
    }

    private class HitTimer extends AbilityTimer {
        private final Note C = Note.natural(0, Note.Tone.C),
                E= Note.natural(0, Note.Tone.E),
                G= Note.natural(1, Note.Tone.G);

        private boolean lockOn = false;
        private Player targetPlayer = getPlayer();
        private int hit = 0;
        private final IHologram hologram;
        private String hologramMessage = "§f";
        private int multiply = 0;

        private HitTimer() {
            super();
            this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
            this.hologram.setText(hologramMessage);
            this.register();
            this.setPeriod(TimeUnit.TICKS, 1);
        }

        public void run(int count) {
            this.hologram.teleport(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ(), targetPlayer.getLocation().getYaw(), 0);
            this.hologram.setText(hologramMessage);
        }

        @Override
        protected void onEnd() {
            hologram.unregister();
        }

        @Override
        protected void onSilentEnd() {
            hologram.unregister();
        }

        private Player getTargetPlayer() {
            return targetPlayer;
        }

        private void setTargetPlayer(Player targetPlayer) {
            this.targetPlayer = targetPlayer;
            this.hit = 0;
        }

        private void addHit() {
            switch(++hit){
                case 1: {
                    SoundLib.BELL.playInstrument(getPlayer(), C);
                    hologramMessage = "§f♐";
                    hologram.display(getPlayer());
                    multiply = FIRST_HIT.getValue();
                    break;
                }
                case 2: {
                    SoundLib.BELL.playInstrument(getPlayer(), E);
                    hologramMessage = "§c♐";
                    multiply = SECOND_HIT.getValue();
                    break;
                }
                case 3: {
                    SoundLib.BELL.playInstrument(getPlayer(), G);
                    hologramMessage = "§f";
                    hologram.hide(getPlayer());
                    multiply = THIRD_HIT.getValue();
                    hit = 0;
                    break;
                }
            }
        }

        private void changeLockOn() {
            lockOn = !lockOn;
            if (!lockOn) cool.start();
        }

        private boolean isLockOn() {
            return lockOn;
        }

        private int getMultiply() {
            return multiply;
        }
    }
}
