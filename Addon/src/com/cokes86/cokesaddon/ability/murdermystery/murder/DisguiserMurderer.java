package com.cokes86.cokesaddon.ability.murdermystery.murder;

import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.murdermystery.module.DisguiseModule;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.collect.Lists;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@AbilityManifest(name = "머더: 변장술사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "모든 시민을 죽이세요!",
        "금 우클릭으로 금 $[RIGHT_GOLD]개를 소모해 활과 화살을 얻을 수 있습니다.",
        "살인자의 검으로 상대를 죽일 경우 5초간 투명 효과를 받습니다.",
        "[§5변장§f] 금 좌클릭으로 $[LEFT_GOLD]개 소모해 생존중인 시민 중 한 명으로 변장합니다. $[LEFT_COOLDOWN]",
        "  이는 다시 §5변장§f하거나 누군가 하나 죽을 때 까지 유지됩니다.",
        "[§5바꿔치기§f] 금 F키로 $[F_GOLD]개 소모해 생존중인 시민 중 한 명을",
        "  다른 사람으로 변장시킵니다. $[F_COOLDOWN]",
        "  이는 누군가 하나 죽을 때 까지 유지됩니다."
})
public class DisguiserMurderer extends AbstractMurderer {
    private final int RIGHT_GOLD = 8;
    private final int LEFT_GOLD = 14;
    private final int F_GOLD = 18;

    private static final Config<Integer> LEFT_COOLDOWN = Config.cooldown(DisguiserMurderer.class, "disquise-cooldown", 10);
    private static final Config<Integer> F_COOLDOWN = Config.cooldown(DisguiserMurderer.class, "switch-cooldown", 10);

    private final Cooldown disquiseCooldown = new Cooldown(LEFT_COOLDOWN.getValue());
    private final Cooldown switchCooldown = new Cooldown(F_COOLDOWN.getValue());

    public DisguiserMurderer(Participant participant) {
        super(participant);
    }

    public DisguiseModule getModule() {
        return getGame().getModule(DisguiseModule.class);
    }

    @SubscribeEvent(onlyRelevant = true)
    private void onInteract(PlayerInteractEvent e) {
        final MurderMystery murderMystery = (MurderMystery) getGame();
        if (Items.isGold(e.getItem())) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (murderMystery.consumeGold(getParticipant(), RIGHT_GOLD)) {
                    if (!addArrow()) {
                        murderMystery.addGold(getParticipant(), RIGHT_GOLD);
                    } else {
                        if (!hasBow()) {
                            getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
                        }
                    }
                }
            }

            else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (!disquiseCooldown.isCooldown()) {
                    Participant participant = getRandomParticipant();
                    if (participant.equals(getParticipant()) || participant.getAbility() instanceof AbstractMurderer) {
                        onInteract(e);
                        return;
                    }
                    if (murderMystery.consumeGold(getParticipant(), LEFT_GOLD)) {
                        NMSUtil.changeSkin(getPlayer(), participant.getPlayer().getUniqueId());
                        NMSUtil.reloadPlayerData(getPlayer());
                        getPlayer().sendMessage("§c"+(participant.getPlayer().getName())+"으로 변장합니다.");
                        disquiseCooldown.start();
                    }
                }
            }
        }
    }

    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        final MurderMystery murderMystery = (MurderMystery) getGame();
        if (!switchCooldown.isCooldown()) {
            if (Items.isGold(e.getOffHandItem())) {
                e.setCancelled(true);
                Participant target = getRandomParticipant();
                Participant skin = getRandomParticipant();
                if (target.equals(getParticipant()) || target.equals(skin)) {
                    onPlayerSwapHandItems(e);
                    return;
                }
                if (target.getAbility() instanceof AbstractMurderer) {
                    onPlayerSwapHandItems(e);
                    return;
                }
                if (murderMystery.consumeGold(getParticipant(), F_GOLD)) {
                    NMSUtil.changeSkin(target.getPlayer(), skin.getPlayer().getUniqueId());
                    NMSUtil.reloadPlayerData(target.getPlayer());
                    switchCooldown.start();
                    target.getPlayer().sendMessage("§c[!] 변장술사가 당신을 "+(skin.getPlayer().getName())+"으로 변장시켰습니다.");
                    getPlayer().sendMessage("§c"+(target.getPlayer().getName())+"을 "+(skin.getPlayer().getName())+"으로 변장시켰습니다.");
                }
            }
        }
    }

    private Participant getRandomParticipant() {
        final MurderMystery murderMystery = (MurderMystery) getGame();
        Participant target = new Random().pick(Lists.newArrayList(murderMystery.getParticipants()));
        if (murderMystery.isDead(target.getPlayer().getUniqueId())) return getRandomParticipant();
        return target;
    }

    @Override
    protected void onUpdate(Update update) {
        super.onUpdate(update);
        if (update == Update.RESTRICTION_CLEAR) {
            NMS.sendTitle(getPlayer(), "§e직업§f: §5변장술사", "§7시민들을 변장해 혼란시키세요", 10, 80, 10);
            new AbilityTimer(1) {
                @Override
                protected void onEnd() {
                    NMS.clearTitle(getPlayer());
                }
            }.setInitialDelay(TimeUnit.SECONDS, 5).start();
            if (!getGame().hasModule(DisguiseModule.class)) getGame().addModule(new DisguiseModule());
            PASSIVE.stop(true);
            NMSUtil.saveSkinData();
        }
    }
}
