package cokes86.addon.ability.list;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "세트", rank = Rank.S, species = Species.GOD, explain = { "게임 중 플레이어가 사망할때마다 전쟁스택이 1씩 쌓입니다.",
		"전쟁스택이 남아있는 생존자 대비 일정 비율 이상일 시 힘버프가 주어집니다.", "철괴 우클릭시 자신 기준 $[range]블럭 이내 모든 플레이어를 자신의 위치로 이동시킨 후",
		"나약함1 디버프를 $[debuff] 부여합니다. $[cool]", "※$[buff1]% 이상 : 힘1  $[buff2]% 이상 : 힘2  $[buff3]% 이상 : 힘3" })
public class Seth extends CokesAbility implements ActiveHandler {
	int max = getGame().getParticipants().size();
	int kill = 0;
	DecimalFormat df = new DecimalFormat("0.00");
	public static Config<Integer> buff1 = new Config<Integer>(Seth.class, "힘.1단계", 25) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, buff2 = new Config<Integer>(Seth.class, "힘.2단계", 75) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, buff3 = new Config<Integer>(Seth.class, "힘.3단계", 150) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, cool = new Config<Integer>(Seth.class, "쿨타임", 60, 1) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, debuff = new Config<Integer>(Seth.class, "디버프시간", 5, 2) {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	}, range = new Config<Integer>(Seth.class, "범위", 7) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	};

	static {
		if (!((buff1.getValue() < buff2.getValue()) && (buff2.getValue() < buff3.getValue()))) {
			buff1.setValue(buff1.getDefaultValue());
			buff2.setValue(buff2.getDefaultValue());
			buff3.setValue(buff3.getDefaultValue());
		}
	}

	Cooldown c = new Cooldown(cool.getValue());

	ActionbarChannel ac = newActionbarChannel();

	AbilityTimer Passive = new AbilityTimer() {
		@Override
		protected void run(int arg0) {
			ac.update(df.format((double) kill * 100 / max) + "% (" + kill + "/" + max + ")");

			if (kill * 100 / max >= buff1.getValue() && kill * 100 / max < buff2.getValue()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0));
			} else if (kill * 100 / max >= buff2.getValue() && kill * 100 / max < buff3.getValue()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 1));
			} else if (kill * 100 / max >= buff3.getValue()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 2));
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	public Seth(Participant arg0) {
		super(arg0);
		Passive.register();
	}
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			Passive.start();
		}
	}

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
		}
		return true;
	};

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown()) {
				int range = Seth.range.getValue();
				ArrayList<Player> ps = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate);
				if (ps.size() > 0) {
					for (Player p : ps) {
						p.teleport(getPlayer().getLocation());
						p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, debuff.getValue() * 20, 0));
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(p);
					}
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
					c.start();
					return true;
				} else {
					getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
				}
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			kill += 1;

			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(e.getEntity().getUniqueId())) {
					max -= 1;
				}
			}
		}
	}
}
