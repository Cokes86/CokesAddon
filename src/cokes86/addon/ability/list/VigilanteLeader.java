package cokes86.addon.ability.list;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;

@AbilityManifest(name = "자경단장", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
		"게임 중 최초로 철괴 우클릭 시 범위 $[r]블럭 이내 모든 범위를 자경단 아지트로 만듭니다.", "또한 해당 범위 안에 있던 플레이어 전부 자경단원으로 소속됩니다.",
		"이후 철괴 우클릭 시 힘1 버프를 (참가자의 수*0.5)초 부여합니다. $[cool]", "자경단 아지트 내에서 참가자는 2명, 자경단원은 3명, 자경단장은 4명 취급하며",
		"자경단 아지트 밖에서 자경단장은 2명 취급합니다." })
public class VigilanteLeader extends AbilityBase implements ActiveHandler {
	public static Config<Integer> r = new Config<Integer>(VigilanteLeader.class, "아지트범위", 10) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(VigilanteLeader.class, "쿨타임", 90, 1) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};
	RGB color = new RGB(0, 162, 232);
	ArrayList<Participant> vigilantes = new ArrayList<>();
	Location ajit = null;
	int addNum;
	protected ActionbarChannel channel = this.newActionbarChannel();

	public VigilanteLeader(Participant participant) {
		super(participant);
	}

	CooldownTimer c = new CooldownTimer(cool.getValue());
	Timer passive = new Timer() {
		@Override
		protected void run(int count) {
			if (ajit != null) {
				for (Location l : Circle.of(r.getValue(), r.getValue() * 8).toLocations(ajit).floor(ajit.getY())) {
					for (Participant p : vigilantes)
						ParticleLib.REDSTONE.spawnParticle(p.getPlayer(), l, color);
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), l, color);
				}

				ArrayList<Player> inAjit = LocationUtil.getNearbyPlayers(ajit, r.getValue(), r.getValue());
				addNum = inAjit.size();

				if (inAjit.contains(getPlayer())) {
					addNum += 2;
				} else {
					addNum += 1;
				}
				for (Participant vigilante : vigilantes) {
					if (inAjit.contains(vigilante.getPlayer()))
						addNum += 1;
				}

				channel.update("인원 수: " + getGame().getParticipants().size() + "+" + addNum + "");
			}
		}
	}.setPeriod(TimeUnit.TICKS, 2);

	@Override
	public boolean ActiveSkill(Material m, ClickType ct) {
		if (m.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (ajit == null) {
				ajit = getPlayer().getLocation();
				getPlayer().sendMessage("자신의 자경단이 결성되었습니다!");
				for (Player p : LocationUtil.getNearbyPlayers(ajit, r.getValue(), r.getValue())) {
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
							(getGame().getParticipants().size() + addNum) * 10, 0));
					getPlayer().sendMessage((getGame().getParticipants().size() + addNum) * 0.5 + "초의 힘버프가 제공되었습니다.");
					c.start();
					return true;
				}
			}
		}
		return false;
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
			Player t = (Player) e.getDamager();
			AbstractGame g = getGame();
			if (g.isParticipating(t) && vigilantes.contains(g.getParticipant(t))) {
				e.setDamage(e.getDamage() * 7 / 10);
			}
		}
	}

	public void onUpdate(Update update) {
		switch (update) {
		case RESTRICTION_CLEAR:
			passive.start();
		default:
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onRestrictionClear(AbilityRestrictionClearEvent e) {
		passive.start();
	}
}
