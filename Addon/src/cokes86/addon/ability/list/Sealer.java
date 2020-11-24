package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@AbilityManifest(name = "봉인자", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴로 상대방을 우클릭할 시 상대방의 능력을 $[duration]간 비활성화시킵니다. $[cool]",
		"이미 비활성화되어있는 능력에겐 이 능력이 발동하지 않습니다.",
		"봉인한 능력의 등급에 따라 자신에게 각종 효과를 $[duration]간 부여합니다.",
		"§eC 등급§f: 나약함1 | §bB 등급§f: 재생1 | §aA 등급§f: 힘1 | §dS 등급§f: 힘2 | §6L 등급§f: 힘2, 저항1",
		"§7특정 능력을 봉인하면 어떤 일이?!"
})
@NotAvailable({AbstractTripleMix.class})
public class Sealer extends CokesAbility implements TargetHandler {
	private static final Config<Integer> cool = new Config<Integer>(Sealer.class, "쿨타임", 60, 1) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, duration = new Config<Integer>(Sealer.class, "지속시간", 7, 2) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	private Participant target = null;

	private final Cooldown c = new Cooldown(cool.getValue());
	private SealTimer t = new SealTimer(getParticipant());

	public Sealer(Participant participant) {
		super(participant);
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (t.isRunning() && target != null && (e.getEntity().equals(getPlayer())) || e.getEntity().equals(target.getPlayer())) {
			t.stop(true);
		}
	}

	@Override
	public void TargetSkill(Material mt, LivingEntity entity) {
		if (mt.equals(Material.IRON_INGOT) && !t.isRunning() && !c.isCooldown()) {
			if (entity instanceof Player) {
				Player p = (Player) entity;
				target = getGame().isParticipating(p) ? getGame().getParticipant(p) : null;
				if (target != null && target.getAbility() != null && !target.getAbility().isRestricted()) {
					t = new SealTimer(target);
				} else {
					getPlayer().sendMessage("상대방의 능력이 없거나 이미 비활성화되어있는 상태입니다.");
				}
			}
		}
	}

	class SealTimer extends Duration implements Listener {
		private final Participant target;
		private final ActionbarChannel ac;
		private boolean synergy = false;

		public SealTimer(Participant target) {
			super(duration.getValue(), c);
			this.target = target;
			this.ac = target.actionbar().newChannel();
			if (target != getParticipant()) this.start();
		}

		@Override
		public void onDurationStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			AbilityBase ab = target.getAbility();
			if (ab != null) {
				getPlayer().sendMessage(target.getPlayer().getName() + "님의 능력을 봉인하였습니다.");
				target.getPlayer().sendMessage("당신의 능력이 봉인되었습니다.");
				ab.setRestricted(true);
				SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(target.getPlayer());
				if (ab instanceof Mix) {
					Mix mix = (Mix) ab;
					if (mix.hasSynergy()) {
						this.synergy = true;
						getPlayer().sendMessage("§a시너지 §f봉인! 대미지가 §c%80% 감소§f하는 대신 대상에게 §a갑옷을 무시하고 §f대미지를 줍니다!");
					} else {
						int level = (mix.getFirst().getRank().ordinal() + mix.getSecond().getRank().ordinal())/2;
						switch (level) {
							case 0:
								getPlayer().sendMessage("§eC 등급 §f봉인! 나약함1 버프를 부여합니다!");
								getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration.getValue() * 20, 0));
								break;
							case 1:
								getPlayer().sendMessage("§bB 등급 §f봉인! 재생1 버프를 부여합니다!");
								getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration.getValue() * 20, 1));
								break;
							case 2:
								getPlayer().sendMessage("§aA 등급 §f봉인! 힘1 버프를 부여합니다!");
								getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration.getValue() * 20, 0));
								break;
							case 3:
								getPlayer().sendMessage("§dS 등급 §f봉인! 힘2 버프를 부여합니다!");
								getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration.getValue() * 20, 1));
								break;
							case 4:
								getPlayer().sendMessage("§6L 등급 §f봉인! 힘2, 저항1 버프를 부여합니다!");
								getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration.getValue() * 20, 1));
								getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration.getValue() * 20, 0));
								break;
							default:
								getPlayer().sendMessage("이게 무슨 등급의 능력이여?!");
						}
					}
				} else {
					if (ab.getRank().equals(Rank.C)) {
						getPlayer().sendMessage("§eC 등급 §f봉인! 나약함1 버프를 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration.getValue() * 20, 0));
					} else if (ab.getRank().equals(Rank.B)) {
						getPlayer().sendMessage("§bB 등급 §f봉인! 재생1 버프를 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration.getValue() * 20, 0));
					} else if (ab.getRank().equals(Rank.A)) {
						getPlayer().sendMessage("§aA 등급 §f봉인! 힘1 버프를 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration.getValue() * 20, 0));
					} else if (ab.getRank().equals(Rank.S)) {
						getPlayer().sendMessage("§dS 등급 §f봉인! 힘2 버프를 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration.getValue() * 20, 1));
					} else if (ab.getRank().equals(Rank.L)) {
						getPlayer().sendMessage("§6L 등급 §f봉인! 힘2, 저항1 버프를 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration.getValue() * 20, 1));
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration.getValue() * 20, 0));
					} else {
						getPlayer().sendMessage("이게 무슨 등급의 능력이여?!");
					}
				}
			} else {
				stop(true);
			}
		}

		@Override
		protected void onDurationProcess(int count) {
			ac.update("능력 활성화까지 " + TimeUtil.parseTimeAsString(getFixedCount()) + " 남음");
			target.getPlayer().sendMessage("§a능력 활성화까지 " + TimeUtil.parseTimeAsString(getFixedCount()) + " 남음");
			SoundLib.BLOCK_ANVIL_PLACE.playSound(target.getPlayer());
		}

		@Override
		protected void onDurationSilentEnd() {
			if (target.getAbility() != null) target.getAbility().setRestricted(false);
			HandlerList.unregisterAll(this);
			ac.unregister();
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Entity attacker = e.getDamager();
			if (attacker instanceof Projectile) {
				Projectile projectile = (Projectile) attacker;
				if (projectile.getShooter() instanceof Entity) {
					attacker = (Entity) projectile.getShooter();
				}
			}

			if (attacker.equals(getPlayer()) && e.getEntity().equals(target.getPlayer()) && synergy && isRunning()) {
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
