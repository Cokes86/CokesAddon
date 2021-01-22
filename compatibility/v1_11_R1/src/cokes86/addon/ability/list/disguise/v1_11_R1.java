package cokes86.addon.ability.list.disguise;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class v1_11_R1 implements IDisguise {
    @Override
    public void changeSkin(Player player, String targetName) {
        try {
            GameProfile profile = ((CraftPlayer)player).getHandle().getProfile();

            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", getSkin(targetName));

            reloadPlayer(player);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Property getSkin(String name) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(getUUID(name));
        JSONObject json = (JSONObject)obj;
        Object obj1 = parser.parse(getProfile((String)json.get("id")));
        JSONObject json1 = (JSONObject)obj1;
        JSONObject raw = (JSONObject)json1.get("raw");
        JSONArray arr = (JSONArray)raw.get("properties");
        JSONObject skin = (JSONObject)arr.get(0);
        return new Property("textures", (String)skin.get("value"), (String)skin.get("signature"));
    }

    public String getUUID(String playername) throws IOException {
        URL url = new URL("https://api.minetools.eu/uuid/" + playername);
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        StringBuilder response = new StringBuilder();
        BufferedReader in = new BufferedReader(
                new InputStreamReader((InputStream)connection.getContent(), StandardCharsets.UTF_8));
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

    public String getProfile(String uuid) throws IOException {
        URL url = new URL("https://api.minetools.eu/profile/" + uuid);
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        StringBuilder response = new StringBuilder();
        BufferedReader in = new BufferedReader(
                new InputStreamReader((InputStream)connection.getContent(), StandardCharsets.UTF_8));
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }


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

    public void setPlayerNameTag(Player p, String name) {
        try {
            EntityPlayer enp = ((CraftPlayer)p).getHandle();

            Object obj = enp.getClass().getMethod("getProfile", new Class[0]).invoke(enp);
            Field nameField = obj.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(obj, name);

            reloadPlayer(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
