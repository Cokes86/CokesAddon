package cokes86.addon.utils;

import org.bukkit.entity.LivingEntity;

public class DamagePlusUtil {
	
	/**
	 * 대상에게 갑옷, 능력패시브를 무시하는 고정적인 데미지를 줍니다.
	 * @param damage 대상에게 줄 데미지
	 * @param target 대상
	 * @param cause 원인
	 */
	public static void penetratingDamage(double damage, LivingEntity target, LivingEntity cause) {
		double th = target.getHealth();
		if (th - damage < 0) {
			target.setHealth(1);
			target.damage(200, cause);
		} else {
			target.setHealth(th - damage);
			target.damage(0, cause);
		}
	}
	
	/**
	 * 대상에게 갑옷, 능력패시브를 무시하는 고정적인 데미지를 줍니다.
	 * @param damage 대상에게 줄 데미지
	 * @param target 대상
	 */
	public static void penetratingDamage(double damage, LivingEntity target) {
		penetratingDamage(damage,target,null);
	}
	
}
