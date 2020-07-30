package cokes86.addon.configuration.gamemode;

import cokes86.addon.gamemodes.battleability.BattleAbility;
import cokes86.addon.gamemodes.tailcatching.TailCatching;
import cokes86.addon.gamemodes.targethunting.TargetHunting;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManifest;

public enum GameNodes {
	BattleAbility_startSize(BattleAbility.class, "초기사이즈(블럭)",200),
	BattleAbility_time(BattleAbility.class, "게임시간(초)",600),
	TailCatching_range(TailCatching.class, "안내거리", 10, "#몇블럭 이내에 있을 시 엑션바에 안내할 지 설정"),
	TargetHunting_inv(TargetHunting.class, "무적시간", 60),
	TargetHunting_gameTime(TargetHunting.class, "게임시간", 300);
	
	private final String Path;
	private final Object Default;
	private final String[] Comments;
	private final Class<? extends AbstractGame> Clazz;
	
	GameNodes(Class<? extends AbstractGame> Clazz, String SubPath, Object Default, String... Comments) {
		this.Path = SubPath;
		this.Default = Default;
		this.Comments = Comments;
		this.Clazz = Clazz;
	}
	
	public String getSubPath() {
		return Path;
	}
	
	public Class<? extends AbstractGame> getAbilityClass() {
		return Clazz;
	}
	
	public String getPath() {
		GameManifest am = getAbilityClass().getAnnotation(GameManifest.class);
		String AbilityName = am.name();
		
		return AbilityName + "." + getSubPath();
	}
	
	public Object getDefault() {
		return Default;
	}

	public String[] getComments() {
		return Comments;
	}
}
