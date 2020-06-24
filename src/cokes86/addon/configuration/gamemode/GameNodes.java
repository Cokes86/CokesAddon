package cokes86.addon.configuration.gamemode;

import cokes86.addon.gamemodes.battleability.BattleAbility;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManifest;

public enum GameNodes {
	BattleAbility_startSize(BattleAbility.class, "초기사이즈(블럭)",100),
	BattleAbility_time(BattleAbility.class, "게임시간(초)",600);
	
	private String Path;
	private Object Default;
	private String[] Comments;
	private Class<? extends AbstractGame> Clazz;
	
	private GameNodes(Class<? extends AbstractGame> Clazz, String SubPath, Object Default, String... Comments) {
		
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
