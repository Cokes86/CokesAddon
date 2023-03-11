package com.cokes86.cokesaddon.game.module.roulette;

import java.util.Collection;
import java.util.HashMap;

import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;
import com.cokes86.cokesaddon.game.module.roulette.list.*;

import daybreak.abilitywar.utils.base.collect.Pair;

public class RouletteRegister {
    private static final HashMap<String, Pair<Class<? extends RouletteEffect>, SettingObject<Integer>>> map = new HashMap<>();

    public static final SettingObject<Boolean> enable = Roulette.config.new SettingObject<>(null, "룰렛 온오프","enable", true,
            "# 룰렛 모듈 사용 여부");

	public static final SettingObject<Integer> period = Roulette.config.new SettingObject<Integer>(null, "룰렛 발동 주기", "period", 45,
            "# 단위 : (초)") {
        @Override
        public boolean condition(Integer value) {
            return value > 0;
        }
    };

	public static boolean isEnabled() {
		return enable.getValue();
	}

	public static int getRoulletPeriod() {
		return period.getValue();
	}

    static {
        //1.11.0
        registerRouletteEffect(Teleport.class);
        registerRouletteEffect(ShowRank.class);
        registerRouletteEffect(ShowSpecies.class);
        registerRouletteEffect(NoticeFirstName.class);
        registerRouletteEffect(NoticeLastName.class);
        registerRouletteEffect(Location.class);
        registerRouletteEffect(HealthRegain.class);
        registerRouletteEffect(HealthDecrease.class);
        registerRouletteEffect(CooldownReset.class);

        //1.11.1
        registerRouletteEffect(NoAbility.class);
        registerRouletteEffect(ChangeHealth.class);

        //1.11.2
        registerRouletteEffect(Acceleration.class);
    }

    public static void registerRouletteEffect(Class<? extends RouletteEffect> effect) {
        RouletteManifest manifest = effect.getAnnotation(RouletteManifest.class);
        if (manifest != null) {
            String name = manifest.name();
            if (!map.containsKey(name)) {
                SettingObject<Integer> priority = Roulette.config.new SettingObject<Integer>(effect, name, "priority."+effect.getSimpleName().toLowerCase(), manifest.defaultPriority()) {
                    @Override
                    public boolean condition(Integer integer) {
                        return integer >= 0;
                    }
                };
                map.put(name, Pair.of(effect, priority));
            }
        }
    }

    public static Collection<Pair<Class<? extends RouletteEffect>, SettingObject<Integer>>> getMapPairs() {
        return map.values();
    }

    public static String getEffectName(Class<? extends RouletteEffect> effect) {
        for (String string : map.keySet()) {
            if (map.get(string).getLeft().equals(effect)) {
                return string;
            }
        }
        return "";
    }

    public static int getPriority(Class<? extends RouletteEffect> effect) {
        for (String string : map.keySet()) {
            if (map.get(string).getLeft().equals(effect)) {
                return map.get(string).getRight().getValue();
            }
        }
        return -1;
    }
}