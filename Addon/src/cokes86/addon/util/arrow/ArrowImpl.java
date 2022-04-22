package cokes86.addon.util.arrow;

import org.bukkit.entity.Entity;

public interface ArrowImpl {

    void setCritical(Entity arrow, boolean criticial);
    boolean isCritical(Entity arrow);
    int getPierceLevel(Entity arrow);
    void setPierceLevel(Entity arrow, int level);
    void setShotFromCrossbow(Entity arrow, boolean shotFromCrossbow);
    boolean isShotFromCrossbow(Entity arrow);
}
