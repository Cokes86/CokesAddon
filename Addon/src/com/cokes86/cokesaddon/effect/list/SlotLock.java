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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        AbilityWar.getPlugin().getServer().getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        ItemStack old = player.getInventory().getItem(slot);
        player.getInventory().setItem(slot, null);
        player.getInventory().setItem(slot, barrier);
        if (old != null) {
            player.getInventory().addItem(old);
        }
    }

    @Override
    protected void onEnd() {
        player.getInventory().setItem(slot, null);
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void onSilentEnd() {
        player.getInventory().setItem(slot, null);
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void run(int count) {}

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getWhoClicked().equals(player)) return;
        if (e.getSlot() == slot) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (e.getItemDrop().getItemStack().getType() == Material.BARRIER) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (e.getBlock().getType().equals(Material.BARRIER)) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!e.getWhoClicked().equals(player)) return;
        if (e.getInventorySlots().contains(slot)) e.setCancelled(true);
    }
}
