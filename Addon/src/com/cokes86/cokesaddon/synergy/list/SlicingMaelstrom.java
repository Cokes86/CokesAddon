package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
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
        "$[range]블럭 내에 0.5초마다 $[DAMAGE]의 고정 대미지를 주는 번개를 만들어",
        "범위 내 모든 플레이어에게 총 $[LIGHTNING_COUNT]번 내리꽂아버립니다. $[cooldown]",
        "번개를 3회 맞을 시 스턴이 1초 부여됩니다.",
        "번개를 2회 맞을 때 마다 해당 능력으로 받는 고정 대미지가 $[ADDITIONAL] 증가합니다."
})
public class SlicingMaelstrom extends CokesSynergy implements ActiveHandler {
    public static final Config<Integer> cooldown = Config.of(SlicingMaelstrom.class, "쿨타임", 60, Config.Condition.COOLDOWN);
    public static final Config<Integer> LIGHTNING_COUNT = Config.of(SlicingMaelstrom.class, "반복횟수", 6, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> range = Config.of(SlicingMaelstrom.class, "범위", 10, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> DAMAGE = Config.of(SlicingMaelstrom.class, "대미지", 1, FunctionalInterfaceUnit.positive());
    public static final Config<Integer> ADDITIONAL = Config.of(SlicingMaelstrom.class, "추가대미지", 1, FunctionalInterfaceUnit.positive());
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
            super(LIGHTNING_COUNT.getValue(), cool);
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
                int damage = (hitted/2)*ADDITIONAL.getValue() + DAMAGE.getValue();
                Damages.damageFixed(player, getPlayer(), damage);
                hit.put(player, hitted+1);
                hitted+=1;
                player.getWorld().strikeLightningEffect(player.getLocation());

                if (hitted % 3 == 0) {
                    Stun.apply(getGame().getParticipant(player), TimeUnit.SECONDS, 1);
                }
            }

            for (Location location : Circle.iteratorOf(getPlayer().getLocation(), range.getValue(), range.getValue()*20).iterable()){
                location.setY(LocationUtil.getFloorYAt(Objects.requireNonNull(location.getWorld()), getPlayer().getLocation().getY(), location.getBlockX(), location.getBlockZ()) + 0.1);
                ParticleLib.REDSTONE.spawnParticle(location, new RGB(1, 1, 255));
            }
        }
    }
}
