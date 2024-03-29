package com.cokes86.cokesaddon.game.gamemode.tailcatch;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.util.AttributeUtil;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.Category.GameCategory;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@GameManifest(name = "꼬리잡기", description = {
        "§f나 잡아봐라",
        "§f목표를 잡으면 새로운 목표가! 마지막까지 생존하세요!"
})
@Category(GameCategory.GAME)
@GameAliases(value = {"꼬잡", "꼬리"})
public class TailCatch extends Game implements DefaultKitHandler, Winnable, NoticeTail.Handler {
    private final List<Participant> tail = new ArrayList<>();
    private final NoticeTail noticeTail = addModule(new NoticeTail(this));

    public TailCatch() throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
        setRestricted(Settings.InvincibilitySettings.isEnabled());
    }

    @Override
    protected void progressGame(int i) {
        switch (i) {
            case 1:
                List<String> lines = Messager.asList("§6==== §e게임 참여자 목록 §6====");
                int count = 0;
                for (Participant p : getParticipants()) {
                    count++;
                    lines.add("§a" + count + ". §f" + p.getPlayer().getName());
                }
                lines.add("§e총 인원수 : " + count + "명");
                lines.add("§6===========================");

                for (String line : lines) {
                    Bukkit.broadcastMessage(line);
                }

                if (getParticipants().size() < 2) {
                    stop();
                    Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§72명§8)");
                }
                break;
            case 3:
                lines = Messager.asList(
                        "§cTailCatch §f- §6꼬리잡기 - 나 잡아 봐라",
                        "§e버전 §7: §f" + CokesAddon.getAddon().getDescription().getVersion(),
                        "§b개발자 §7: §fCokes_86 코크스",
                        "§9디스코드 §7: §f코크스스§7#9329"
                );

                GameCreditEvent event = new GameCreditEvent(this);
                Bukkit.getPluginManager().callEvent(event);
                lines.addAll(event.getCredits());

                for (String line : lines) {
                    Bukkit.broadcastMessage(line);
                }
                break;
            case 5:
                if (Settings.getDrawAbility()) {
                    for (String line : Messager.asList(
                            "§f플러그인에 총 §b" + AbilityList.nameValues().size() + "개§f의 능력이 등록되어 있습니다.",
                            "§7능력을 무작위로 할당합니다...")) {
                        Bukkit.broadcastMessage(line);
                    }
                    try {
                        startAbilitySelect();
                    } catch (OperationNotSupportedException ignored) {
                    }
                }
                break;
            case 6:
                if (Settings.getDrawAbility()) {
                    Bukkit.broadcastMessage("§f모든 참가자가 능력을 §b확정§f했습니다.");
                } else {
                    Bukkit.broadcastMessage("§f능력자 게임 설정에 따라 §b능력§f을 추첨하지 않습니다.");
                }
                break;
            case 8:
                Bukkit.broadcastMessage("§e잠시 후 게임이 시작됩니다.");
                break;
            case 10:
                Bukkit.broadcastMessage("§e게임이 §c5§e초 후에 시작됩니다.");
                SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
                break;
            case 11:
                Bukkit.broadcastMessage("§e게임이 §c4§e초 후에 시작됩니다.");
                SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
                break;
            case 12:
                Bukkit.broadcastMessage("§e게임이 §c3§e초 후에 시작됩니다.");
                SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
                break;
            case 13:
                Bukkit.broadcastMessage("§e게임이 §c2§e초 후에 시작됩니다.");
                SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
                break;
            case 14:
                Bukkit.broadcastMessage("§e게임이 §c1§e초 후에 시작됩니다.");
                SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
                break;
            case 15:
                if (Seasons.isChristmas()) {
                    final String blocks = Strings.repeat("§c■§2■", 22);
                    Bukkit.broadcastMessage(blocks);
                    Bukkit.broadcastMessage("§f            §cTailCatch §f- §2꼬리잡기 - 나 잡아 봐라  ");
                    Bukkit.broadcastMessage("§f                   게임 시작                ");
                    Bukkit.broadcastMessage(blocks);
                } else {
                    for (String line : Messager.asList(
                            "§e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
                            "§f             §cTailCatch §f- §6꼬리잡기 - 나 잡아 봐라  ",
                            "§f                    게임 시작                ",
                            "§e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
                        Bukkit.broadcastMessage(line);
                    }
                }

                giveDefaultKit(getParticipants());

                if (Settings.getSpawnEnable()) {
                    Location spawn = Settings.getSpawnLocation().toBukkitLocation();
                    for (Participant participant : getParticipants()) {
                        participant.getPlayer().teleport(spawn);
                    }
                }

                if (Settings.getNoHunger()) {
                    Bukkit.broadcastMessage("§2배고픔 무제한§a이 적용됩니다.");
                } else {
                    Bukkit.broadcastMessage("§4배고픔 무제한§c이 적용되지 않습니다.");
                }

                if (Settings.getInfiniteDurability()) {
                    addModule(new InfiniteDurability());
                } else {
                    Bukkit.broadcastMessage("§4내구도 무제한§c이 적용되지 않습니다.");
                }

                if (Settings.getClearWeather()) {
                    for (World world : Bukkit.getWorlds()) world.setStorm(false);
                }

                if (isRestricted()) {
                    getInvincibility().start(false);
                } else {
                    Bukkit.broadcastMessage("§4초반 무적§c이 적용되지 않습니다.");
                    setRestricted(false);
                }

                ScriptManager.runAll(this);

                startGame();

                tail.addAll(getParticipants());
                Collections.shuffle(tail);
                noticeTail.updateBossBar();
                break;
        }
    }

    @Override
    protected @NotNull DeathManager newDeathManager() {
        return new TailCatchDeathManager(this);
    }

    protected @NotNull Participant getNextTail(Participant participant) {
        int index = tail.indexOf(participant);
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        int nextIndex = index == tail.size()-1 ? 0 : index + 1;
        return tail.get(nextIndex);
    }

    protected boolean removeTail(Participant participant) {
        return tail.remove(participant);
    }

    @Override
    public NoticeTail getNoticeTail() {
        return noticeTail;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Entity) {
                damager = (Entity) projectile.getShooter();
            }
        }

        if (damager instanceof Player && e.getEntity() instanceof Player) {
            Participant entityParticipant = getParticipant(e.getEntity().getUniqueId());
            Participant damagerParticipant = getParticipant(damager.getUniqueId());

            int beforeIndex = tail.indexOf(entityParticipant) == 0 ? tail.size()-1 : tail.indexOf(entityParticipant) - 1;
            if (tail.get(beforeIndex).equals(damagerParticipant)) {
                e.setDamage(0);
                AttributeUtil.setMaxHealth(damagerParticipant.getPlayer(), AttributeUtil.getMaxHealth(damagerParticipant.getPlayer()) * 2 / 3);
            }
        }
    }
}
