package cokes86.addon.synergy.list;

import cokes86.addon.synergy.CokesSynergy;
import daybreak.abilitywar.ability.*;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Objects;

@AbilityManifest(name = "능력 도둑", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 도굴자의 손놀림: 플레이어가 사망할 시, 해당 플레이어의",
        "  능력을 자신의 §b컬랙션 북§f에 담습니다.",
        "책 우클릭 - 변장의 달인: 자신이 훔친 능력들 중 한 쌍을 가져와",
        "  자신의 능력으로 삼습니다. 적용중인 능력은 액션바를 통해 확인할 수 있습니다",
        "책 좌클릭 - 계획 확인: 자신이 적용중인 두 능력의 설명을 볼 수 있습니다."
})
@Materials(materials = {Material.BOOK})
@Beta
public class AbilityThief extends CokesSynergy implements ActiveHandler, TargetHandler {
    private final ArrayList<Pair<AbilityRegistration, AbilityRegistration>> stolen = new ArrayList<>();
    private final ItemStack book = new ItemBuilder(MaterialX.BOOK).displayName(ChatColor.BLUE+"컬랙션 북").build();
    private Pair<AbilityRegistration, AbilityRegistration> choose = null;
    private final Mix mix;
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();

    public AbilityThief(AbstractGame.Participant participant) throws ReflectiveOperationException {
        super(participant);
        this.mix = Objects.requireNonNull(Mix.create(Mix.class, getParticipant()));
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {
        if (!e.getParticipant().equals(getParticipant())) {
            AbstractMix.MixParticipant participant = (AbstractMix.MixParticipant) e.getParticipant();
            Mix mix = participant.getAbility();
            if (mix != null) {
                if (mix.getSynergy() != null) {
                    stolen.add(SynergyFactory.getSynergyBase(mix.getSynergy().getRegistration()));
                } else {
                    stolen.add(Pair.of(mix.getFirst().getRegistration(), mix.getSecond().getRegistration()));
                }
            }
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.BOOK) {
            if (clickType == ClickType.RIGHT_CLICK) {
                try {
                    Pair<AbilityRegistration, AbilityRegistration> pick = new Random().pick(stolen);
                    if (pick.equals(choose)) {
                        return ActiveSkill(material, clickType);
                    }
                    getPlayer().sendMessage("§b"+pick.getLeft().getManifest().name() + "+"+pick.getRight().getManifest().name()+" §r으로 변장합니다.");
                    mix.setAbility(pick.getLeft(), pick.getRight());
                    choose = pick;
                    channel.update("변장 능력 | §b"+pick.getLeft().getManifest().name() + "+"+pick.getRight().getManifest().name());
                } catch (Exception ignored) {}
            } else {
                if (mix != null) {
                    for (String info : Formatter.formatAbilityInfo(mix)) {
                        getPlayer().sendMessage(info);
                    }
                } else {
                    getPlayer().sendMessage("자신에게 적용중인 능력이 없습니다.");
                }
            }
        }
        return false;
    }

    @Override
    public void TargetSkill(Material material, LivingEntity livingEntity) {
        if (mix != null && mix.usesMaterial(material)) {
            mix.TargetSkill(material, livingEntity);
        }
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            getPlayer().getInventory().addItem(book);
        } else {
            if (mix != null) {
                mix.removeAbility();
            }
        }
    }
}
