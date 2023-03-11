package com.cokes86.cokesaddon.effect.list;

import com.cokes86.cokesaddon.effect.AddonEffectRegistry;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@EffectManifest(displayName = "§3셔플", name = "셔플", method = ApplicationMethod.UNIQUE_IGNORE, description = {
        "자신의 양손과 갑옷을 제외한 모든 아이템이 뒤섞이며, 위치를 조정할 수 없습니다.",
        "상태이상이 사라지면, 상태이상이 적용되기 전으로 돌아갑니다."
})
public class Suffle extends AbstractGame.Effect implements Listener{
    public static final EffectRegistry.EffectRegistration<Suffle> registration = AddonEffectRegistry.getRegistration(Suffle.class);
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;
    private final ItemStack[] before;

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        registration.apply(participant, timeunit, duration);
    }

    public Suffle(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
        setPeriod(TimeUnit.TICKS, 1);

        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = participant.getPlayer().getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§3셔플");
        this.before = participant.getPlayer().getInventory().getContents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        shuffleInventory(participant.getPlayer());
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    }

    @Override
    protected void run(int arg0) {
        super.run(arg0);
        if (hologram.isValid()) {
            hologram.teleport(participant.getPlayer().getLocation().clone().add(0,2.2,0));
        }
    }

    @Override
    protected void onEnd() {
        HandlerList.unregisterAll(this);
        hologram.remove();
        participant.getPlayer().getInventory().setContents(before);
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        HandlerList.unregisterAll(this);
        hologram.remove();
        participant.getPlayer().getInventory().setContents(before);
        super.onSilentEnd();
    }

    private final Random random = new Random();
    private final ItemStack nullItemStack = new ItemStack(Material.AIR);

    private void shuffleInventory(Player player) {
        List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        items.remove(player.getInventory().getHelmet());
        items.remove(player.getInventory().getChestplate());
        items.remove(player.getInventory().getLeggings());
        items.remove(player.getInventory().getBoots());
        items.remove(player.getInventory().getItemInOffHand());
        items.removeIf(Objects::isNull);
        items = new ArrayList<>(items);
        int mainhand = player.getInventory().getHeldItemSlot();
        List<Integer> index = getRandomNumber(items.size(), mainhand);

        for (int a = 0; a < 36; a++) {
            if (a==mainhand) continue;
            if (index.contains(a)) {
                ItemStack pickes = random.pick(items);
                player.getInventory().setItem(a, pickes);
                items.remove(pickes);
            } else {
                player.getInventory().setItem(a, nullItemStack);
            }
        }
    }

    private List<Integer> getRandomNumber(int count, int ignore) {
        List<Integer> result = new ArrayList<>();
        while (result.size() < count) {
            int a = random.nextInt(36);
            if (!result.contains(a) && ignore != a) {
                result.add(a);
            }
        }
        return result;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        if (e.getInventory().equals(participant.getPlayer().getInventory())) {
            e.setCancelled(this.isRunning());
        }
    }
}
