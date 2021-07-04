package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "권투선수", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8- §c연속 펀치§f: 상대방을 근접 공격할 시 50%씩 두 번 나누어 공격을 합니다.",
        "  $[percentage]% 확률로 상대방의 공격을 회피할 수 있습니다.",
        "  상대방을 공격하고 나서 8틱동안 상대방을 공격할 수 없습니다.",
        "§7철괴 타게팅 §8- §c스트레이트§f: $[damage]의 대미지와 함께 멀리 밀쳐냅니다. $[right_cool]",
        "§7철괴 좌클릭 §8- §c나도 좀 쉬자고§f: 휴식상태에 돌입해 $[duration]마다 체력 1씩 회복합니다.",
        "  이때 자신은 행동불능 효과를 받고 공격을 받거나 다시 철괴 좌클릭 시",
        "  해당 효과를 취소하고 움직일 수 있습니다. $[left_cool]",
        "  공격을 회피하였을 경우, 이 효과는 취소되지 않습니다."
})
public class Boxer extends CokesAbility implements ActiveHandler, TargetHandler {
    private static final Config<Integer> percentage = new Config<Integer>(Boxer.class, "회피확률(%)", 20) {
    }, damage = new Config<Integer>(Boxer.class, "스트레이트.대미지", 7) {
    }, right_cool = new Config<Integer>(Boxer.class, "스트레이트.쿨타임", 30, Config.Condition.COOLDOWN) {
    }, left_cool = new Config<Integer>(Boxer.class, "휴식.쿨타임", 90, Config.Condition.COOLDOWN) {
    }, duration = new Config<Integer>(Boxer.class, "휴식.회복주기", 2, Config.Condition.TIME) {
    };
    
    private final Predicate<Entity> predicate = entity -> {
		if (entity == null || entity.equals(getPlayer())) return false;
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
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};

    private final Cooldown right_cooldown = new Cooldown(right_cool.getValue(), "스트레이트");
    private final Cooldown left_cooldown = new Cooldown(left_cool.getValue(), "나도 좀 쉬자고");
    private final AbilityTimer rest = new AbilityTimer(1) {
        @Override
        protected void onStart() {
            Rooted.apply(getParticipant(), TimeUnit.TICKS, duration.getValue()*40);
        }

        @Override
        protected void run(int count) {
            Healths.setHealth(getPlayer(), getPlayer().getHealth() + 1);
        }

        @Override
        protected void onEnd() {
            this.start();
        }

        @Override
        protected void onSilentEnd() {
            AbstractGame.Effect root = getParticipant().getPrimaryEffect(Rooted.registration);
            if(root != null)
                root.stop(true);
        }
    }.setInitialDelay(TimeUnit.SECONDS, duration.getValue()).register();
    private final AttackTimer attack = new AttackTimer();

    public Boxer(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !left_cooldown.isCooldown()) {
            if (rest.isRunning()) {
                rest.stop(true);
                left_cooldown.start();
            } else {
                rest.start();
                return true;
            }
        }
        return false;
    }

    @Override
    public void TargetSkill(Material material, LivingEntity entity) {
        if(material == Material.IRON_INGOT && !right_cooldown.isCooldown() && predicate.test(entity)) {
            entity.damage(damage.getValue());
            Location playerLocation = getPlayer().getLocation().clone();
            entity.setVelocity(entity.getLocation().toVector().subtract(playerLocation.toVector()).normalize().multiply(4.5).setY(0));
            SoundLib.ENTITY_GENERIC_EXPLODE.playSound(playerLocation);
            ParticleLib.EXPLOSION_NORMAL.spawnParticle(playerLocation);
            right_cooldown.start();
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof LivingEntity && !attack.isRunning()) {
            double damage = e.getDamage()/2;
            e.setDamage(damage);
            attack.start((LivingEntity) e.getEntity(), damage);
        }

        else if (e.getEntity().equals(getPlayer())) {
            final Random r = new Random();
            if (r.nextDouble() < percentage.getValue() / 100.00) {
                SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1.0F, 1.2F);
                e.setCancelled(true);
            } else {
                if (rest.isRunning()) {
                    rest.stop(true);
                    left_cooldown.start();
                }
            }
        }
    }

    class AttackTimer extends AbilityTimer {
        private LivingEntity entity;
        private double damage;

        public AttackTimer() {
            super(1);
            setInitialDelay(TimeUnit.TICKS, 3);
        }

        public boolean start(LivingEntity entity, double damage) {
            this.entity = entity;
            this.damage = damage;
            return start();
        }

        @Override
        protected void run(int count) {
            if (entity.isDead()) {
                stop(true);
                return;
            }
            (entity).setNoDamageTicks(0);
            (entity).damage(damage, getPlayer());
        }
    }
}
