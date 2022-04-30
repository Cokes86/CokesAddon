package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.function.Predicate;

@AbilityManifest(name = "로열 스트레이트 플러쉬", rank = Rank.S, species = Species.OTHERS, explain = {
		"철괴 우클릭시 1 ~ 20사이의 숫자를 2개 뽑습니다.",
		"주변 $[range]범위 이내 모든 플레이어에게",
		"뽑은 두 숫자의 합을 3으로 나눈 값에 해당하는 고정 대미지를 줍니다. $[cool]"
})
public class RoyalStraightFlush extends CokesSynergy implements ActiveHandler {
	public static Config<Integer> range = Config.of(RoyalStraightFlush.class, "범위", 15, FunctionalInterfaceUnit.positive());
	public static Config<Integer> cool = Config.of(RoyalStraightFlush.class, "쿨타임", 60, Config.Condition.COOLDOWN);
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
	private final Cooldown cooldown = new Cooldown(cool.getValue());

	public RoyalStraightFlush(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!cooldown.isCooldown()) {
				int r = range.getValue();
				int a = new Random().nextInt(20)+1, b = new Random().nextInt(20)+1;
				getPlayer().sendMessage("숫자를 뽑습니다 : " + a + ", " + b);
				getPlayer().sendMessage("총 " + (a + b) / 3.0 + "의 고정대미지를 상대방에게 줍니다.");
				for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), r, r, predicate)) {
					Damages.damageFixed(p, getPlayer(), (a + b) / 3.0f);
					SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR.playSound(p);
				}
				cooldown.start();
				return true;
			}
		}
		return false;
	}

}
