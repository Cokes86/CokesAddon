package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@AbilityManifest(name = "자경단장", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
		"§7최초 철괴 우클릭 - §c자경단 집결§r: 범위 $[r]블럭 이내 모든 범위를 자경단 아지트로 만듭니다. 또한 해당 범위 안에 있던 플레이어 전부 자경단원으로 소속됩니다.",
		"§7철괴 우클릭 - §c자경단 공격§r: 힘1 버프를 참가자의 수만큼 부여합니다. $[cool]",
		"§7패시브 - §c단합§r: 자경단 아지트 내에서 참가자는 2명, 자경단원은 3명, 자경단장은 4명 취급하며",
		"  자경단 아지트 밖에서 자경단장은 2명 취급합니다.",
		"  자경단원은 자경단장을 공격할 때, 자경단장이 받는 대미지가 30% 감소합니다."})
public class VigilanteLeader extends CokesAbility implements ActiveHandler {
	public static Config<Integer> r = new Config<Integer>(VigilanteLeader.class, "아지트범위", 10) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<>(VigilanteLeader.class, "쿨타임", 90, Config.Condition.COOLDOWN);
	private final ActionbarChannel channel = this.newActionbarChannel();
	private final RGB color = RGB.of(0, 162, 232);
	private final Set<Participant> vigilantes = new HashSet<>();
	private Location ajit = null;
	private int addNum;
	private final Predicate<Entity> predicate = entity -> {
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};
	private final Cooldown c = new Cooldown(cool.getValue());
	private int num = getGame().getParticipants().size();
	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int count) {
			if (ajit != null) {
				for (Location l : Circle.of(r.getValue(), r.getValue() * 8).toLocations(ajit).floor(ajit.getY())) {
					for (Participant p : vigilantes)
						ParticleLib.REDSTONE.spawnParticle(p.getPlayer(), l, color);
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), l, color);
				}

				List<Player> inAjit = LocationUtil.getNearbyEntities(Player.class, ajit, r.getValue(), r.getValue(), predicate);
				addNum = 1;

				for (Player p : inAjit) {
					if (p.equals(getPlayer())) addNum += 3;
					else if (vigilantes.contains(getGame().getParticipant(p))) addNum += 2;
					else addNum += 1;
				}

				channel.update("인원 수: " + num + "+" + addNum + "");
			}
		}
	}.setPeriod(TimeUnit.TICKS, 2).register();
	public VigilanteLeader(Participant participant) {
		super(participant);
		passive.start();
	}

	@Override
	public boolean ActiveSkill(Material m, ClickType ct) {
		if (m.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (ajit == null) {
				ajit = getPlayer().getLocation().clone();
				getPlayer().sendMessage("자신의 자경단이 결성되었습니다!");
				for (Player p : LocationUtil.getNearbyEntities(Player.class, ajit, r.getValue(), r.getValue(), predicate)) {
					if (getGame().isParticipating(p) && !p.equals(getPlayer())) {
						vigilantes.add(getGame().getParticipant(p));
						getPlayer().sendMessage(p.getName() + "을 자경단원으로 영입했습니다.");
						p.sendMessage("당신은 " + getPlayer().getName() + "의 자경단에 입단하였습니다.");
					}
				}
				return true;
			} else {
				if (!c.isCooldown()) {
					getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
							(getGame().getParticipants().size() + addNum) * 20, 0));
					getPlayer().sendMessage((getGame().getParticipants().size() + addNum) + "초의 힘버프가 제공되었습니다.");
					c.start();
					return true;
				}
			}
		}
		return false;
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (e.getEntity().equals(getPlayer()) && damager instanceof Player) {
			Player t = (Player) damager;
			if (getGame().isParticipating(t) && vigilantes.contains(getGame().getParticipant(t))) {
				e.setDamage(e.getDamage() * 7 / 10);
			}
		}
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getGame() instanceof DeathManager.Handler) {
			DeathManager.Handler game = (DeathManager.Handler) getGame();
			if (game.getDeathManager().isExcluded(e.getEntity().getUniqueId())) {
				num -= 1;
			}
		}
	}
}
