package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "변장술", rank = Rank.A, species = Species.HUMAN, explain = {
		"7칸 이내의 상대방을 바라본 체 철괴로 우클릭 시 그 대상으로 변장합니다.",
		"변장하고 있는 동안 다른 참가자에게 받는 대미지의 $[reflec]%는 대상에게 돌아갑니다.",
		"대미지를 3회 받으면 변장이 풀립니다. $[cool]"
})
@Beta
public class Disguise extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> range = new Config<Integer>(Disguise.class, "범위", 7) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, count = new Config<Integer>(Disguise.class, "변장_후_공격받는_횟수", 3) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(Disguise.class, "쿨타임", 180, 1) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, reflec = new Config<Integer>(Disguise.class, "반사(%)", 50) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};
	
	private Participant target = null;
	private int check = 0;
	private final Cooldown cooldown = new Cooldown(cool.getValue());
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
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};

	public Disguise(Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (target == null && material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), range.getValue(), predicate);
			if (player != null) {
				target = getGame().getParticipant(player.getUniqueId());
				getPlayer().sendMessage(player.getName()+"님으로 변장합니다.");
				return true;
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}
		
		if (e.getEntity().equals(getPlayer()) && target != null && damager instanceof Player && !damager.equals(getPlayer())) {
			check += 1;
			SoundLib.BELL.playInstrument(getPlayer(), Note.natural(2, Tone.C));
			if (check >= count.getValue()) {
				target = null;
				check = 0;
				getPlayer().sendMessage("변장이 풀렸습니다.");
				cooldown.start();
			}
			
			((Player) damager).damage(e.getDamage() * reflec.getValue() / 100, damager);
			e.setDamage(0);
		}
	}
}
