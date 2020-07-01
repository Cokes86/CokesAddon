package cokes86.addon.ability.list;

import java.text.DecimalFormat;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import cokes86.addon.ability.Test;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.base.collect.Pair;

@AbilityManifest(name = "데이터마이닝", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
		"철괴 우클릭시 모든 플레이어의 능력을 알 수 있습니다.",
		"플레이어가 능력을 사용할 때 마다 그 사실을 알 수 있으며,",
		"§c마이닝 스택§f이 1만큼 상승합니다.", 
		"플레이어끼리 전투가 발생할 시 그 사실을 알 수 있으며,",
		"각 플레이어의 체력과 피해량을 알 수 있습니다.",
		"§c마이닝 스택§f이 1만큼 상승할 때 마다",
		"1.0%씩 데미지가 감소하거나, 0.2만큼의 추가데미지를 주는",
		"버프를 랜덤하게 받습니다. (각각 최대치 25%, 5)",
		"§c마이닝 스택§f은 최대 50까지 쌓입니다.",
		"철괴 좌클릭시 사실 여부 메세지를 끄고 킬 수 있습니다."
		}
)
@Test
public class DataMining extends AbilityBase implements ActiveHandler {
	DecimalFormat df = new DecimalFormat("0.00");
	int count = 0;
	double damage =0, defense =0;
	ActionbarChannel ac = newActionbarChannel();
	boolean message = true;

	public DataMining(Participant arg0) {
		super(arg0);
	}
	
	public void Active() {
		Random r = new Random();
		double a = r.nextDouble()*2;
		if (a > 1) {
			if (damage == 5) {
				defense += 1.0;
			} else {
				damage += 0.2;
			}
		} else {
			if (defense == 25) {
				damage += 0.2;
			} else {
				defense += 1.0;
			}
		}
	}
	
	public void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			ac.update("§c마이닝 스택§f: "+count+ " (추가대미지: "+df.format(damage)+"  피해감소: "+df.format(defense)+"%)");
			break;
		default:
		}
	}

	@SubscribeEvent
	public void onAbilityActiveSkill(AbilityActiveSkillEvent e) {
		if (!e.getParticipant().equals(getParticipant())) {
			if (message) getPlayer().sendMessage("§e" + e.getPlayer().getName() + "§f님이 능력을 사용하였습니다.");
			if (count != 50) {
				count++;
				Active();
			}
			ac.update("§c마이닝 스택§f: "+count+ " (추가대미지: "+df.format(damage)+"  피해감소: "+df.format(defense)+"%)");
		}
	}

	@SubscribeEvent()
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			Player entity = (Player) e.getEntity();
			Player damager;
			if (e.getDamager() instanceof Player)
				damager = (Player) e.getDamager();
			else if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player)
					damager = (Player) projectile.getShooter();
				else
					damager = null;
			} else
				damager = null;
			
			if (damager.equals(getPlayer())) {
				e.setDamage(e.getDamage()+damage);
			} else if (entity.equals(getPlayer())) {
				e.setDamage(e.getDamage()*((double)100-defense)/100);
			}

			if (damager != null && !e.isCancelled()) {
				if (message) getPlayer().sendMessage(
						"§e" + damager.getName() + "§f(§c♥" + df.format(damager.getHealth()) + "§f)님이 §e" + entity.getName()
								+ "§f(§c♥" + df.format(entity.getHealth()) + "§f)님을 공격! (대미지: " + df.format(e.getFinalDamage()) + ")");
			}
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			AbstractGame game = getGame();
			getPlayer().sendMessage("§2===== §a능력자 목록 §2=====");
			int count = 0;
			for (Participant p : game.getParticipants()) {
				if (p.equals(getParticipant()))
					continue;
				count++;
				AbilityBase ability = p.getAbility();
				String name;
				if (ability != null) {
					if (ability instanceof Mix) {
						Mix mix = (Mix) ability;
						if (mix.hasSynergy()) {
							Synergy synergy = mix.getSynergy();
							Pair<AbilityRegistration, AbilityRegistration> base = SynergyFactory
									.getSynergyBase(synergy.getRegistration());
							name = "§e"+synergy.getName() + " §f(§e" + base.getLeft().getManifest().name() + " §f+ §e"
									+ base.getRight().getManifest().name() + "§f)";
						} else {
							name = "§e"+mix.getFirst().getName() + " §f+ §e" + mix.getSecond().getName();
						}
					} else {
						name = ability.getName();
					}

					getPlayer().sendMessage("§e" + count + ". §f" + p.getPlayer().getName() + " §7: §e" + name);
				}
			}
			return true;
		} else if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK)) {
			message = (!message);
			if (message) getPlayer().sendMessage("사실 확인 메세지를 볼 수 있게 됩니다.");
			else getPlayer().sendMessage("더이상 사실 확인 메세지를 볼 수 없습니다.");
		}
		return false;
	}
}
