package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.effect.list.Seal;
import com.cokes86.cokesaddon.util.PairSet;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeveloperSettings;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@AbilityManifest(name = "듀얼", rank = Rank.SPECIAL, species = Species.SPECIAL, explain={
    "§7패시브 §8- §c듀얼모드§f: 2개의 캐릭터를 운용합니다.",
    "  두 캐릭터는 능력과 체력은 다르나 그 외의 것들은 공유합니다.",
    "  능력은 B ~ S 사이에서 배정받습니다.",
    "§7에메랄드 우클릭 §8- §c체인지§f: 자신이 운용하는 캐릭터를 바꿉니다.",
    "  단 1초간 움직일 수 없고, 능력이 봉인됩니다.",
    "$(CHARACTER_EXPLAIN)"
})
public class Dual extends CokesAbility implements ActiveHandler, TargetHandler {
    private final PairSet<AbilityBase, Double> first, second;
    private boolean usingSecond = false;
    private boolean giveEmerald = false;

    @SuppressWarnings("unused")
    private final Object CHARACTER_EXPLAIN = new Object() {
        @Override
        public String toString() {
            final StringJoiner joiner = new StringJoiner("\n");
            joiner.add("====== [ 운용중인 캐릭터 ] ======");
            String restrictedAbility = "[ ";
            restrictedAbility += (first.getLeft().isRestricted() ? "":"§e")+first.getLeft().getName();
            restrictedAbility += "§f | ";
            restrictedAbility += (second.getLeft().isRestricted() ? "":"§e")+second.getLeft().getName() + "§f ]";
            joiner.add(restrictedAbility);

            formatInfo(joiner, usingSecond ? second.getLeft() : first.getLeft());
            return joiner.toString();
        }

        private void formatInfo(final StringJoiner joiner, final AbilityBase ability) {
			if (ability != null) {
				joiner.add("§b" + ability.getName() + " " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext(); ) {
					joiner.add(ChatColor.RESET + iterator.next());
				}
			} else {
				joiner.add("§f능력이 없습니다.");
			}
		}
    };

    public Dual(Participant arg0) {
        super(arg0);
        first = PairSet.of(createCharacter(), (double)Settings.getDefaultMaxHealth());
        second = PairSet.of(createCharacter(), (double)Settings.getDefaultMaxHealth());
        first.getLeft().setRestricted(isRestricted());
        second.getLeft().setRestricted(isRestricted());
    }

    private AbilityBase createCharacter() {
        try {
            List<AbilityRegistration> returnAbilities = AbilityList.values().stream().filter(abilityRegistration -> {
                if (abilityRegistration.getAbilityClass().getAnnotation(Beta.class) != null) {
                    if (!DeveloperSettings.isEnabled()) return false;
                }
                if (Configuration.Settings.isBlacklisted(abilityRegistration.getManifest().name())) {
                    return false;
                }
                if (abilityRegistration.getManifest().name().equals("유키")) return false;
                return abilityRegistration.getManifest().rank() == Rank.B || abilityRegistration.getManifest().rank() == Rank.A || abilityRegistration.getManifest().rank() == Rank.S;
            }).collect(Collectors.toList());
            final Random random = new Random();

            return AbilityBase.create(random.pick(returnAbilities), getParticipant());
        } catch (ReflectiveOperationException e) {
            return createCharacter();
        }
    }

    @Override
    public void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            if (usingSecond) {
                second.getLeft().setRestricted(false);
                first.getLeft().setRestricted(true);
                first.setRight(getPlayer().getHealth());
                Healths.setHealth(getPlayer(), second.getRight());
            } else {
                second.getLeft().setRestricted(true);
                first.getLeft().setRestricted(false);
                second.setRight(getPlayer().getHealth());
                Healths.setHealth(getPlayer(), first.getRight());
            }

            if (!giveEmerald) {
                getPlayer().getInventory().addItem(new ItemBuilder(MaterialX.EMERALD).build());
                giveEmerald = true;
            }
        } else if (update == Update.RESTRICTION_SET) {
            second.getLeft().setRestricted(true);
            first.getLeft().setRestricted(true);
        } else if (update == Update.ABILITY_DESTROY) {
            second.getLeft().destroy();
            first.getLeft().destroy();
        }
    }

    @Override
    public @NotNull String getDisplayName() {
        return "듀얼 ("+first.getLeft().getDisplayName()+" + "+second.getLeft().getName()+")";
    }

    @Override
	public boolean usesMaterial(Material material) {
		return material == Material.EMERALD || first.getLeft().usesMaterial(material) || second.getLeft().usesMaterial(material);
	}

    @Override
    public boolean ActiveSkill(Material arg0, ClickType arg1) {
        if (arg0 == Material.EMERALD && arg1 == ClickType.RIGHT_CLICK) {
            Stun.apply(getParticipant(), TimeUnit.TICKS, 20);
            Seal.apply(getParticipant(), TimeUnit.TICKS, 20);
            usingSecond = !usingSecond;
            return true;
        } else if (!usingSecond && first.getLeft() instanceof ActiveHandler && !first.getLeft().isRestricted() && first.getLeft().usesMaterial(arg0)) {
            return ((ActiveHandler) first.getLeft()).ActiveSkill(arg0, arg1);
        } else if (usingSecond && second.getLeft() instanceof ActiveHandler && !second.getLeft().isRestricted() && second.getLeft().usesMaterial(arg0)) {
            return ((ActiveHandler) second.getLeft()).ActiveSkill(arg0, arg1);
        }
        
        return false;
    }
    @Override
    public void TargetSkill(Material arg0, LivingEntity arg1) {
        if (!usingSecond && first.getLeft() instanceof TargetHandler && !first.getLeft().isRestricted() && first.getLeft().usesMaterial(arg0)) {
            ((TargetHandler) first.getLeft()).TargetSkill(arg0, arg1);
        }
        else if (usingSecond && second.getLeft() instanceof TargetHandler && !second.getLeft().isRestricted() && second.getLeft().usesMaterial(arg0)) {
           ((TargetHandler) second.getLeft()).TargetSkill(arg0, arg1);
        }
    }

    public boolean hasAbility() {
        return (first != null && second != null);
    }

    @Override
    public Set<GameTimer> getTimers() {
        if (hasAbility()) {
            return SetUnion.union(first.getLeft().getTimers(), second.getLeft().getTimers(), super.getTimers());
        }
        return super.getTimers();
    }

    @Override
    public Set<GameTimer> getRunningTimers() {
        if (hasAbility()) {
            return SetUnion.union(first.getLeft().getRunningTimers(), second.getLeft().getRunningTimers(), super.getRunningTimers());
        }
        return super.getRunningTimers();
    }
}
