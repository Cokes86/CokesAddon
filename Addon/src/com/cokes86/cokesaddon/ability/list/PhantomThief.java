package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import com.cokes86.cokesaddon.util.timer.InvincibilityTimer;
import com.cokes86.cokesaddon.util.timer.TimeoutTimer;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.AbstractMix.MixParticipant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AbilityManifest(name = "팬텀 시프", rank = Rank.S, species = Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c팬텀 쇼§f: 자신 위치에 §7그림자§f를 3초간 소환하고 자신은 §a은신§f합니다. $[COOLDOWN]",
        "  그림자를 공격한 플레이어는 $[DAMAGE]의 대미지를 주고",
        "  그 사람의 능력의 등급을 §b1단계 내려 재배정§f합니다.",
        "  재배정한 플레이어는 3초간 무적시간이 부여되며, 공격또한 불가능합니다.",
        "  그림자가 사라지면 §a은신§f또한 중간에 해제됩니다.",
        "§8[§7HIDDEN§8] §b구제§f: 누구를 구제하셨나요?"
})
@NotAvailable({AbstractTripleMix.class})
public class PhantomThief extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> COOLDOWN = Config.of(PhantomThief.class, "cooldown", 120, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 팬텀 쇼 쿨타임",
            "# 기본 값: 120 (초)");
    private static final Config<Double> DAMAGE = Config.of(PhantomThief.class, "damage", 3.5, FunctionalInterfaces.positive(),
            "# 팬텀 쇼 도중 분신 공격 시 대미지",
            "# 기본 값: 3.5");

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final PhantomShow phantomShow = new PhantomShow();

    public PhantomThief(Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !phantomShow.isDuration()) {
            return phantomShow.start();
        }
        return false;
    }

    private boolean setNewAbility(Participant target) {
        if (target instanceof MixParticipant) {
            return setNewAbility((MixParticipant) target);
        }
        if (target.getAbility() != null) {
            Rank rank = target.getAbility().getRank();
            List<AbilityRegistration> returnAbilities = AbilityList.values().stream().filter(abilityRegistration -> {
                Rank rank1 = abilityRegistration.getManifest().rank();
                if (abilityRegistration.getAbilityClass().getAnnotation(Beta.class) != null) return false;
                return (rank == Rank.SPECIAL && rank1 == Rank.L) || (rank == Rank.L && rank1 == Rank.S)
                        || (rank == Rank.S && rank1 == Rank.A) || (rank == Rank.A && rank1 == Rank.B)
                        || (rank == Rank.B && rank1 == Rank.C) || (rank == Rank.C && (rank1 == Rank.S || rank1 == Rank.L || rank1 == Rank.SPECIAL));
            }).collect(Collectors.toList());

            AbilityRegistration newOne = new Random().pick(returnAbilities);

            try {
                target.setAbility(newOne);
                target.getPlayer().sendMessage("[팬텀 시프] 능력이 재배정되었습니다. 당신의 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                if (rank == Rank.C) {
                    getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 능력이 §eC등급§f이기에 <%s구제§f>하였습니다.",
                            target.getPlayer().getDisplayName(),
                            "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                }
                return new InvincibilityTimer(target.getAbility(), 3, true).start();
            } catch (ReflectiveOperationException e) {
                getPlayer().sendMessage("[팬텀 시프] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private boolean setNewAbility(MixParticipant target) {
        if (target.getAbility() != null && getParticipant().getAbility() != null) {
            if (target.getAbility().hasSynergy()) {
                int lowRank = 99, highRank = 0;
                int myRank1 = 6-SynergyFactory.getSynergyBase(target.getAbility().getSynergy().getRegistration()).getLeft().getManifest().rank().ordinal();
                int myRank2 = 6-SynergyFactory.getSynergyBase(target.getAbility().getSynergy().getRegistration()).getRight().getManifest().rank().ordinal();

                List<AbilityRegistration> returnAbilities = new ArrayList<>();
                for (AbilityRegistration abilityRegistration : SynergyFactory.getSynergies()) {
                    int checkRank1 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getLeft().getManifest().rank().ordinal();
                    int checkRank2 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getRight().getManifest().rank().ordinal();

                    if (checkRank1 + checkRank2 < lowRank) lowRank = checkRank1 + checkRank2;
                    if (checkRank1 + checkRank2 > highRank) highRank = checkRank1 + checkRank2;
                }
                for (AbilityRegistration abilityRegistration : SynergyFactory.getSynergies()) {
                    int checkRank1 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getLeft().getManifest().rank().ordinal();
                    int checkRank2 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getRight().getManifest().rank().ordinal();
                    if (myRank1 + myRank2 == lowRank && checkRank1 + checkRank2 >= highRank - 3) {
                        returnAbilities.add(abilityRegistration);
                    } else if (myRank1 + myRank2 > checkRank1 + checkRank2){
                        returnAbilities.add(abilityRegistration);
                    }
                }

                AbilityRegistration newOne = new Random().pick(returnAbilities);

                try {
                    target.getAbility().setSynergy(newOne);
                    target.getPlayer().sendMessage("[팬텀 시프] 시너지 능력이 재배정되었습니다. 당신의 시너지 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                    getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 시너지 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                    if (myRank1 + myRank2 == lowRank) {
                        getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 능력의 등급이 가장 낮았기에 <%s구제§f>하였습니다.",
                                target.getPlayer().getDisplayName(),
                                "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                    }
                    return new InvincibilityTimer(target.getAbility(), 3, true).start();
                } catch (ReflectiveOperationException e) {
                    getPlayer().sendMessage("[팬텀 시프] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                    e.printStackTrace();
                    return false;
                }
            } else {
                Mix targetMix = target.getAbility();
                Mix myMix = (Mix) getParticipant().getAbility();
                if (targetMix.getFirst() != null && myMix.getFirst().getClass().equals(PhantomThief.class)) {
                    Rank rank = targetMix.getFirst().getRank();
                    List<AbilityRegistration> returnAbilities = AbilityList.values().stream().filter(abilityRegistration -> {
                        Rank rank1 = abilityRegistration.getManifest().rank();
                        return (rank == Rank.SPECIAL && rank1 == Rank.L) || (rank == Rank.L && rank1 == Rank.S)
                                || (rank == Rank.S && rank1 == Rank.A) || (rank == Rank.A && rank1 == Rank.B)
                                || (rank == Rank.B && rank1 == Rank.C) || (rank == Rank.C && (rank1 == Rank.S || rank1 == Rank.L || rank1 == Rank.SPECIAL));
                    }).collect(Collectors.toList());
        
                    AbilityRegistration newOne = new Random().pick(returnAbilities);

                    try {
                        targetMix.setFirst(newOne);
                        target.getPlayer().sendMessage("[팬텀 시프] 첫번째 능력이 재배정되었습니다. 당신의 첫번째 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                        getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 첫번째 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                        if (rank == Rank.C) {
                            getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 첫번째 능력이 §eC등급§f이기에 <%s구제§f>하였습니다.",
                                    target.getPlayer().getDisplayName(),
                                    "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                        }
                        SoundLib.ENTITY_PLAYER_LEVELUP.playSound(target.getPlayer().getLocation());
                        return new InvincibilityTimer(target.getAbility(), 3, true).start();
                    } catch (ReflectiveOperationException e) {
                        getPlayer().sendMessage("[팬텀 시프] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                        e.printStackTrace();
                        return false;
                    }
                }

                else if (targetMix.getSecond() != null && myMix.getSecond().getClass().equals(PhantomThief.class)) {
                    Rank rank = targetMix.getSecond().getRank();
                    List<AbilityRegistration> returnAbilities = AbilityList.values().stream().filter(abilityRegistration -> {
                        Rank rank1 = abilityRegistration.getManifest().rank();
                        return (rank == Rank.SPECIAL && rank1 == Rank.L) || (rank == Rank.L && rank1 == Rank.S)
                                || (rank == Rank.S && rank1 == Rank.A) || (rank == Rank.A && rank1 == Rank.B)
                                || (rank == Rank.B && rank1 == Rank.C) || (rank == Rank.C && (rank1 == Rank.S || rank1 == Rank.L || rank1 == Rank.SPECIAL));
                    }).collect(Collectors.toList());
        
                    AbilityRegistration newOne = new Random().pick(returnAbilities);

                    try {
                        targetMix.setSecond(newOne);
                        target.getPlayer().sendMessage("[팬텀 시프] 두번째 능력이 재배정되었습니다. 당신의 두번째 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                        getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 두번째 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                        if (rank == Rank.C) {
                            getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 두번째 능력이 §eC등급§f이기에 <%s구제§f>하였습니다.",
                                    target.getPlayer().getDisplayName(),
                                    "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                        }
                        return new InvincibilityTimer(target.getAbility(), 3, true).start();
                    } catch (ReflectiveOperationException e) {
                        getPlayer().sendMessage("[팬텀 시프] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private class PhantomShow extends Duration implements Listener {
        private IDummy phantom;

        public PhantomShow() {
            super(60, cooldown);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onDurationStart() {
            if (phantom != null) {
                phantom.remove();
                phantom = null;
            }
            phantom = NMSUtil.createDummy(getPlayer().getLocation().clone(), getPlayer());
            phantom.getBukkitEntity().getInventory().setStorageContents(getPlayer().getInventory().getStorageContents());
            phantom.getBukkitEntity().getInventory().setArmorContents(getPlayer().getInventory().getArmorContents());
            phantom.getBukkitEntity().getInventory().setHeldItemSlot(getPlayer().getInventory().getHeldItemSlot());
            for (Participant participant : getGame().getParticipants()) {
                phantom.display(participant.getPlayer());
            }
            phantom.getBukkitEntity().getLocation().setPitch(getPlayer().getLocation().clone().getPitch());
            phantom.getBukkitEntity().getLocation().setYaw(getPlayer().getLocation().clone().getYaw());
            phantom.getBukkitEntity().getLocation().setDirection(getPlayer().getLocation().getDirection());
            NMSUtil.hidePlayer(getParticipant());
            getParticipant().attributes().TARGETABLE.setValue(false);
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());

            final RGB rgb = new RGB(51,51,51);
            for (Location l : Circle.iteratorOf(phantom.getBukkitEntity().getLocation(), 1, 15).iterable()) {
                l.setY(LocationUtil.getFloorYAt(phantom.getBukkitEntity().getWorld(), l.getY(), l.getBlockX(), l.getBlockZ()));
                ParticleLib.REDSTONE.spawnParticle(l.clone().add(0,0.1,0), rgb);
                ParticleLib.REDSTONE.spawnParticle(l.clone().add(0,0.5,0), rgb);
                ParticleLib.REDSTONE.spawnParticle(l.clone().add(0,1.0,0), rgb);
                ParticleLib.REDSTONE.spawnParticle(l.clone().add(0,1.5,0), rgb);
                ParticleLib.REDSTONE.spawnParticle(l.clone().add(0,2.0,0), rgb);
            }
            SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(phantom.getBukkitEntity().getLocation().clone(), 1.0f, 0.1f);
        }

        @Override
        protected void onDurationEnd() {
            onDurationSilentEnd();
        }

        @Override
        protected void onDurationSilentEnd() {
            phantom.getBukkitEntity().getInventory().clear();
            phantom.remove();
            NMSUtil.showPlayer(getParticipant());
            getParticipant().attributes().TARGETABLE.setValue(true);
            phantom = null;
            HandlerList.unregisterAll(this);

            armorReset();
        }

        private void armorReset() {
            ItemStack[] armor = getPlayer().getInventory().getArmorContents();
            getPlayer().getInventory().setArmorContents(new ItemStack[]{});
            TimeoutTimer.start(TimeUnit.TICKS, 1, () -> getPlayer().getInventory().setArmorContents(armor));
        }

        @Override
        protected void onDurationProcess(int i) {}

        @EventHandler
        public void onEntityDamage(EntityDamageByEntityEvent e) {
            Entity damagerEntity = CokesUtil.getDamager(e.getDamager());
            if (phantom != null && e.getEntity().getUniqueId().equals(phantom.getUniqueID())) {
                e.setDamage(0);
                if (getGame().isParticipating(damagerEntity.getUniqueId()) && !damagerEntity.equals(getPlayer())) {
                    Participant damager = getGame().getParticipant(damagerEntity.getUniqueId());
                    damager.getPlayer().damage(DAMAGE.getValue(), getPlayer());
                    phantom.remove();
                    setNewAbility(damager);
                    phantomShow.stop(false);
                }
            }
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e) {
            if (e.getEntity().getUniqueId().equals(phantom.getUniqueID())) {
                e.setKeepInventory(true);
            }
        }
    }
}
