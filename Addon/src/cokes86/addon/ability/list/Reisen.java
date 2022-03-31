package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.effect.list.Warp;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "레이센", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS, explain = {
        "§7패시브 §8- §9마인드 쉐이커§f: 상대방을 3회 타격할 때 마다 §9마인드 쉐이커§f의 수치가 1씩 증가합니다.",
        "  §9마인드 쉐이커§f의 수치가 $[MIND_SHAKER_ENHANCE_PREDICATE] 이상일 경우, ",
        "  자신의 모든 공격에 §8뒤틀림§f 효과를 $[MIND_SHAKER_ENHANCE_DURATION] 부여합니다.",
        "§7패시브 §8- §c광기를 다루는 자§f: 매 $[MADNESS_PERIOD]마다 $[MADNESS_RANGE]블럭 이내 플레이어의 시야를 무작위로 변경합니다.",
        "  이 효과로 시야가 바뀐 플레이어 수 마다 §9마인드 쉐이커§f가 증가합니다.",
        "  §9마인드 쉐이커§f의 수치가 $[MADNESS_ENHANCE_PREDICATE] 이상일 경우 주기가 $[MADNESS_ENHANCE_PERIOD_DECREASE]% 감소합니다.",
        "§7철괴 우클릭 §8- §c광기의 눈동자§f: $[EYES_RANGE]칸 이내 플레이어를 바라보고 사용할 수 있습니다.",
        "  대상을 $[EYES_DURATION]간 액티브, 타겟팅 스킬을 사용할 수 없게 합니다.",
        "  그리고 자신의 §9마인드 쉐이커§f 수치를 2만큼 상승합니다.",
        "  §9마인드 쉐이커§f의 수치가 $[EYES_ENHANCE_PREDICATE] 이상일 경우 쿨타임이 $[EYES_ENHANCE_COOLDOWN_DECREASE]% 감소합니다.",
        "§7상태이상 §8- §8뒤틀림§f: 멀미 효과를 부여합니다. 멀미 효과가 있는 동안",
        "  상대방을 공격할 시 30%의 확률로 해당 공격이 무효화됩니다."
}, summarize = {
    "일정시간마다 주변 플레이어의 시야가 뒤틀어집니다.",
    "시야가 뒤튼 플레이어의 수만큼, 또는 3회 공격 시, 스킬 사용 시 스택이 증가합니다.",
    "스택에 따라 스킬이 강화됩니다.",
    "상대방을 보고 우클릭 시 상대방의 액티브, 타겟팅 능력을 봉인합니다."
})
public class Reisen extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> MADNESS_ENHANCE_PREDICATE = new Config<>(Reisen.class, "광기를_다루는_자.강화_조건", 25, PredicateUnit.positive());
    private static final Config<Integer> MADNESS_ENHANCE_PERIOD_DECREASE = new Config<>(Reisen.class, "광기를_다루는_자.강화_주기_감소치(%)", 50, PredicateUnit.between(0, 100, false));
    private static final Config<Integer> MADNESS_PERIOD = new Config<>(Reisen.class, "광기를_다루는_자.주기", 20, Config.Condition.TIME);
    private static final Config<Integer> MADNESS_RANGE = new Config<>(Reisen.class, "광기를_다루는_자.범위", 5, PredicateUnit.positive());

    private static final Config<Integer> MIND_SHAKER_ENHANCE_PREDICATE = new Config<>(Reisen.class, "마인드_쉐이커.강화_조건", 50, PredicateUnit.positive());
    private static final Config<Integer> MIND_SHAKER_ENHANCE_DURATION = new Config<>(Reisen.class, "마인드_쉐이커.강화_뒤틀림_지속시간", 7, Config.Condition.TIME);

    private static final Config<Integer> EYES_RANGE = new Config<>(Reisen.class, "광기의_눈동자.범위", 10, PredicateUnit.positive());
    private static final Config<Integer> EYES_DURATION = new Config<>(Reisen.class, "광기의_눈동자.지속시간", 10, Config.Condition.TIME);
    private static final Config<Integer> EYES_COOLDOWN = new Config<>(Reisen.class, "광기의_눈동자.쿨타임", 10, Config.Condition.COOLDOWN);
    private static final Config<Integer> EYES_ENHANCE_PREDICATE = new Config<>(Reisen.class, "광기의_눈동자.강화_조건", 35, PredicateUnit.positive());
    private static final Config<Integer> EYES_ENHANCE_COOLDOWN_DECREASE = new Config<>(Reisen.class, "광기의_눈동자.강화_쿨타임_감소치(%)", 50, PredicateUnit.between(0, 100, false));

    public Reisen(AbstractGame.Participant arg0) {
        super(arg0);
    }

    private final BossBar bar = Bukkit.createBossBar("§8마인드 쉐이커", BarColor.RED, BarStyle.SEGMENTED_10);

    private int mind_shaker = 0;
    private boolean madness_enhance = false;
    private boolean mind_shaker_enhance = false;
    private boolean eyes_enhance = false;

    private Player eyes = null;
    private final Cooldown eyes_cooldown = new Cooldown(EYES_COOLDOWN.getValue());
    private final Duration eyes_duration = new Duration(EYES_DURATION.getValue(), eyes_cooldown) {
        @Override
        protected void onDurationProcess(int i) { }

        @Override
        protected void onDurationEnd() {
            eyes = null;
        }

        @Override
        protected void onDurationSilentEnd() {
            eyes = null;
        }
    };

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                return !game.getDeathManager().isExcluded(entity.getUniqueId());
            }
            return getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue();
        }
        return true;
    };

    private final AbilityTimer passive_timer = new ReisenMadnessTimer();
    private final MindShakerNoticeTimer notice = new MindShakerNoticeTimer();

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            bar.addPlayer(getPlayer());
            bar.setVisible(true);
            int max = Math.max(MADNESS_ENHANCE_PREDICATE.getValue(), Math.max(EYES_ENHANCE_PREDICATE.getValue(), MIND_SHAKER_ENHANCE_PREDICATE.getValue()));
            bar.setProgress(Math.min(1.0, (double)mind_shaker/max));

            passive_timer.start();
            notice.start();
        } else {
            bar.removePlayer(getPlayer());
            bar.setVisible(false);
            passive_timer.stop(true);
        }
    }

    public void addMindShaker(int a) {
        mind_shaker+=a;
        if (mind_shaker >= MADNESS_ENHANCE_PREDICATE.getValue() && !madness_enhance) {
            passive_timer.setPeriod(TimeUnit.SECONDS, passive_timer.getPeriod()*(100- MADNESS_ENHANCE_PERIOD_DECREASE.getValue())/100);
            madness_enhance = true;
            notice.add();
            SoundLib.ENTITY_ENDER_DRAGON_AMBIENT.playSound(getPlayer());
        }
        if (mind_shaker >= EYES_ENHANCE_PREDICATE.getValue() && !eyes_enhance) {
            eyes_enhance = true;
            eyes_cooldown.setCooldown(EYES_COOLDOWN.getValue() * (100-EYES_ENHANCE_COOLDOWN_DECREASE.getValue())/100);
            eyes_cooldown.setCount(eyes_cooldown.getCount() * (100-EYES_ENHANCE_COOLDOWN_DECREASE.getValue())/100);
            notice.add();
            SoundLib.ENTITY_ENDER_DRAGON_AMBIENT.playSound(getPlayer());
        }
        if (mind_shaker >= MIND_SHAKER_ENHANCE_PREDICATE.getValue() && !mind_shaker_enhance) {
            mind_shaker_enhance=true;
            notice.add();
            SoundLib.ENTITY_ENDER_DRAGON_AMBIENT.playSound(getPlayer());
        }

        int max = Math.max(MADNESS_ENHANCE_PREDICATE.getValue(), Math.max(EYES_ENHANCE_PREDICATE.getValue(), MIND_SHAKER_ENHANCE_PREDICATE.getValue()));
        bar.setProgress(Math.min(1.0, (double)mind_shaker/max));
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !eyes_cooldown.isCooldown() && !eyes_duration.isDuration()) {
            Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), EYES_RANGE.getValue(), predicate);
            if (player != null) {
                this.eyes = player;
                player.sendMessage("[§c레이센§f] 당신의 액티브, 타겟팅 능력이 봉인당했습니다.");
                new AbilityTimer(eyes_duration.getMaximumCount()) {
                    final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = getGame().getParticipant(player).actionbar().newChannel();
                    @Override
                    protected void run(int count) {
                        channel.update("§c광기의 눈동자§f: "+count+"초");
                    }

                    @Override
                    protected void onEnd() {
                        channel.unregister();
                    }

                    @Override
                    protected void onSilentEnd() {
                        channel.unregister();
                    }
                }.start();
                addMindShaker(2);
                return eyes_duration.start();
            }
        }
        return false;
    }

    int hit = 0;

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (NMS.isArrow(damager)) {
            Projectile arrow = (Projectile) e.getDamager();
            if (arrow.getShooter() instanceof Entity) {
                damager = (Entity) arrow.getShooter();
            }
        }

        if (damager.equals(getPlayer()) && predicate.test(e.getEntity()) && e.getEntity() instanceof Player) {
            if(mind_shaker_enhance) Warp.apply(getGame().getParticipant(e.getEntity().getUniqueId()), TimeUnit.SECONDS, MIND_SHAKER_ENHANCE_DURATION.getValue());
            hit++;
            if (hit == 3) {
                hit = 0;
                addMindShaker(1);
            }
        }
    }

    @SubscribeEvent
    public void onAbilityPreActive(AbilityPreActiveSkillEvent e) {
        if (e.getPlayer().equals(eyes)) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onAbilityPreTarget(AbilityPreTargetEvent e) {
        if (e.getPlayer().equals(eyes)) {
            e.setCancelled(true);
        }
    }

    class ReisenMadnessTimer extends AbilityTimer {
        public ReisenMadnessTimer() {
            super();
            setPeriod(TimeUnit.SECONDS, MADNESS_PERIOD.getValue());
        }

        @Override
        protected void run(int count) {
            List<Player> near_list = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation().clone(), MADNESS_RANGE.getValue(),MADNESS_RANGE.getValue(),predicate);
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (Player near : near_list) {
                    Player target = getFarthestEntity(near.getLocation(), predicate);
                    if (target != null) {
                        Vector direction = target.getEyeLocation().clone().toVector().subtract(near.getEyeLocation().toVector());
                        NMS.rotateHead(player, near, LocationUtil.getYaw(direction), LocationUtil.getPitch(direction));
                    }
                }
            }
            addMindShaker(near_list.size());
        }

        private Player getFarthestEntity(Location center, Predicate<Entity> predicate) {
            double distance = Double.MIN_VALUE;
            Player current = null;

            Location centerLocation = center.clone();
            if (center.getWorld() == null)
                return null;
            for (Entity e : center.getWorld().getEntities()) {
                if (e instanceof Player) {
                    Player entity = (Player) e;
                    double compare = centerLocation.distanceSquared(entity.getLocation());
                    if (compare > distance && (predicate == null || predicate.test(entity))) {
                        distance = compare;
                        current = entity;
                    }
                }
            }

            return current;
        }
    }

    private class MindShakerNoticeTimer extends AbilityTimer {
        private int num = 0;
        private boolean up = true;
        private double y;

        public MindShakerNoticeTimer() {
            super(TaskType.INFINITE, -1);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        public void onStart() {
            y = 0.0;
        }

        @Override
        protected void run(int count) {
            int a = 9 * count;
            double x = FastMath.cos(Math.toRadians(a));
            double z = FastMath.sin(Math.toRadians(a));

            if (y >= 0.5 && up) up = false;
            if (y <= -0.5 && !up) up = true;

            if (up) y+=0.05;
            else y-=0.05;

            if (num >= 1) {
                Location particle1 = getPlayer().getLocation().clone().add(x,1+y,z);
                ParticleLib.REDSTONE.spawnParticle(particle1, RGB.RED);
            }
            if (num >= 2) {
                Location particle2 = getPlayer().getLocation().clone().add(x,1,z);
                ParticleLib.REDSTONE.spawnParticle(particle2, RGB.RED);
            }
            if (num >= 3) {
                Location particle3 = getPlayer().getLocation().clone().add(x,1-y,z);
                ParticleLib.REDSTONE.spawnParticle(particle3, RGB.RED);
            }
        }

        public void add() {
            num++;
        }
    }
}
