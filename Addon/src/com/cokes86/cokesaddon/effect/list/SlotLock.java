package com.cokes86.cokesaddon.effect.list;

import com.cokes86.cokesaddon.effect.AddonEffectRegistry;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

@EffectManifest(displayName = "§c슬롯 봉인", name = "술롯 봉인", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.COMBAT_RESTRICTION
}, description = {
        "슬롯중 1개를 봉인합니다."
})
public class SlotLock extends AbstractGame.Effect implements Listener {

    public static final EffectRegistry.EffectRegistration<SlotLock> registration = AddonEffectRegistry.getRegistration(SlotLock.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        registration.apply(participant, timeUnit, duration);
    }

    private final Player player;
    private final int slot;
    private final ItemStack barrier;

    public SlotLock(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
        this.player = participant.getPlayer();
        this.slot = new Random().nextInt(9);
        this.barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName("§c§l봉인된 슬롯");
        barrier.setItemMeta(meta);
        setPeriod(TimeUnit.TICKS, 1);
    }

    @Override
    protected void onStart() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());

        ItemStack old = player.getInventory().getItem(slot);

        // 먼저 슬롯 비우기
        player.getInventory().setItem(slot, null);

        if (old != null && old.getType() != Material.AIR) {
            Map<Integer, ItemStack> remain = player.getInventory().addItem(old);

            remain.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        // 마지막에 방벽 설치
        player.getInventory().setItem(slot, barrier);

        super.onStart();
    }

    @Override
    protected void run(int count) {
        setCount(20);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getWhoClicked().equals(player)) return;
        if (e.getRawSlot() == slot) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (e.getItemDrop().equals(barrier)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (e.getBlock().getType().equals(Material.BARRIER)) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!e.getWhoClicked().equals(player)) return;
        if (e.getRawSlots().contains(slot)) e.setCancelled(true);
    }
}
