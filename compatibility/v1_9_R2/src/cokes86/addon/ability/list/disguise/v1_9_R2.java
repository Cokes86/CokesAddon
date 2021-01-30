package cokes86.addon.ability.list.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.utils.base.collect.Pair;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class v1_9_R2 implements IDisguise {
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

    public void addData(UUID uuid) throws IOException {
        URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replaceAll("-","") + "?unsigned=false");
        InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
        JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
        String nameProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("name").getAsString();
        String texture = textureProperty.get("value").getAsString();
        String signature = textureProperty.get("signature").getAsString();

        Property property = new Property("textures", texture, signature);

        origin.put(uuid, Pair.of(nameProperty, property));
    }

    @Override
    public boolean isChanged(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        boolean skin = false;
        boolean nametag;

        Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();

        if (iterator.hasNext() && origin.containsKey(player.getUniqueId())) {
            Property property = iterator.next();
            skin = property != origin.get(player.getUniqueId()).getRight();
        }

        EntityPlayer enp = cp.getHandle();
        nametag = !origin.get(player.getUniqueId()).getLeft().equals(enp.getProfile().getName());

        return skin || nametag;
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
