package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.disguise.DisguiseUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerQuitEvent;

@AbilityManifest(name = "변장술", rank = Rank.A, species = Species.HUMAN, explain = {
		"7칸 이내의 상대방을 바라본 체 철괴로 우클릭 시 그 대상으로 변장합니다.",
		"변장하고 있는 동안 다른 참가자에게 받는 대미지의 $[reflect]%는 대상에게 돌아갑니다.",
		"대미지를 3회 받으면 변장이 풀립니다. $[cool]",
		"변장하는 동안 자신의 스킨과 이름표가 그 대상의 것으로 바뀝니다.",
		"§8팀전에서 이름표가 바뀌는 기능은 작동하지 않습니다.",
		"※ 스킨변경: $[changeSkin], 이름변경: $[changeNameTag]"
})
public class Disguise extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> range = Config.of(Disguise.class, "범위", 7, FunctionalInterfaces.positive());
	private static final Config<Integer> count = Config.of(Disguise.class, "변장_후_공격받는_횟수", 3, FunctionalInterfaces.positive());
	private static final Config<Integer> cool = Config.of(Disguise.class, "쿨타임", 180, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
	private static final Config<Integer> reflect = Config.of(Disguise.class, "반사(%)", 50, FunctionalInterfaces.positive());
	private static final Config<Boolean> changeSkin = Config.of(Disguise.class, "스킨변경", true, FunctionalInterfaces.ON_OFF,
			"# 변장술 사용할 시 변장술의 대상으로 스킨 변경 여부",
			"# 기본값: true");
	private static final Config<Boolean> changeNameTag = Config.of(Disguise.class, "이름표변경", true, FunctionalInterfaces.ON_OFF,
			"# 변장술 사용할 시 변장술의 대상으로 닉네임 변경 여부",
			"# 팀전에선 작동하지 않음",
			"# 기본값: true");
	private final Cooldown cooldown = new Cooldown(cool.getValue());
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
	private Participant target = null;
	private int check = 0;

	public Disguise(Participant arg0) {
		super(arg0);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update != Update.RESTRICTION_CLEAR) {
			if (DisguiseUtil.isChanged(getPlayer())) {
				if (changeSkin.getValue()) {
					DisguiseUtil.changeSkin(getPlayer(), getPlayer().getUniqueId());
				}
				if (changeNameTag.getValue() && !(getGame() instanceof Teamable)) {
					DisguiseUtil.setPlayerNameTag(getPlayer(), getPlayer().getUniqueId());
				}
				DisguiseUtil.reloadPlayer(getPlayer());
			}
			DisguiseUtil.clearData();
		} else {
			DisguiseUtil.saveData();
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (target == null && material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), range.getValue(), predicate);
			if (player != null) {
				target = getGame().getParticipant(player.getUniqueId());

				DisguiseUtil.saveData();
				getPlayer().sendMessage(KoreanUtil.addJosa(player.getName(), Josa.으로로) + " 변장합니다.");
				if (changeSkin.getValue()) {
					DisguiseUtil.changeSkin(getPlayer(), player.getUniqueId());
				}
				if (changeNameTag.getValue() && !(getGame() instanceof Teamable)) {
					DisguiseUtil.setPlayerNameTag(getPlayer(), player.getUniqueId());
				}
				DisguiseUtil.reloadPlayer(getPlayer());
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent event) {
		if (event.getParticipant().equals(target)) {
			target = null;
			check = 0;
			getPlayer().sendMessage("변장이 풀렸습니다.");
			if (DisguiseUtil.isChanged(getPlayer())) {
				if (changeSkin.getValue()) {
					DisguiseUtil.changeSkin(getPlayer(), getPlayer().getUniqueId());
				}
				if (changeNameTag.getValue() && !(getGame() instanceof Teamable)) {
					DisguiseUtil.setPlayerNameTag(getPlayer(), getPlayer().getUniqueId());
				}
				DisguiseUtil.reloadPlayer(getPlayer());
			}
			cooldown.start();
		}
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		Entity damager = e.getDamager();
		if (damager != null && NMS.isArrow(damager)) {
			Projectile arrow = (Projectile) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (e.getEntity().equals(getPlayer()) && target != null && damager instanceof Player && !damager.equals(getPlayer())) {
			check += 1;
			SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.C));
			if (check >= count.getValue()) {
				target = null;
				check = 0;
				getPlayer().sendMessage("변장이 풀렸습니다.");
				if (DisguiseUtil.isChanged(getPlayer())) {
					if (changeSkin.getValue()) {
						DisguiseUtil.changeSkin(getPlayer(), getPlayer().getUniqueId());
					}
					if (changeNameTag.getValue() && !(getGame() instanceof Teamable)) {
						DisguiseUtil.setPlayerNameTag(getPlayer(), getPlayer().getUniqueId());
					}
					DisguiseUtil.reloadPlayer(getPlayer());
				}
				cooldown.start();
			}
			target.getPlayer().damage(e.getDamage() * reflect.getValue() / 100.0, damager);
			e.setDamage(0);
		}
	}

	@SubscribeEvent
	public void onPlayerLeave(PlayerQuitEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			target = null;
			check = 0;
			getPlayer().sendMessage("변장이 풀렸습니다.");
			if (DisguiseUtil.isChanged(getPlayer())) {
				if (changeSkin.getValue()) {
					DisguiseUtil.changeSkin(getPlayer(), getPlayer().getUniqueId());
				}
				if (changeNameTag.getValue() && !(getGame() instanceof Teamable)) {
					DisguiseUtil.setPlayerNameTag(getPlayer(), getPlayer().getUniqueId());
				}
				DisguiseUtil.reloadPlayer(getPlayer());
			}
			DisguiseUtil.reloadPlayer(getPlayer());
			cooldown.start();
		}
	}
}
