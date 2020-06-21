package cokes86.addon.gamemodes.boomcatch;

import static daybreak.abilitywar.game.GameManager.stopGame;

import java.util.ArrayList;
import java.util.Random;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import cokes86.addon.configuration.gamemode.GameConfiguration.Config;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.Bar;
import daybreak.abilitywar.utils.base.minecraft.FireworkUtil;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name = "폭탄 넘기기", description = { "§f최소 2명플레이! 폭탄을 들고 있는 사람을 피해 생존하자!", "§f폭탄을 받았어? 그러면 폭탄을 얼른 넘겨줘!!" })
public class BoomCatch extends AbstractGame implements Winnable, AbstractGame.Observer, Listener {
	private ArrayList<Participant> live;
	private int maxround = Config.getInt(GameNodes.Boom_round);
	private int ready = Config.getInt(GameNodes.Boom_readytime);
	private int gaming = Config.getInt(GameNodes.Boom_gamingTime);
	private int time;
	private daybreak.abilitywar.utils.base.minecraft.Bar bar;
	private int round;

	public BoomCatch() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	protected void onStart() {
		live = new ArrayList<>();
		for (Participant p : getParticipants()) {
			live.add(p);
		}
		
		this.time = 0;
		this.round = 0;
	}

	@Override
	protected void run(int arg0) {
		time++;
		int gamestart = 10+ready;
		int gameend = gamestart + gaming;
		
		for (Participant p : live) {
			p.getPlayer().setFoodLevel(19);
		}
		
		switch (time) {
		case 1: // Participant List
			Bukkit.broadcastMessage("§e====== §a게임 참여자 목록 §e======");
			StringBuilder build = new StringBuilder("");
			StringJoiner joiner = new StringJoiner("§f, §a", "§a", "§f.");
			for (Participant p : getParticipants()) {
				joiner.add(p.getPlayer().getName());
			}
			build.append(joiner.toString());
			Bukkit.broadcastMessage(build.toString());
			Bukkit.broadcastMessage("총 §a" + getParticipants().size() + "명§f 참가");
			Bukkit.broadcastMessage("§e========================");

			if (getParticipants().size() < 2) {
				stopGame();
				Bukkit.broadcastMessage("§c최소 참가자수 2명을 충족하지 못하였습니다. 게임이 종료되었습니다.");
				break;
			}
			break;
		case 4: // Game Maker
			Bukkit.broadcastMessage("§b코크스애드온 §f폭탄 술래잡기 게임모드 제작자 : §aCokes_86");

			GameCreditEvent event = new GameCreditEvent();
			Bukkit.getPluginManager().callEvent(event);
			for (String s : event.getCreditList()) {
				Bukkit.broadcastMessage(s);
			}
			break;
		case 6: // Rule
			Bukkit.broadcastMessage("총 §d"+maxround+"라운드§f로 진행되며, 생존자 중 20%가 폭탄을 가집니다. (20%로 1명이 안될 경우, 1명만 폭탄을 가집니다.)");
			Bukkit.broadcastMessage("라운드 당 폭탄을 가지고 있지 않는 자는 §a"+ TimeUtil.parseTimeAsString(gaming)+"§f을 버티면 생존합니다.");
			Bukkit.broadcastMessage("§c폭탄§f을 가지게 되면, 상대방을 공격할 시 전달됩니다.");
			Bukkit.broadcastMessage(maxround+"라운드를 모두 버티거나, 남은 생존자가 1명일 경우 §a승리§f합니다.");
			bar = new Bar("게임 진행 상황", BarColor.WHITE, BarStyle.SEGMENTED_10);
			bar.setProgress(0.00);
			break;
		case 8: // Setup
			if (round == maxround) {
				Participant[] lives = new Participant[live.size()];
				for (int a = 0; a < live.size(); a++) {
					lives[a] = live.get(a);
				}
				BoomCatch.this.Win(lives);
			}
			if (live.size() == 1) {
				BoomCatch.this.Win(live.get(0));
			}
			
			round++;
			break;
		case 10: // Ready
			startGame();
			Bukkit.broadcastMessage("잠시 후 §d" + round + "라운드§f가 시작됩니다.");
			setRestricted(false);
			break;	
		}
		
		if (time >= 10 && time < gamestart) {  // Ready Time
			int seconds = gamestart - time;
			bar.setTitle(round+"라운드 대기 시간 "+TimeUtil.parseTimeAsString(seconds));
			bar.setProgress((double) seconds / ready);
			bar.setColor(BarColor.GREEN);
			
			if (seconds == ready / 2 || (seconds <= 5 && seconds >= 1)) {
				Bukkit.broadcastMessage("§d"+round + "라운드 §f시작까지 §a" + TimeUtil.parseTimeAsString(seconds) + " §f남았습니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			}
		}
		
		if (time == gamestart) {  // Start
			Bukkit.broadcastMessage("§d"+round + "라운드가 시작되었습니다!");
			bar.setTitle(round + "라운드 종료까지 "+TimeUtil.parseTimeAsString(gaming)+" 남았습니다.");
			bar.setColor(BarColor.RED);
			bar.setProgress(1.00);
			SoundLib.ENTITY_PLAYER_LEVELUP.broadcastSound();
			
			if ((int)(live.size() / 5) >= 1)
				drawSeak(live, (int)(live.size() / 5));
			else
				drawSeak(live, 1);
		}
		if (time > gamestart && time < gameend) {  // In gaming
			int count = gameend - time;
			bar.setTitle(round + "라운드 종료까지 " + TimeUtil.parseTimeAsString(count) + " 남았습니다.");
			bar.setProgress(Math.min((double) count / gaming, 1.0D));

			if (count == gaming / 2 || (count <= 5 && gaming >= 1)) {
				Bukkit.broadcastMessage("§d"+round + "라운드 §f종료까지 §a" + TimeUtil.parseTimeAsString(count) + " §f남았습니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			}
		}
		if (time == gameend) {  // Round End
			Bukkit.broadcastMessage("§d"+round + "라운드§f가 종료되었습니다!");
			StringJoiner join = new StringJoiner("님§f, §a", "§a", "§f");
			ArrayList<Participant> list = new ArrayList<>(live);
			for (Participant p : list) {
				if (p.hasAbility() && p.getAbility().getClass().equals(Seak.class)) {
					live.remove(p);
					p.getPlayer().getInventory().clear();
					p.getPlayer().setGameMode(GameMode.SPECTATOR);
					p.removeAbility();
					join.add(p.getPlayer().getName());
				}
			}
			Bukkit.broadcastMessage(join.toString() + "§f이 §c탈락§f하였습니다!");
			SoundLib.ENTITY_GENERIC_EXPLODE.broadcastSound();
			
			bar.setTitle(round+"라운드 게임 종료!");
			bar.setColor(BarColor.WHITE);
			
			time = 7;
		}
	}
	
	protected void onEnd() {
		if (bar != null) {
			bar.remove();
			bar = null;
		}
		for (Participant p : getParticipants()) {
			p.getPlayer().getInventory().clear();
		}
	}
	
	private void drawSeak(ArrayList<Participant> ps, int count) {
		if (count == 0)
			return;
		ArrayList<Participant> List = new ArrayList<>(ps);
		Random r = new Random();
		int random = r.nextInt(List.size());
		Participant p = List.get(random);
		try {
			p.setAbility(Seak.class);
		} catch (Exception e) {
			Messager.sendConsoleMessage("폭탄을 배정하는 도중 오류가 발생하였습니다!");
			e.printStackTrace();
		}
		Bukkit.broadcastMessage("§a" + p.getPlayer().getName() + "§f님이 §c폭탄§f을 가지게 되었습니다!");
		p.getPlayer().getInventory().addItem(new ItemStack(Material.IRON_INGOT));
		p.getAbility().setRestricted(false);
		List.remove(p);
		drawSeak(List, count - 1);
	}

	public boolean isEliminated(Participant p) {
		if (live.contains(p))
			return false;
		else
			return true;
	}

	public void Win(Participant... winners) {
		StringBuilder builder = new StringBuilder(ChatColor.translateAlternateColorCodes('&', "&5&l최종 생존자&f: "));
		StringJoiner joiner = new StringJoiner(ChatColor.WHITE + ", " + ChatColor.LIGHT_PURPLE,
				ChatColor.LIGHT_PURPLE.toString(), ChatColor.WHITE + ".");
		for (BoomCatch.Participant participant : winners) {
			final Player p = participant.getPlayer();
			joiner.add(p.getName());
			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
			(new SimpleTimer(TaskType.REVERSE, 8) {
				@Override
				protected void run(int arg0) {
					FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
				}
			}).setPeriod(TimeUnit.TICKS, 8).start();
		}
		builder.append(joiner.toString());
		Bukkit.broadcastMessage(builder.toString());

		for (Participant p : getParticipants()) {
			p.getPlayer().setGameMode(GameMode.SURVIVAL);
		}

		stopGame();
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (isParticipating(player)) {
				e.setDamage(0);
			}
		}
	}

	@Override
	public void update(GameUpdate gameUpdate) {
		if (gameUpdate.equals(GameUpdate.END))
			HandlerList.unregisterAll(this);
	}
}
