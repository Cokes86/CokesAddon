package cokes86.addon.ability.list;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

import daybreak.abilitywar.game.interfaces.TeamGame;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.object.DeathManager;
import org.bukkit.entity.Player;

@AbilityManifest(
		name = "소환사",
		rank = Rank.A,
		species = Species.HUMAN,
		explain = {"철괴 우클릭 시 자신을 제외한 참가자 1명을 $[duration] 뒤에 소환합니다.",
		"소환되는 위치는 능력을 발동한 시점의 위치입니다.",
		"이 능력을 사용할 경우 바로 비활성화됩니다."}
)
public class Summoner extends AbilityBase implements ActiveHandler {
	public static Config<Integer> duration = new Config<Integer>(Summoner.class, "대기시간", 3, 2) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	Location l;
	Participant target;
	boolean active = true;

	public Summoner(Participant arg0) {
		super(arg0);
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
			if (getGame() instanceof TeamGame) {
				TeamGame game = (TeamGame) getGame();
				return (!game.hasTeam(getParticipant()) || game.hasTeam(target) || game.getTeam(getParticipant()).equals(game.getTeam(target)));
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && !Active.isRunning() && active) {
			AbstractGame g = getGame();
			ArrayList<Participant> par = new ArrayList<>(g.getParticipants());
			par.removeIf(p -> !predicate.test(p.getPlayer()));
			Random r = new Random();

			if (par.size() > 0) {
				target = par.get(r.nextInt(par.size()));
				getPlayer().sendMessage(target.getPlayer().getName()+"님을 소환합니다.");
				Active.start();
				return true;
			} else {
				getPlayer().sendMessage("자신을 제외한 플레이어가 존재하지 않습니다.");
				return false;
			}
		}
		return false;
	}

	AbilityTimer Active = new AbilityTimer(duration.getValue()) {
		@Override
		protected void onEnd() {
			NMS.clearTitle(target.getPlayer());
			target.getPlayer().teleport(l);
			onSilentEnd();
			active = false;
		}

		@Override
		protected void onSilentEnd() {
			Summoner.this.setRestricted(true);
		}

		@Override
		protected void run(int arg0) {

		}

		@Override
		protected void onStart() {
			NMS.sendTitle(target.getPlayer(), "경   고", "소환사가 당신을 소환하고 있습니다", 5, duration.getValue() * 20 - 10, 5);
			l = getPlayer().getLocation();
		}
	};
}
