package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.effect.list.Nightmare;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import static org.bukkit.ChatColor.*;
import static cokes86.addon.ability.CokesAbility.Config.Condition.*;

import java.util.HashMap;
import java.util.Map;

@AbilityManifest(name = "아이리스", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c레인보우§r: $[RAINBOW_RANGE]블럭 이내 상대방을 보고 우클릭 시",
        "  대상에게 $[RAINBOW_DAMAGE]의 대미지를 주고 무지개 스택을 1씩 증가합니다. $[RAINBOW_COOLDOWN]",
        "  각 플레이어 머리 위에 1 이상의 무지개 스택이 표기됩니다.",
        "§7철괴 우클릭 §8- §c나이트메어§r: 상대방을 보지않고 우클릭 시,",
        "  무지개 스택이 3 이상인 모든 플레이어에게 악몽 상태이상을 $[NIGHTMARE_DURATION] 부여합니다. $[NIGHTMARE_COOLDOWN]",
        "  무지개 스택이 3 이상인 플레이어가 존재하지 않다면 발동하지 않으며,",
        "  사용되었을 시 스택 개수에 상관없이 모든 스택이 초기화됩니다.",
        "§7철괴 좌클릭 §8- §c레디 투 나이트메어§r: 모든 플레이어의 무지개 스택을 확인합니다.",
        "§7상태이상 §8- 악몽§r: 움직일 수 없고, 시야가 가려집니다. 받는 대미지가 1.5배 증가하고",
        "  액티브, 타겟팅 능력을 사용할 수 없습니다."
})
@Beta
public class Iris extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RAINBOW_COOLDOWN = new Config<>(Iris.class, "레인보우_쿨타임", 7, COOLDOWN);
    private static final Config<Integer> RAINBOW_RANGE = new Config<>(Iris.class, "레인보우_범위", 7, a -> a > 0);
    private static final Config<Double> RAINBOW_DAMAGE = new Config<>(Iris.class, "레인보우_범위", 3.0, a -> a > 0);
    private static final Config<Integer> NIGHTMARE_DURATION = new Config<>(Iris.class, "악몽_지속시간", 7, TIME);
    private static final Config<Integer> NIGHTMARE_COOLDOWN = new Config<>(Iris.class, "나이트메어_쿨타임", 30, COOLDOWN);

    private final Cooldown rainbow = new Cooldown(RAINBOW_COOLDOWN.getValue(), "레인보우");
    private final Cooldown nightmare = new Cooldown(NIGHTMARE_COOLDOWN.getValue(), "나이트메어");

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

    private final Map<AbstractGame.Participant, Integer> rainbowStack = new HashMap<>();

    public Iris(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), RAINBOW_RANGE.getValue(), predicate);

            if (player != null) {
                if (!rainbow.isCooldown()) {
                    AbstractGame.Participant target = getGame().getParticipant(player);
                    rainbowStack.put(target, rainbowStack.getOrDefault(target, 0)+1);
                    player.damage(RAINBOW_DAMAGE.getValue(), getPlayer());
                    SoundLib.AMBIENT_CAVE.playSound(player);
                    return rainbow.start();
                }
            } else {
                if (!nightmare.isCooldown()) {
                    int nightmared = 0;
                    for (AbstractGame.Participant participant : rainbowStack.keySet()) {
                        if (rainbowStack.get(participant) >= 3) {
                            nightmared++;
                            Nightmare.apply(participant, TimeUnit.SECONDS, NIGHTMARE_DURATION.getValue());
                        }
                    }
                    if (nightmared != 0) {
                        rainbowStack.clear();
                        return nightmare.start();
                    }
                    getPlayer().sendMessage("무지개 스택이 3이상인 플레이어가 없습니다.");
                }
            }
        } else if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK) {
            getPlayer().sendMessage(Formatter.formatTitle(GRAY, DARK_GRAY, "무지개 스택"));

            for (Map.Entry<AbstractGame.Participant, Integer> entry : rainbowStack.entrySet()) {
                getPlayer().sendMessage(GRAY + entry.getKey().getPlayer().getName() + DARK_GRAY + ": "+GRAY + entry.getValue());
            }
        }
        return false;
    }
}
