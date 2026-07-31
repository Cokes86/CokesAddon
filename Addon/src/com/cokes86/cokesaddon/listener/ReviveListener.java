package com.cokes86.cokesaddon.listener;

import com.cokes86.cokesaddon.ability.decorate.Revivable;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.event.AbilityDestroyEvent;
import daybreak.abilitywar.game.event.participant.ParticipantAbilitySetEvent;
import daybreak.abilitywar.game.module.EventManager;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableList;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

public class ReviveListener implements Listener {
    private final Map<AbilityBase, ReviveObserver> map = new HashMap<>();

    @EventHandler
    public void onParticipantAbilitySet(ParticipantAbilitySetEvent event) {
        if (event.getOldAbility() instanceof Revivable) {
            AbilityBase ability = event.getOldAbility();
            ReviveObserver observer = map.get(ability);
            if (observer != null) {
                ability.getGame().getEventManager().unregister(observer.damage());
                ability.getGame().getEventManager().unregister(observer.health());
                map.remove(ability);
            }
        }

        if (event.getNewAbility() instanceof Revivable) {
            AbilityBase ability = event.getNewAbility();
            ReviveObserver observer = new ReviveObserver(ability);
            map.put(ability, observer);
            ability.getGame().getEventManager().register(observer.damage());
            ability.getGame().getEventManager().register(observer.health());
        }
    }

    @EventHandler
    public void onAbilityDestroy(AbilityDestroyEvent event) {
        if (event.getAbility() instanceof Revivable) {
            AbilityBase ability = event.getAbility();
            ReviveObserver observer = map.get(ability);
            if (observer != null) {
                ability.getGame().getEventManager().unregister(observer.damage());
                ability.getGame().getEventManager().unregister(observer.health());
                map.remove(ability);
            }
        }
    }

    private class ReviveObserver {
        private final AbilityBase abilityBase;
        private final List<Class<? extends Event>> children = ImmutableList.of(EntityDamageByBlockEvent.class, EntityDamageByEntityEvent.class);
        private final EventManager.EventObserver damage = new EventManager.EventObserver(EntityDamageEvent.class, EventPriority.HIGHEST, 1000, children) {
            @Override
            protected void onEvent(Event event) {
                event(event);
            }
        };
        private final EventManager.EventObserver health = new EventManager.EventObserver(PlayerSetHealthEvent.class, EventPriority.HIGHEST, 1000) {
            @Override
            protected void onEvent(Event event) {
                event(event);
            }
        };

        public ReviveObserver(AbilityBase abilityBase) {
            this.abilityBase = abilityBase;
        }

        private EventManager.EventObserver damage() {
            return damage;
        }

        private EventManager.EventObserver health() {
            return health;
        }

        protected void event(Event event) {
            if (!(abilityBase instanceof Revivable)) return;
            if (abilityBase.isRestricted() || abilityBase.isDestroyed()) return;
            Revivable revivable = (Revivable) abilityBase;
            if (!revivable.canRevive()) return;
            if (event instanceof EntityDamageEvent) {
                EntityDamageEvent e = (EntityDamageEvent) event;
                if (e.getEntity().equals(abilityBase.getPlayer())) {
                    if (abilityBase.getPlayer().getHealth() - e.getFinalDamage() <= 0) {
                        e.setDamage(0);
                        SoundLib.ITEM_TOTEM_USE.playSound(abilityBase.getPlayer());
                        revivable.afterRevive();
                    }
                }
            }

            else if (event instanceof PlayerSetHealthEvent) {
                PlayerSetHealthEvent e = (PlayerSetHealthEvent) event;
                if (e.getPlayer().equals(abilityBase.getPlayer())) {
                    if (e.getHealth() <= 0) {
                        e.setCancelled(true);
                        SoundLib.ITEM_TOTEM_USE.playSound(abilityBase.getPlayer());
                        revivable.afterRevive();
                    }
                }
            }
        }
    }
}
