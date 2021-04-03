package cokes86.addon.synergy.list;

import cokes86.addon.effect.list.Seal;
import cokes86.addon.synergy.CokesSynergy;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Objects;
import java.util.StringJoiner;

@AbilityManifest(name = "능력 도둑", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "능력 활성화 - 컬랙션: 자신이 훔친 능력을 고유의 책에 저장하여 소지합니다.",
        "  메이크업을 통해 변장하면, 해당 능력의 스킬을 쓸 수 있습니다.",
        "철괴 우클릭 - 소매치기: 상대방을 보고 사용 시 대상의 능력 2개를 가져옵니다.",
        "  훔쳐진 대상은 10초간 능력 봉인 효과를 받습니다.",
        "  메이크업을 한 상태에선 훔쳐진 능력이 우선적으로 사용됩니다.",
        "참가자 사망 - 흔적없는 도굴: 대상이 사망한 자리에 흔적이 남습니다.",
        "  흔적에 3블럭 이내 가까워 질 경우, 대상의 능력을 훔칩니다.",
        "책 좌클릭 - 메이크업: 자신이 훔친 능력을 순서대로 얻습니다.",
        "  웅크리고 사용할 경우, 자신이 훔친 능력을 순서대로 알 수 있습니다.",
        "책 우클릭 - 기록 제거: 메이크업으로 얻은 자신의 능력을 버리고 컬랙션에서 지웁니다.",
        "  자신은 다시 §b능력 도둑§r의 소매치기를 사용할 수 있습니다.",
        "훔쳐진 능력 | $(STOLEN_INFO)"
})
@Materials(materials = {Material.IRON_INGOT, Material.BOOK})
@Beta
public class AbilityThief extends CokesSynergy implements ActiveHandler, TargetHandler {
    private final ArrayList<Pair<AbilityRegistration, AbilityRegistration>> stolen = new ArrayList<>();
    private final ItemStack book = new ItemBuilder(MaterialX.BOOK).displayName(ChatColor.BLUE+"컬랙션 북").build();
    private final Mix mix;
    private int cursor = 0;
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();

    @SuppressWarnings(value = "unused")
    private final Object STOLEN_INFO = new Object() {
        @Override
        public String toString() {
            if (mix.hasAbility()) {
                if (mix.hasSynergy()) {
                    Synergy synergy = mix.getSynergy();
                    AbilityRegistration first = SynergyFactory.getSynergyBase(synergy.getRegistration()).getLeft(),
                            second = SynergyFactory.getSynergyBase(synergy.getRegistration()).getRight();
                    return mix.getSynergy().getDisplayName() + " ("+first.getManifest().name() + " + " + second.getManifest().name()+")";
                }
                return mix.getFirst().getDisplayName() + " + " + mix.getSecond().getDisplayName();
            }
            return "없습니다.";
        }
    };

    public AbilityThief(AbstractGame.Participant participant) throws ReflectiveOperationException {
        super(participant);
        this.mix = Objects.requireNonNull(Mix.create(Mix.class, getParticipant()));
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (mix.hasAbility()) {
            if (mix.usesMaterial(material)) {
                return mix.ActiveSkill(material, clickType);
            }
        }
        if (material == Material.BOOK) {
            if (clickType == ClickType.LEFT_CLICK) {
                if (getPlayer().isSneaking()) {
                    StringJoiner joiner = new StringJoiner(" | ");
                    for (Pair<AbilityRegistration, AbilityRegistration> pair : stolen) {
                        if (SynergyFactory.getSynergy(pair.getLeft().getAbilityClass(), pair.getRight().getAbilityClass()) != null) {
                            AbilityRegistration s = SynergyFactory.getSynergy(pair.getLeft().getAbilityClass(), pair.getRight().getAbilityClass());
                            joiner.add(s.getManifest().name());
                        } else {
                            joiner.add(pair.getLeft().getManifest().name()+" + "+pair.getRight().getManifest().name());
                        }
                    }
                    getPlayer().sendMessage("==== 훔친 능력 목록 ====");
                    getPlayer().sendMessage(joiner.toString());
                } else if (!stolen.isEmpty()){
                    if (++cursor >= stolen.size()) {
                        cursor = 0;
                    }
                    try {
                        mix.setAbility(stolen.get(cursor).getLeft(), stolen.get(cursor).getRight());
                        if (mix.hasSynergy()) {
                            getPlayer().sendMessage(mix.getSynergy().getDisplayName()+"으로 메이크업합니다.");
                            channel.update(mix.getSynergy().getDisplayName());
                        }
                        else {
                            getPlayer().sendMessage(mix.getFirst().getDisplayName() + " + "+mix.getSecond().getDisplayName()+"으로 메이크업합니다.");
                            channel.update(mix.getFirst().getDisplayName() + " + "+mix.getSecond().getDisplayName());
                        }
                    } catch (Exception ignored) {}
                    return true;
                }
            }
            if (clickType == ClickType.RIGHT_CLICK && !stolen.isEmpty()) {
                Pair<AbilityRegistration, AbilityRegistration> pair = stolen.remove(cursor);
                cursor = 0;
                mix.removeAbility();

                getPlayer().sendMessage(pair.getLeft().getManifest().name()+" + "+pair.getRight().getManifest().name()+" 능력을 삭제합니다.");
                channel.update(null);
                return true;
            }
        }
        return false;
    }

    @Override
    public void TargetSkill(Material material, LivingEntity livingEntity) {
        if (mix != null && mix.usesMaterial(material)) {
           mix.TargetSkill(material, livingEntity);
        } else {
            if (material == Material.IRON_INGOT && getGame().isParticipating(livingEntity.getUniqueId())) {
                AbstractMix.MixParticipant participant = ((AbstractMix) getGame()).getParticipant(livingEntity.getUniqueId());
                if (participant.getAbility() != null) {
                    Mix mix = participant.getAbility();
                    if (mix.hasAbility()) {
                        if (mix.getSynergy() != null) {
                            Synergy synergy = mix.getSynergy();
                            AbilityFactory.AbilityRegistration first = SynergyFactory.getSynergyBase(synergy.getRegistration()).getLeft(),
                                    second = SynergyFactory.getSynergyBase(synergy.getRegistration()).getRight();

                            getPlayer().sendMessage(synergy.getDisplayName()+" 능력을 훔쳐옵니다.");
                            stolen.add(Pair.of(first, second));
                        } else {
                            getPlayer().sendMessage(mix.getFirst().getDisplayName()+" + "+mix.getSecond().getDisplayName()+" 능력을 훔쳐옵니다.");
                            stolen.add(Pair.of(mix.getFirst().getRegistration(), mix.getSecond().getRegistration()));
                        }
                        Seal.apply(participant, TimeUnit.SECONDS, 10);
                    }
                }
            }
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
