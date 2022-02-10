package cokes86.addon.util.disguise;

import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.collect.Pair;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class v1_13_R2 implements IDisguise {
    private static final Map<UUID, Pair<String, Property>> origin = new HashMap<>();

    @Override
    public void changeSkin(Player player, UUID uuid) {
        if (origin.containsKey(uuid)) {
            CraftPlayer cp = (CraftPlayer) player;
            cp.getProfile().getProperties().removeAll("textures");
            cp.getProfile().getProperties().put("textures", origin.get(uuid).getRight());
        }
    }

    @Override
    public void saveData() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (origin.containsKey(pl.getUniqueId())) continue;
            if (pl.getName().isEmpty()) continue;
            CraftPlayer cp = (CraftPlayer) pl;

            Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();
            if (iterator.hasNext()) {
                origin.put(pl.getUniqueId(), Pair.of(pl.getName(), iterator.next()));
            }
        }
    }

    @Override
    public void addData(UUID uuid) {
        if (origin.containsKey(uuid)) return;
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        CraftPlayer cp = (CraftPlayer) pl;

        Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();
        if (iterator.hasNext()) {
            origin.put(uuid, Pair.of(pl.getName(), iterator.next()));
        }
    }

    @Override
    public void clearData() {
        origin.clear();
    }

    @Override
    public void setPlayerNameTag(Player p, UUID uuid) {
        if (origin.containsKey(uuid)) {
            try {
                EntityPlayer enp = ((CraftPlayer)p).getHandle();

                Object obj = enp.getClass().getMethod("getProfile", new Class[0]).invoke(enp);
                Field nameField = obj.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                nameField.set(obj, origin.get(uuid).getLeft());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isChanged(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        boolean skin = false;
        boolean nameTag = false;

        Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();

        if (iterator.hasNext() && origin.containsKey(player.getUniqueId())) {
            Property property = iterator.next();
            skin = property != origin.get(player.getUniqueId()).getRight();

            EntityPlayer enp = cp.getHandle();
            nameTag = !origin.get(player.getUniqueId()).getLeft().equals(enp.getProfile().getName());
        }

        return skin || nameTag;
    }

    @Override
    public void reloadPlayer(Player p) {
        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(AbilityWar.getPlugin(), p));
        Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(AbilityWar.getPlugin(), p));
    }
}
