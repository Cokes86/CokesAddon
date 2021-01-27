package cokes86.addon.ability.list.disguise;

import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.collect.Pair;
import net.minecraft.server.v1_9_R1.EntityPlayer;
import net.minecraft.server.v1_9_R1.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class v1_9_R1 implements IDisguise {
    private static final Map<UUID, Pair<String, Property>> origin = new HashMap<>();

    @Override
    public void changeSkin(Player player, UUID uuid) {
        saveData();

        CraftPlayer cp = (CraftPlayer) player;
        cp.getProfile().getProperties().removeAll("textures");
        cp.getProfile().getProperties().put("textures", origin.get(uuid).getRight());
    }

    @Override
    public void saveData() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (origin.containsKey(pl.getUniqueId())) continue;
            CraftPlayer cp = (CraftPlayer) pl;
            origin.put(pl.getUniqueId(), Pair.of(pl.getName(), cp.getProfile().getProperties().get("textures").iterator().next()));
        }
    }

    @Override
    public void clearData() {
        origin.clear();
    }

    @Override
    public void setPlayerNameTag(Player p, UUID uuid) {
        try {
            EntityPlayer enp = ((CraftPlayer)p).getHandle();

            Object obj = enp.getClass().getMethod("getProfile", new Class[0]).invoke(enp);
            Field nameField = obj.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(obj, origin.get(uuid).getLeft());

            reloadPlayer(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reloadPlayer(Player p) {
        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(p));
        Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(p));
    }
}
