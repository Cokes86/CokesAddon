package cokes86.addon.util.arrow;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.entity.Entity;

public class ArrowUtil {

    private static final ArrowImpl instance;
    private Entity arrow = null;

    static {
        try {
            instance = Class.forName("cokes86.addon.util.arrow." + ServerVersion.getName())
                    .asSubclass(ArrowImpl.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }

    public static ArrowUtil of(Entity entity) {
        final ArrowUtil util = new ArrowUtil();
        util.setArrow(entity);
        return util;
    }

    private void setArrow(Entity entity) {
        this.arrow = entity;
    }

    public void setCritical(boolean critical) {
        instance.setCritical(arrow, critical);
    }

    public boolean isCritical() {
        return instance.isCritical(arrow);
    }

    public int getPierceLevel() {
        return instance.getPierceLevel(arrow);
    }

    public void setPierceLevel(int level) {
        instance.setPierceLevel(arrow, level);
    }

    public void setShotFromCrossbow(boolean shotFromCrossbow) {
        instance.setShotFromCrossbow(arrow, shotFromCrossbow);
    }

    public boolean isShotFromCrossbow() {
        return instance.isShotFromCrossbow(arrow);
    }
}
