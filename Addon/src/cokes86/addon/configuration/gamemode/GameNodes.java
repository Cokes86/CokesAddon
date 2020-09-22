package cokes86.addon.configuration.gamemode;

import cokes86.addon.gamemodes.battleability.BattleAbility;
import cokes86.addon.gamemodes.tailcatching.TailCatching;
import cokes86.addon.gamemodes.targethunting.TargetHunting;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManifest;

public enum GameNodes {
	BATTLE_ABILITY_START_SIZE(BattleAbility.class, "초기사이즈(블럭)", 200),
	BATTLE_ABILITY_TIME(BattleAbility.class, "게임시간(초)", 600),
	TAIL_CATCHING_RANGE(TailCatching.class, "안내거리", 10, "#몇블럭 이내에 있을 시 엑션바에 안내할 지 설정"),
	TARGET_HUNTING_INV(TargetHunting.class, "무적시간", 60),
	TARGET_HUNTING_GAME_TIME(TargetHunting.class, "게임시간", 300);

	private final String path;
	private final Object defaultValue;
	private final String[] comments;
	private final Class<? extends AbstractGame> clazz;

	GameNodes(Class<? extends AbstractGame> clazz, String path, Object defaultValue, String... comments) {
		this.path = path;
		this.defaultValue = defaultValue;
		this.comments = comments;
		this.clazz = clazz;
	}

	public String getSubPath() {
		return path;
	}

	public Class<? extends AbstractGame> getAbilityClass() {
		return clazz;
	}

	public String getPath() {
		final GameManifest manifest = getAbilityClass().getAnnotation(GameManifest.class);
		return manifest.name() + "." + this.path;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public String[] getComments() {
		return comments;
	}
}
