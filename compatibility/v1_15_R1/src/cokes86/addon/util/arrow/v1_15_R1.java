package cokes86.addon.util.arrow;

import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;

public class v1_15_R1 implements ArrowImpl{
    @Override
    public void setCritical(Entity arrow, boolean critical) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        ((Arrow) arrow).setCritical(critical);
    }

    @Override
    public boolean isCritical(Entity arrow) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        return ((Arrow) arrow).isCritical();
    }

    @Override
    public int getPierceLevel(Entity arrow) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        return ((AbstractArrow) arrow).getPierceLevel();
    }

    @Override
    public void setPierceLevel(Entity arrow, int level) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        ((AbstractArrow) arrow).setPierceLevel(level);
    }

    @Override
    public void setShotFromCrossbow(Entity arrow, boolean shotFromCrossbow) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        throw new VersionNotSupportedException();
    }

    @Override
    public boolean isShotFromCrossbow(Entity arrow) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        throw new VersionNotSupportedException();
    }
}
