package cokes86.addon.util;

import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

public class CokesUtil {

    /**
    발사한 물체가 화살종류인지 확인합니다.
     @param projectile 발사한 물체
     @return 발사한 물체의 화살 여부
     */
    public static boolean isInstanceOfArrow(Projectile projectile) {
        if (projectile instanceof Arrow) return true;
        else return ServerVersion.getVersion() >= 14 && projectile instanceof AbstractArrow;
    }

    /**
     발사한 물체가 화살종류인지 확인합니다.
     @param entity 엔티티
     @return 엔티티의 발사체 여부와 발사한 물체의 화살 여부
     */
    public static boolean isInstanceOfArrow(Entity entity) {
        return entity instanceof Projectile && isInstanceOfArrow((Projectile) entity);
    }
}
