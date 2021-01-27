package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
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
        "§7패시브 §8- §c분산 타격§r: 같은 대상을 연속으로 공격할 때 1회 타격은 75%,",
        "  2회 타격은 100%, 3회 타격은 125%의 대미지로 공격합니다.",
        "  다른 이를 공격하거나, 일정 시간이 지날 시 이 수치는 초기화됩니다.",
        "  록 온의 쿨타임동안 이 수치는 각각 25%p 감소합니다.",
        "§7철괴 우클릭 §8- §c록 온§r: 사용 전 마지막에 타격한 플레이어만 공격할 수 있습니다.",
        "  분산 타격의 모든 대미지 수치는 대상 한정 75%p 상승합니다. $[COOLDOWN]",
        "  대상이 사망하거나 다시 사용할 경우 취소됩니다."
})
@Beta
public class OnlyHitYou extends CokesAbility implements ActiveHandler {
    private final HitTimer passive = new HitTimer();
    private final Cooldown cool = new Cooldown(60);

    public OnlyHitYou(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            passive.changeLockOn();
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
                    getPlayer().sendMessage("[§c!§r] 록 온 사용 전 마지막에 타격한 플레이어만 공격 가능합니다.");
                    e.setCancelled(true);
                    return;
                }
            }
            int info = (cool.isRunning() ? -1 : 0) + (passive.isLockOn() ? 3 : 0);
            e.setDamage(e.getDamage() * (1 + (passive.getHit() - 1)*0.25 + info*0.25));
            passive.addHit();
        }
    }

    private class HitTimer extends AbilityTimer {
        private final Note C = Note.natural(1, Note.Tone.C),
                E= Note.natural(1, Note.Tone.E),
                G= Note.natural(1, Note.Tone.G);

        private boolean lockOn = false;
        private Player targetPlayer = getPlayer();
        private int hit = 0;
        private final IHologram hologram;
        private String hologramMessage = "";

        private HitTimer() {
            super();
            this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
            this.hologram.setText(hologramMessage);
            this.register();
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

        private int getHit() {
            return hit;
        }

        private void addHit() {
            switch(hit){
                case 0:
                    SoundLib.BELL.playInstrument(getPlayer(), C);
                    hologramMessage = "§f♐";
                    hologram.display(getPlayer());
                case 1:
                    SoundLib.BELL.playInstrument(getPlayer(), E);
                    hologramMessage = "§c♐";
                case 2:
                    SoundLib.BELL.playInstrument(getPlayer(), G);
                    hologramMessage = "§f";
                    hologram.hide(getPlayer());
            }

            hit++;
            if (hit == 3) {
                hit = 0;
            }
        }

        private void changeLockOn() {
            lockOn = !lockOn;
        }

        private boolean isLockOn() {
            return lockOn;
        }
    }
}
