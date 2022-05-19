package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;

@AbilityManifest(name = "포커", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
		"철괴 우클릭 시 1~10 사이의 숫자를 3개 뽑습니다. 이 3개의 숫자의 조합에 따라 각종 효과를 얻습니다. $[cool]",
		"탑: 아무런 조합이 되지 않는 경우. 가장 높은 수가 9 또는 10일 경우 신속1 버프를 (높은 수)초 만큼 부여합니다.",
		"§a페어§f : 2개의 숫자가 같은 경우입니다. (페어의 수 * 2)초 만큼 재생2 버프를 부여합니다.",
		"§b스트레이트§f : 3개의 숫자가 연달아 나오는 경우입니다. 다음 공격 시 주는 대미지가 (가장 높은 수)만큼 증가합니다.",
		"§e트리플§f : 3개의 숫자가 모두 같은 경우입니다.",
		"자신과 팀을 제외한 모든 플레이어에게 (트리플의 수 * 1.5)의 관통 대미지를 줍니다."
})
public class Poker extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool = Config.of(Poker.class, "쿨타임", 30, Config.Condition.COOLDOWN);
	private final int[] num = new int[3];
	private int additional = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final Cooldown c = new Cooldown(cool.getValue());
	private final AbilityTimer p = new AbilityTimer() {
		@Override
		protected void run(int var1) {
			if (additional > 0) ac.update("다음 추가대미지: " + additional);
			else ac.update(null);
		}
	}.setPeriod(TimeUnit.TICKS, 1);
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

	public Poker(Participant participant) {
		super(participant);
		p.register();
	}

	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			p.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material materialType, ClickType ct) {
		if (materialType.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown()) {
				Random r = new Random();
				num[0] = r.nextInt(10) + 1;
				num[1] = r.nextInt(10) + 1;
				num[2] = r.nextInt(10) + 1;
				Arrays.sort(num);
				Pair<String,Integer> result = getPockerResult(num);
				getPlayer().sendMessage("숫자를 뽑습니다 : " + num[0] + " " + num[1] + " " + num[2]);
				switch (result.getLeft()) {
					case "Top":
						String str = "";
						if (num[2] == 9 || num[2] == 10) {
							str = str + "신속 버프를 " + result.getRight() + "초만큼 부여합니다.";
							PotionEffects.SPEED.addPotionEffect(getPlayer(), result.getRight() * 20, 0, true);
						}
						getPlayer().sendMessage("이런! 탑입니다! " + str);
						break;
					case "Pair":
						getPlayer().sendMessage("좋습니다! §a페어§f입니다! 재생2 버프를 " + (result.getRight() * 2) + "초간 받습니다.");
						PotionEffects.REGENERATION.addPotionEffect(getPlayer(), result.getRight() * 40, 0, true);
						break;
					case "Straight":
						getPlayer().sendMessage("와우! §b스트레이트§f입니다! 다음 공격은 추가적으로 " + (result.getRight()) + "의 대미지를 줍니다.");
						additional = result.getRight();
						break;
					case "Triple":
						getPlayer().sendMessage("완벽합니다! §e트리플§f입니다! 자신을 제외한 모든 플레이어에게 " + (result.getRight() * 1.5) + "만큼의 대미지를 줍니다.");
						for (Participant p : getGame().getParticipants()) {
							if (p.equals(getParticipant())) continue;
							if (predicate.test(p.getPlayer())) {
								Damages.damageFixed(p.getPlayer(), getPlayer(), result.getRight() * 1.5f);
							}
						}
						Bukkit.broadcastMessage("[§c!§f] 포커가 같은 수 3개를 뽑아 모두에게 대미지를 줍니다!");
						SoundLib.UI_TOAST_CHALLENGE_COMPLETE.broadcastSound();
						break;
				}
				c.start();
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (damager.equals(getPlayer()) && additional > 0) {
			e.setDamage(e.getDamage() + additional);
			additional = 0;
		}
	}

	public Pair<String, Integer> getPockerResult(int[] num) {
		int min = num[0], mid = num[1], max = num[2];
		if (min == mid && mid == max) return Pair.of("Triple", max);
		else if (mid == max - 1 && min == mid - 1) return Pair.of("Straight", max);
		else if (min == 1 && (mid == 9 || mid == 2) && max == 10) return Pair.of("Straight", 10);
		else if (min == mid || mid == max) return Pair.of("Pair", mid);
		else if (min == max) return Pair.of("Pair", max);
		else return Pair.of("Top", max);
	}
}
