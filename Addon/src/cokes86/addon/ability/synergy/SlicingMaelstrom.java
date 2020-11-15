package cokes86.addon.ability.synergy;

import cokes86.addon.ability.CokesSynergy;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@AbilityManifest(name="날카로운 소용돌이", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 우클릭 시 강력한 소용돌이를 생성해 자기 주변을 휩쓸어버립니다.",
        "$[range]블럭 내에 0.5초마다 고정 1의 대미지를 주는 번개를 만들어",
        "범위 내 모든 플레이어에게 총 6번 내리꽂아버립니다. $[cooldown]",
        "번개를 3회 맞을 시 스턴이 1초 부여됩니다.",
        "번개를 2번 맞을 때 마다 입는 피해가 1씩 상승합니다."
})
@Beta
public class SlicingMaelstrom extends CokesSynergy implements ActiveHandler {
    public static final Config<Integer> cooldown = new Config<Integer>(SlicingMaelstrom.class, "쿨타임", 60, 1) {
        @Override
        public boolean condition(Integer value) {
            return value > 0;
        }
    };
    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
            }
            if (getGame() instanceof Teamable) {
                Teamable game = (Teamable) getGame();
                return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
            }
            if (entity.isDead()) return false;
            if (!Damages.canDamage(entity, getPlayer(), EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1)) return false;
            return target.attributes().TARGETABLE.getValue();
        }
        return true;
    };

    public static final Config<Integer> range = new Config<Integer>(SlicingMaelstrom.class, "범위", 10) {
        @Override
        public boolean condition(Integer integer) {
            return integer > 0;
        }
    };

    public final Cooldown cool = new Cooldown(cooldown.getValue());
    public final Maelstrom maelstrom = new Maelstrom();

    public SlicingMaelstrom(AbstractGame.Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cool.isCooldown() && !maelstrom.isDuration()) {
            return maelstrom.start();
        }
        return false;
    }

    class Maelstrom extends Duration {
        private final Map<Player, Integer> hit = new HashMap<>();

        public Maelstrom() {
            super(6, cool);
            this.setPeriod(TimeUnit.TICKS, 10);
        }

        @Override
        protected void onDurationEnd() {
            onDurationSilentEnd();
        }

        @Override
        protected void onDurationSilentEnd() {
            hit.clear();
        }

        @Override
        protected void onDurationProcess(int i) {
            List<Player> ps = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
            for (Player player : ps) {
                int hitted = hit.getOrDefault(player, 0);
                Damages.damageFixed(player, getPlayer(), hitted/2 + 1);
                hit.put(player, hitted+1);
                hitted+=1;
                player.getWorld().strikeLightningEffect(player.getLocation());

                if (hitted % 3 == 0) {
                    Stun.apply(getGame().getParticipant(player), TimeUnit.SECONDS, 1);
                }
            }

            for (Location location : Circle.iteratorOf(getPlayer().getLocation(), range.getValue(), range.getValue()*20).iterable()){
                location.setY(LocationUtil.getFloorYAt(Objects.requireNonNull(location.getWorld()), getPlayer().getLocation().getY(), location.getBlockX(), location.getBlockZ()) + 0.1);
                ParticleLib.REDSTONE.spawnParticle(location, new ParticleLib.RGB(1, 1, 255));
            }
        }
    }
}
