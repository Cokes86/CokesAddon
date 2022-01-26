package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "돌로레스", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 남에 의해 고통 받는 자: 플레이어에게서 받는 대미지가 $[RECEIVE_DAMAGE]% 증가합니다.",
        "  이때, 증가해서 받은 대미지를 축척합니다. (최대 $[MAX]대미지)",
        "패시브 - 남에게 고통을 주는 자: 사망 위기에 처했을 시 이를 무시하고 주변 $[RANGE] 이내",
        "  모든 플레이어에게 남에 의해 고통 받는 자로 축척한 대미지를",
        "  고정적으로 입힙니다. 이때, 이 능력으로 플레이어를 살해한 경우",
        "  그 인원당 체력을 10% 회복합니다. $[COOLDOWN]",
        "  주변에 플레이어가 없거나, 축척한 대미지가 $[PREDICATE_DAMAGE] 이하일 경우",
        "  발동하지 않으며, 쿨타임 종료 시 쿨타임이 기존보다 $[COOLDOWN_INCREASE_PERCENT]% 증가합니다."
})
@Beta
public class Dolores extends CokesAbility {
    private static final Config<Integer> RECEIVE_DAMAGE = new Config<>(Dolores.class, "받는_대미지_증가량(%)", 20, a -> a > 0);
    private static final Config<Integer> MAX = new Config<>(Dolores.class, "최대_축척대미지", 20, a -> a > 0);
    private static final Config<Integer> RANGE = new Config<>(Dolores.class, "범위", 5, a -> a > 1);
    private static final Config<Integer> COOLDOWN = new Config<>(Dolores.class, "쿨타임", 60, a -> a > 1);
    private static final Config<Integer> PREDICATE_DAMAGE = new Config<>(Dolores.class, "발동조건", 5, a -> a > 0);
    private static final Config<Integer> COOLDOWN_INCREASE_PERCENT = new Config<>(Dolores.class, "쿨타임_증가량(%)", 10, a -> a > 0);

    private double damage = 0;
    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                return !game.getDeathManager().isExcluded(entity.getUniqueId());
            }
            AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
            if (getGame() instanceof Teamable) {
                Teamable game = (Teamable) getGame();
                return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
            }
            return target.attributes().TARGETABLE.getValue();
        }
        return true;
    };
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._75).attachObserver(new SimpleTimer.Observer() {
        @Override
        public void onEnd() {
            cooldown.setCooldown((int) (cooldown.getCooldown() * (1 + COOLDOWN_INCREASE_PERCENT.getValue()/100.0)));
        }
    });

    public Dolores(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEnityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Entity){
                damager = (Entity) projectile.getShooter();
            }
        }

        if (damager instanceof Player && e.getEntity().equals(getPlayer())) {
            double original_damage = e.getDamage();
            e.setDamage(original_damage*(1 + RECEIVE_DAMAGE.getValue()/100.0));
            damage += original_damage*RECEIVE_DAMAGE.getValue() / 100.0;
            if (damage >= MAX.getValue()) {
                damage = MAX.getValue();
            }

            if (getPlayer().getHealth() - e.getDamage() <= 0 && damage >= PREDICATE_DAMAGE.getValue() && !cooldown.isRunning()) {
                List<Player> near = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation().clone(), RANGE.getValue(), RANGE.getValue(), predicate);
                if (near.size() > 0) {
                    e.setDamage(0);
                    for (Player player : near) {
                        new DoloresAttack(player, damage);
                    }
                }
                damage = 0;
            }
        }
    }

    class DoloresAttack extends AbilityTimer implements Listener {
        private final Player player;
        private final double damage;

        public DoloresAttack(Player player, double damage) {
            super(1);
            this.player = player;
            this.damage = damage;
            setPeriod(TimeUnit.TICKS, 2);
            start();
        }

        public Player getPlayer() {
            return player;
        }

        @Override
        protected void run(int cnt) {
            Damages.damageMagic(player, Dolores.this.getPlayer(), true, (float) damage);
        }

        @Override
        protected void onEnd() {
            HandlerList.unregisterAll(this);
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (event.getEntity().equals(player)) {
                Player dolores = Dolores.this.getPlayer();
                Healths.setHealth(dolores, dolores.getHealth() + AttributeUtil.getMaxHealth(dolores)*0.1);
            }
        }
    }
}
