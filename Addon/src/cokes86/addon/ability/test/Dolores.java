package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
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

import java.util.function.Predicate;

@AbilityManifest(name = "돌로레스", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 남에 의해 고통 받는 자: 플레이어에게서 받는 대미지가 $[RECEIVE_DAMAGE]% 증가합니다.",
        "  이때, 증가해서 받은 대미지를 축척합니다. (최대 $[MAX]대미지)",
        "패시브 - 남에게 고통을 주는 자: 사망 위기에 처했을 시 이를 무시하고 주변 $[RANGE] 이내",
        "  모든 플레이어에게 남에 의해 고통 받는 자로 축척한 대미지를",
        "  고정적으로 입힙니다. 이때, 이 능력으로 플레이어를 살해한 경우",
        "  그 인원당 체력을 10% 회복합니다. $[COOLDOWN]"
})
public class Dolores extends CokesAbility {
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
            e.setDamage(original_damage*1.2);
            damage += original_damage*0.2;

            if (getPlayer().getHealth() - e.getDamage() <= 0) {
                e.setDamage(0);
                for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation().clone(), 5, 5, predicate)) {
                    new DoloresAttack(player, damage);
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
