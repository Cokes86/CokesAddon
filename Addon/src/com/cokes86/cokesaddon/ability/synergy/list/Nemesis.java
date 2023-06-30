package com.cokes86.cokesaddon.ability.synergy.list;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;

@AbilityManifest(name = "아라아라", rank = Rank.S, species = Species.GOD, explain = {
    "패시브 - : 자신이 받은 최종대미지의 10%을 저장합니다.",
    "  저장한 대미지는 최대 1분까지 유지됩니다.",
    "  상대방을 공격할 시 로 저장한 대미지를",
    "  고정 대미지로 추가로 부여합니다.",
    "철괴 우클릭 - : 저장한 대미지를 모두 소모합니다.",
    "  일점 범위 이내 플레이어에게 대미지를 주고 밀어냅니다.",
    "  범위와 대미지, 넉백거리는 저장된 대미지에 비례합니다."
})
@Beta
public class Nemesis extends CokesSynergy implements ActiveHandler {
    private double final_damage = 0.0;
    private final ActionbarChannel channel = newActionbarChannel();
    private final HashMap<Player, AdditionalHit> map = new HashMap<>();

    public Nemesis(Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material arg0, ClickType arg1) {
        return false;
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
            new DamageSupplier(e.getFinalDamage() * 0.1);
        }

        if (e.getDamager() != null && e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player && !e.isCancelled()) {
            if (e.getCause() == DamageCause.VOID) return;
            Player target = (Player) e.getEntity();
			if (map.containsKey(target)) {
				e.setCancelled(true);
			} else {
				AdditionalHit hit = new AdditionalHit(target, final_damage);
				hit.start();
				map.put(target, hit);
			}
        }
    }
    
    @Override
    protected void onUpdate(Update update) {
        switch(update) {
            case RESTRICTION_CLEAR: {
                channel.update(" : "+final_damage);
                break;
            }
            case ABILITY_DESTROY: case RESTRICTION_SET: {
                channel.update(null);
                break;
            }
        }
    }

    private class DamageSupplier extends AbilityTimer {
        private final double damage;
        private DamageSupplier(double damage) {
            super(60);
            this.damage = damage;
            this.start();
        }

        @Override
        protected void onEnd() {
            final_damage -= damage;
            channel.update(" : "+final_damage);
        }

        @Override
        protected void onSilentEnd() {
            final_damage -= damage;
            channel.update(" : "+final_damage);
        }

        @Override
        protected void onStart() {
            final_damage += damage;
            channel.update(" : "+final_damage);
        }
    }

    private class AdditionalHit extends AbilityTimer implements Listener {
		private final Player player;
		private final double damage;
		private AdditionalHit(Player player, double damage) {
			super(10);
			this.player = player;
			this.damage = damage;
			setPeriod(TimeUnit.TICKS, 1);
		}

		public void onStart() {
			if (!player.isDead() && Damages.canDamage(player, getPlayer(), DamageCause.VOID, damage)) {
				player.setNoDamageTicks(0);
				NMSUtil.damageVoid(player, (float) final_damage);
			} else {
				this.stop(true);
			}
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		public void run(int a) {
			if (player.getNoDamageTicks() == 0) {
				this.stop(false);
			}
		}

		public void onEnd() {
			HandlerList.unregisterAll(this);
			map.remove(player);
		}

		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			map.remove(player);
		}

		@EventHandler
		public void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(player) && isRunning()) {
				e.setCancelled(true);
			}
		}
	}
}
