package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.effect.list.Seal;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AbilityManifest(name = "봉인자", rank = Rank.L, species = Species.HUMAN, explain = {
		"철괴로 상대방을 우클릭할 시 상대방의 능력을 $[duration]간 비활성화시킵니다. $[cool]",
		"이미 비활성화되어있는 능력에겐 이 능력이 발동하지 않습니다.",
		"봉인한 능력의 등급에 따라 자신에게 각종 효과를 $[duration]간 부여합니다.",
		"§eC 등급§f: 나약함1 | §bB 등급§f: 재생1 | §aA 등급§f: 힘1 | §dS 등급§f: 힘2 | §6L 등급§f: 힘2, 저항1",
		"§7특정 능력을 봉인하면 어떤 일이?!"
})
@NotAvailable({AbstractTripleMix.class})
public class Sealer extends CokesAbility implements TargetHandler {
	private static final Config<Integer> cool = Config.of(Sealer.class, "쿨타임", 60, Config.Condition.COOLDOWN);
	private static final Config<Integer> duration = Config.of(Sealer.class, "지속시간", 7, Config.Condition.TIME);

	private final Set<Player> synergy = new HashSet<>();
	private final Map<Rank, Integer> rank = ImmutableMap.<Rank, Integer>builder().put(Rank.C, 1).put(Rank.B, 2).put(Rank.A, 3).put(Rank.S, 4).put(Rank.L, 5).put(Rank.SPECIAL, 6).build();

	private final Cooldown c = new Cooldown(cool.getValue(), CooldownDecrease._50);
	private final SealTimer t = new SealTimer();

	public Sealer(Participant participant) {
		super(participant);
	}

	@Override
	public void TargetSkill(Material mt, LivingEntity entity) {
		if (mt.equals(Material.IRON_INGOT) && !t.isRunning() && !c.isCooldown()) {
			if (entity instanceof Player) {
				final Player p = (Player) entity;
				final Participant target = getGame().getParticipant(p);
				t.start(target);
			}
		}
	}

	class SealTimer extends Duration implements Listener {
		private Participant target;

		public SealTimer() {
			super(duration.getValue(), c);
		}

		public boolean start(Participant target) {
			this.target = target;
			return this.start();
		}

		@Override
		public void onDurationStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			int rankNum;
			if (target instanceof AbstractMix.MixParticipant) {
				AbstractMix.MixParticipant participant = (AbstractMix.MixParticipant) target;
				Mix mix = participant.getAbility();
				if (mix == null) {
					this.stop(true);
					getPlayer().sendMessage("해당 플레이어의 능력이 존재하지 않습니다.");
					c.setCount(0);
					return;
				}
				if (!mix.hasAbility() || mix.isRestricted()) {
					this.stop(true);
					getPlayer().sendMessage("해당 플레이어의 능력이 존재하지 않습니다.");
					c.setCount(0);
					return;
				}

				if (mix.hasSynergy()) {
					rankNum = 100;
				} else {
					int a = mix.getFirst() != null ? rank.getOrDefault(mix.getFirst().getRank(), 0) : 0;
					int b = mix.getSecond() != null ? rank.getOrDefault(mix.getSecond().getRank(), 0) : 0;
					rankNum = (a+b)/2;
				}
			} else {
				AbilityBase ability = target.getAbility();
				if (ability == null || ability.isRestricted()) {
					this.stop(true);
					getPlayer().sendMessage("해당 플레이어의 능력이 존재하지 않습니다.");
					c.setCount(0);
					return;
				}
				rankNum = rank.getOrDefault(ability.getRank(), 0);
			}

			Seal.apply(target, TimeUnit.TICKS, duration.getValue()*20);
			switch(rankNum){
				case 100:
					getPlayer().sendMessage("시너지 봉인! 대미지가 80% 감소하는 대신 방어를 무시하고 공격합니다!");
					synergy.add(target.getPlayer());
					break;
				case 1:
					getPlayer().sendMessage( Rank.C.getRankName()+" §f봉인! 나약함1 버프를 부여합니다.");
					PotionEffects.WEAKNESS.addPotionEffect(getPlayer(), duration.getValue()*20, 0, true);
					break;
				case 2:
					getPlayer().sendMessage( Rank.B.getRankName()+" §f봉인! 재생2 버프를 부여합니다.");
					PotionEffects.REGENERATION.addPotionEffect(getPlayer(), duration.getValue()*20, 1, true);
					break;
				case 3:
					getPlayer().sendMessage( Rank.A.getRankName()+" §f봉인! 힘1 버프를 부여합니다.");
					PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), duration.getValue()*20, 0, true);
					break;
				case 4:
					getPlayer().sendMessage( Rank.S.getRankName()+" §f봉인! 힘2 버프를 부여합니다.");
					PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), duration.getValue()*20, 1, true);
					break;
				case 5:
					getPlayer().sendMessage( Rank.L.getRankName()+" §f봉인! 힘2, 저항1 버프를 부여합니다.");
					PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), duration.getValue()*20, 1, true);
					PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), duration.getValue()*20, 0, true);
					break;
				case 6:
					getPlayer().sendMessage( Rank.SPECIAL.getRankName()+" §f봉인! 뭘 줘야해???");
					break;
			}
		}

		@Override
		protected void onDurationProcess(int count) {
		}

		@Override
		protected void onDurationSilentEnd() {
			synergy.remove(target.getPlayer());
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}


		@EventHandler
		public void onEntityDamage(CEntityDamageEvent e) {
			if (e.getDamager() == null) return;
			Entity attacker = e.getDamager();
			if (attacker instanceof Projectile) {
				Projectile projectile = (Projectile) attacker;
				if (projectile.getShooter() instanceof Entity) {
					attacker = (Entity) projectile.getShooter();
				}
			}

			if (attacker.equals(getPlayer()) && e.getEntity().equals(target.getPlayer()) && synergy.contains(target.getPlayer()) && isRunning()) {
				final double damage = e.getDamage() * 0.2;
				final double targetHealth = target.getPlayer().getHealth();

				if (targetHealth > damage) {
					e.setDamage(0);
					Healths.setHealth(target.getPlayer(), targetHealth - damage);
				} else {
					Healths.setHealth(target.getPlayer(), 1);
					e.setDamage(1000000);
				}
			}
		}
	}
}
