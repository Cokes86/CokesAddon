package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.list.PhantomThief;
import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import com.cokes86.cokesaddon.util.timer.InvincibilityTimer;
import com.cokes86.cokesaddon.util.timer.TimeoutTimer;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AbilityManifest(name = "대괴도", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c그레이트 팬텀§f: 자신 위치에 §7그림자§f를 $[DURATION]간 소환하고 자신은 §a은신§f합니다. $[COOLDOWN]",
        "  그림자를 공격한 플레이어는 $[DAMAGE]의 대미지를 주고",
        "  그 사람의 능력의 등급을 §b1단계 내려 재배정§f합니다.",
        "  재배정한 플레이어는 3초간 무적시간이 부여되며, 공격또한 불가능합니다.",
        "  그림자가 사라지면 ca은신§f또한 중간에 해제됩니다.",
        "  §8믹스능력자의 경우, 두 능력 모두 재배정합니다.",
        "§7사망 시 §8- §c본모습으로§f: 자신은 다른 시너지로 능력이 변경되고, 최대체력으로 회복합니다.",
        "§8[§7HIDDEN§8] §b구제§f: 누구를 구제하셨나요?"
})
public class GreatPhantom extends CokesSynergy implements ActiveHandler {
    private static final Config<Integer> DURATION = Config.of(GreatPhantom.class, "duration", 5, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 그레이트 팬텀 그림자 지속시간",
            "# 기본 값: 5 (초)");
    private static final Config<Integer> COOLDOWN = Config.of(GreatPhantom.class, "cooldown", 70, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 그레이트 팬텀 쿨타임",
            "# 기본 값: 70 (초)");
    private static final Config<Double> DAMAGE = Config.of(GreatPhantom.class, "damage", 17.0, FunctionalInterfaces.positive(),
            "# 그레이트 팬텀 도중 분신 공격 시 대미지",
            "# 기본 값: 17");

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final PhantomShow phantomShow = new PhantomShow();
    private int counter = 0;

    public GreatPhantom(AbstractGame.Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !phantomShow.isDuration()) {
            return phantomShow.start();
        }
        return false;
    }

    @SubscribeEvent(priority = 1000, eventPriority = EventPriority.HIGHEST, childs = {EntityDamageByBlockEvent.class, EntityDamageByEntityEvent.class})
    public void onBeforeDeath(EntityDamageEvent e) throws ReflectiveOperationException {
        if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
            Set<AbilityFactory.AbilityRegistration> synergies = SynergyFactory.getSynergies();
            synergies.remove(SynergyFactory.getSynergy(PhantomThief.class, PhantomThief.class));

            List<AbilityFactory.AbilityRegistration> registrations = new ArrayList<>(synergies);

            Random random = new Random();

            assert ((AbstractMix.MixParticipant) getParticipant()).getAbility() != null;
            ((AbstractMix.MixParticipant)getParticipant()).getAbility().setSynergy(random.pick(registrations));
            SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
            e.setDamage(0);
            getPlayer().setHealth(AttributeUtil.getMaxHealth(getPlayer()));
            Bukkit.broadcastMessage(getPlayer().getDisplayName()+": 지금부터 본 모습으로 상대해 드리죠.");
        }
    }

    @SubscribeEvent(priority = 1000, eventPriority = EventPriority.HIGHEST)
    public void onPlayerSetHealth(PlayerSetHealthEvent e) throws ReflectiveOperationException {
        if (e.getPlayer().equals(getPlayer()) && e.getHealth() <= 0) {
            Set<AbilityFactory.AbilityRegistration> synergies = SynergyFactory.getSynergies();
            synergies.remove(SynergyFactory.getSynergy(PhantomThief.class, PhantomThief.class));

            List<AbilityFactory.AbilityRegistration> registrations = new ArrayList<>(synergies);

            Random random = new Random();

            assert ((AbstractMix.MixParticipant) getParticipant()).getAbility() != null;
            ((AbstractMix.MixParticipant)getParticipant()).getAbility().setSynergy(random.pick(registrations));
            SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
            e.setCancelled(true);
            getPlayer().setHealth(AttributeUtil.getMaxHealth(getPlayer()));
            Bukkit.broadcastMessage(getPlayer().getDisplayName()+": 지금부터 본 모습으로 상대해 드리죠.");
        }
    }

    private void setNewAbility(AbstractGame.Participant target) {
        if (target instanceof AbstractMix.MixParticipant) {
            setNewAbility((AbstractMix.MixParticipant) target);
            return;
        }
        if (target.getAbility() != null) {
            AbilityManifest.Rank rank = target.getAbility().getRank();
            List<AbilityFactory.AbilityRegistration> returnAbilities = AbilityList.values().stream().filter(abilityRegistration -> {
                AbilityManifest.Rank rank1 = abilityRegistration.getManifest().rank();
                return (rank == AbilityManifest.Rank.SPECIAL && rank1 == AbilityManifest.Rank.L) || (rank == AbilityManifest.Rank.L && rank1 == AbilityManifest.Rank.S)
                        || (rank == AbilityManifest.Rank.S && rank1 == AbilityManifest.Rank.A) || (rank == AbilityManifest.Rank.A && rank1 == AbilityManifest.Rank.B)
                        || (rank == AbilityManifest.Rank.B && rank1 == AbilityManifest.Rank.C) || (rank == AbilityManifest.Rank.C && (rank1 == AbilityManifest.Rank.S || rank1 == AbilityManifest.Rank.L || rank1 == AbilityManifest.Rank.SPECIAL));
            }).collect(Collectors.toList());

            returnAbilities.removeIf(reg -> {
                if (Configuration.Settings.isBlacklisted(reg.getManifest().name())) return true;
                if (!reg.isAvailable(getGame().getClass())) return true;
                return !Configuration.Settings.isUsingBetaAbility() && reg.hasFlag(AbilityFactory.AbilityRegistration.Flag.BETA);
            });

            AbilityFactory.AbilityRegistration newOne = new Random().pick(returnAbilities);

            try {
                target.setAbility(newOne);
                target.getPlayer().sendMessage("[대괴도] 능력이 재배정되었습니다. 당신의 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                if (rank == AbilityManifest.Rank.C) {
                    getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 능력이 §eC등급§f이기에 <%s구제§f>하였습니다.",
                            target.getPlayer().getDisplayName(),
                            "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                }
                new InvincibilityTimer(target.getAbility(), 3, true).start();
            } catch (ReflectiveOperationException e) {
                getPlayer().sendMessage("[대괴도] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                e.printStackTrace();
            }
        }
    }

    private void setNewAbility(AbstractMix.MixParticipant target) {
        if (target.getAbility() != null && getParticipant().getAbility() != null) {
            if (target.getAbility().hasSynergy()) {
                int lowRank = 99, highRank = 0;
                int myRank1 = 6- SynergyFactory.getSynergyBase(target.getAbility().getSynergy().getRegistration()).getLeft().getManifest().rank().ordinal();
                int myRank2 = 6-SynergyFactory.getSynergyBase(target.getAbility().getSynergy().getRegistration()).getRight().getManifest().rank().ordinal();

                List<AbilityFactory.AbilityRegistration> returnAbilities = new ArrayList<>();
                for (AbilityFactory.AbilityRegistration abilityRegistration : SynergyFactory.getSynergies()) {
                    int checkRank1 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getLeft().getManifest().rank().ordinal();
                    int checkRank2 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getRight().getManifest().rank().ordinal();

                    if (checkRank1 + checkRank2 < lowRank) lowRank = checkRank1 + checkRank2;
                    if (checkRank1 + checkRank2 > highRank) highRank = checkRank1 + checkRank2;
                }
                for (AbilityFactory.AbilityRegistration abilityRegistration : SynergyFactory.getSynergies()) {
                    int checkRank1 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getLeft().getManifest().rank().ordinal();
                    int checkRank2 = 6-SynergyFactory.getSynergyBase(abilityRegistration).getRight().getManifest().rank().ordinal();
                    if (myRank1 + myRank2 == lowRank && checkRank1 + checkRank2 >= highRank - 3) {
                        returnAbilities.add(abilityRegistration);
                    } else if (myRank1 + myRank2 > checkRank1 + checkRank2){
                        returnAbilities.add(abilityRegistration);
                    }
                }

                returnAbilities.removeIf(reg -> {
                    if (Configuration.Settings.isBlacklisted(reg.getManifest().name())) return true;
                    if (!reg.isAvailable(getGame().getClass())) return true;
                    return !Configuration.Settings.isUsingBetaAbility() && reg.hasFlag(AbilityFactory.AbilityRegistration.Flag.BETA);
                });

                AbilityFactory.AbilityRegistration newOne = new Random().pick(returnAbilities);

                try {
                    target.getAbility().setSynergy(newOne);
                    target.getPlayer().sendMessage("[대괴도] 시너지 능력이 재배정되었습니다. 당신의 시너지 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                    getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 시너지 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                    if (myRank1 + myRank2 == lowRank) {
                        getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 능력의 등급이 가장 낮았기에 <%s구제§f>하였습니다.",
                                target.getPlayer().getDisplayName(),
                                "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                    }
                    new InvincibilityTimer(target.getAbility(), 3, true).start();
                } catch (ReflectiveOperationException e) {
                    getPlayer().sendMessage("[대괴도] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                    e.printStackTrace();
                }
            } else {
                Mix targetMix = target.getAbility();
                Mix myMix = (Mix) getParticipant().getAbility();
                if (targetMix.getFirst() != null && targetMix.getSecond() != null && myMix.getSynergy() instanceof GreatPhantom) {
                    AbilityManifest.Rank rankFirst = targetMix.getFirst().getRank();
                    List<AbilityFactory.AbilityRegistration> returnAbilitiesForFirst = AbilityList.values().stream().filter(abilityRegistration -> {
                        AbilityManifest.Rank rank1 = abilityRegistration.getManifest().rank();
                        return (rankFirst == AbilityManifest.Rank.SPECIAL && rank1 == AbilityManifest.Rank.L) || (rankFirst == AbilityManifest.Rank.L && rank1 == AbilityManifest.Rank.S)
                                || (rankFirst == AbilityManifest.Rank.S && rank1 == AbilityManifest.Rank.A) || (rankFirst == AbilityManifest.Rank.A && rank1 == AbilityManifest.Rank.B)
                                || (rankFirst == AbilityManifest.Rank.B && rank1 == AbilityManifest.Rank.C) || (rankFirst == AbilityManifest.Rank.C && (rank1 == AbilityManifest.Rank.S || rank1 == AbilityManifest.Rank.L || rank1 == AbilityManifest.Rank.SPECIAL));
                    }).collect(Collectors.toList());

                    returnAbilitiesForFirst.removeIf(reg -> {
                        if (Configuration.Settings.isBlacklisted(reg.getManifest().name())) return true;
                        if (!reg.isAvailable(getGame().getClass())) return true;
                        return !Configuration.Settings.isUsingBetaAbility() && reg.hasFlag(AbilityFactory.AbilityRegistration.Flag.BETA);
                    });

                    AbilityManifest.Rank rankSecond = targetMix.getSecond().getRank();
                    List<AbilityFactory.AbilityRegistration> returnAbilitiesForSecond = AbilityList.values().stream().filter(abilityRegistration -> {
                        AbilityManifest.Rank rank1 = abilityRegistration.getManifest().rank();
                        return (rankSecond == AbilityManifest.Rank.SPECIAL && rank1 == AbilityManifest.Rank.L) || (rankSecond == AbilityManifest.Rank.L && rank1 == AbilityManifest.Rank.S)
                                || (rankSecond == AbilityManifest.Rank.S && rank1 == AbilityManifest.Rank.A) || (rankSecond == AbilityManifest.Rank.A && rank1 == AbilityManifest.Rank.B)
                                || (rankSecond == AbilityManifest.Rank.B && rank1 == AbilityManifest.Rank.C) || (rankSecond == AbilityManifest.Rank.C && (rank1 == AbilityManifest.Rank.S || rank1 == AbilityManifest.Rank.L || rank1 == AbilityManifest.Rank.SPECIAL));
                    }).collect(Collectors.toList());

                    returnAbilitiesForFirst.removeIf(reg -> {
                        if (Configuration.Settings.isBlacklisted(reg.getManifest().name())) return true;
                        if (!reg.isAvailable(getGame().getClass())) return true;
                        return !Configuration.Settings.isUsingBetaAbility() && reg.hasFlag(AbilityFactory.AbilityRegistration.Flag.BETA);
                    });

                    AbilityFactory.AbilityRegistration newOneForFirst = new Random().pick(returnAbilitiesForFirst);
                    AbilityFactory.AbilityRegistration newOneForSecond = new Random().pick(returnAbilitiesForSecond);

                    try {
                        targetMix.setFirst(newOneForFirst);
                        targetMix.setSecond(newOneForSecond);
                        target.getPlayer().sendMessage(String.format("[대괴도] 능력이 재배정되었습니다. 당신의 능력은 §e%s§f + §e%s§f입니다.", newOneForFirst.getManifest().name(), newOneForSecond.getManifest().name()));
                        getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                        if (rankFirst == AbilityManifest.Rank.C) {
                            getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 첫번째 능력이 §eC등급§f이기에 <§%s구제§f>하였습니다.",
                                    target.getPlayer().getDisplayName(),
                                    newOneForFirst.getManifest().rank().getRankName().charAt(1)));
                        }

                        if (rankSecond == AbilityManifest.Rank.C) {
                            getPlayer().sendMessage(String.format("[대괴도] §e%s§f님의 두번째 능력이 §eC등급§f이기에 <§%s구제§f>하였습니다.",
                                    target.getPlayer().getDisplayName(),
                                    newOneForSecond.getManifest().rank().getRankName().charAt(1)));
                        }
                        SoundLib.ENTITY_PLAYER_LEVELUP.playSound(target.getPlayer().getLocation());
                        new InvincibilityTimer(target.getAbility(), 3, true).start();
                    } catch (ReflectiveOperationException e) {
                        getPlayer().sendMessage("[대괴도] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class PhantomShow extends Duration implements Listener {
        private IDummy phantom;

        public PhantomShow() {
            super(DURATION.getValue() * 20, cooldown);
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
            for (AbstractGame.Participant participant : getGame().getParticipants()) {
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
                    AbstractGame.Participant damager = getGame().getParticipant(damagerEntity.getUniqueId());
                    damager.getPlayer().damage(DAMAGE.getValue(), getPlayer());
                    phantom.remove();
                    setNewAbility(damager);
                    phantomShow.stop(false);
                    counter++;
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
