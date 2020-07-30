package cokes86.addon.gamemodes.battleability;

import cokes86.addon.configuration.gamemode.GameConfiguration.Config;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.event.InvincibilityStatusChangeEvent;
import daybreak.abilitywar.game.manager.object.Invincibility;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.minecraft.Bar;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class Border extends Invincibility {
	private final int duration = Settings.InvincibilitySettings.getDuration();
	private final boolean isBossbarEnabled = Settings.InvincibilitySettings.isBossbarEnabled();
	private final String bossbarMessage = Settings.InvincibilitySettings.getBossbarMessage(), bossbarInfiniteMessage = Settings.InvincibilitySettings.getBossbarInfiniteMessage();

	private final int size = Config.getInt(GameNodes.BattleAbility_startSize);
	private final int time = Config.getInt(GameNodes.BattleAbility_time);
	private final World world = Bukkit.getWorld(Settings.getSpawnLocation().world);
	private final WorldBorder wb = world.getWorldBorder();

	private final Game game;
	private AbstractGame.GameTimer timer;
	
	public Border(Game game) {
		super(game);
		this.game = game;
		Bukkit.getPluginManager().registerEvent(EntityDamageEvent.class, game, EventPriority.HIGH, this,
				AbilityWar.getPlugin());
	}

	@Override
	public boolean start(boolean isInfinite) {
		if (timer == null || !timer.isRunning()) {
			if (!isInfinite) {
				this.timer = new InvincibilityTimer(duration);
			} else {
				this.timer = new InvincibilityTimer();
			}
			timer.start();
			return true;
		}
		return false;
	}

	@Override
	public boolean start(final int duration) {
		if (timer == null || !timer.isRunning()) {
			this.timer = new InvincibilityTimer(duration);
			timer.start();
			return true;
		}
		return false;
	}

	@Override
	public boolean stop() {
		if (timer != null && timer.isRunning()) {
			timer.stop(false);
			timer = null;
			return true;
		}
		return false;
	}

	public boolean isEnabled() {
		return this.timer != null && this.timer.isRunning();
	}

	@Override
	public void execute(Listener listener, Event event) {
		if (!isEnabled()) return;
		if (event instanceof EntityDamageEvent) {
			EntityDamageEvent e = (EntityDamageEvent) event;
			if (e.getEntity() instanceof Player && game.isParticipating(e.getEntity().getUniqueId())) {
				e.setCancelled(true);
			}
		}
	}

	private class InvincibilityTimer extends AbstractGame.GameTimer {

		private Bar bossBar = null;
		private final String startMessage;

		private InvincibilityTimer(int duration) {
			game.super(TaskType.REVERSE, duration);
			this.startMessage = ChatColor.GREEN + "무적이 " + ChatColor.WHITE + TimeUtil.parseTimeAsString(duration) + ChatColor.GREEN + "동안 적용됩니다.";
			if (isBossbarEnabled) {
				int[] time = TimeUtil.parseTime(duration);
				bossBar = new Bar(String.format(bossbarMessage, time[0], time[1]), BarColor.GREEN, BarStyle.SEGMENTED_10);
			}
		}

		private InvincibilityTimer() {
			game.super(TaskType.INFINITE, -1);
			this.startMessage = ChatColor.GREEN + "무적이 적용되었습니다. 지금부터 무적이 해제될 때까지 대미지를 입지 않습니다.";
			if (isBossbarEnabled) {
				bossBar = new Bar(bossbarInfiniteMessage, BarColor.GREEN, BarStyle.SEGMENTED_10);
			}
		}

		@Override
		protected void onStart() {
			game.setRestricted(true);
			Bukkit.broadcastMessage(startMessage);
			wb.setCenter(Settings.getSpawnLocation().toBukkitLocation());
			wb.setSize(size);
			Bukkit.getPluginManager().callEvent(new InvincibilityStatusChangeEvent(game, true));
		}

		@Override
		protected void run(int count) {
			if (getTaskType() != TaskType.INFINITE) {
				if (bossBar != null) {
					int[] time = TimeUtil.parseTime(count);
					bossBar.setTitle(String.format(bossbarMessage, time[0], time[1])).setProgress(Math.min(count / (double) getMaximumCount(), 1.0));
				}
				if (count == (getMaximumCount()) / 2 || (count <= 5 && count >= 1)) {
					Bukkit.broadcastMessage("§a무적이 §f" + TimeUtil.parseTimeAsString(count) + " §a후에 해제됩니다.");
					SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				}
			}
		}

		@Override
		protected void onEnd() {
			if (bossBar != null) {
				bossBar.remove();
			}
			game.setRestricted(false);
			Bukkit.broadcastMessage(ChatColor.GREEN + "무적이 해제되었습니다. 지금부터 대미지를 입습니다.");
			SoundLib.ENTITY_ENDER_DRAGON_GROWL.broadcastSound();
			Bukkit.getPluginManager().callEvent(new InvincibilityStatusChangeEvent(game, false));
			wb.setSize(1, time);
		}

		@Override
		protected void onSilentEnd() {
			if (bossBar != null) {
				bossBar.remove();
			}
		}

	}
}
