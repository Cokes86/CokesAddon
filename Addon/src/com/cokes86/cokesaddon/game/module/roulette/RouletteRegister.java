package com.cokes86.cokesaddon.game.module.roulette;

import java.util.Collection;
import java.util.HashMap;

import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.effect.list.*;

import daybreak.abilitywar.utils.base.collect.Pair;

public class RouletteRegister {
    private static final HashMap<String, Pair<Class<? extends RouletteEffect>, SettingObject<Integer>>> map = new HashMap<>();

    public static final SettingObject<Boolean> enable = Roulette.config.new SettingObject<>(null, "룰렛 온오프","enable", true,
            "룰렛 모듈 사용 여부");

	public static final SettingObject<Integer> period = Roulette.config.new SettingObject<Integer>(null, "룰렛 발동 주기", "period", 45,
            "# 단위 : (초)") {
        @Override
        public boolean condition(Integer value) {
            return value > 0;
        }
    };

    public static final SettingObject<Boolean> ignoreBlindRoulette = Roulette.config.new SettingObject<>(null, "블라인드 능력자 룰렛 대체","ignore-blind-roulette", false,
            "블라인드 능력자의 기본 룰렛 대신 해당 룰렛 사용 여부",
            "AbilityWar 3.3.5 이상의 버전에서만 해당됩니다.");

	public static boolean isEnabled() {
		return enable.getValue();
	}

	public static int getRoulletPeriod() {
		return period.getValue();
	}

    public static boolean isIgnoreBlindRoulette() {
        return ignoreBlindRoulette.getValue();
    }

    static {
        //1.11.0
        registerRouletteEffect(Teleport.class);
        registerRouletteEffect(ShowRank.class);
        registerRouletteEffect(ShowSpecies.class);
        registerRouletteEffect(ShowFirstName.class);
        registerRouletteEffect(ShowLastName.class);
        registerRouletteEffect(ShowLocation.class);
        registerRouletteEffect(HealthRegain.class);
        registerRouletteEffect(HealthDecrease.class);
        registerRouletteEffect(CooldownReset.class);

        //1.11.1
        registerRouletteEffect(SealAndPower.class);
        registerRouletteEffect(ChangeHealth.class);

        //1.11.2
        registerRouletteEffect(Acceleration.class);

        //1.11.4
        registerRouletteEffect(StunParticipant.class);

        //1.12.0
        registerRouletteEffect(Detection.class);
        registerRouletteEffect(DiceGod.class);
        registerRouletteEffect(Suprise.class);

        //2.1.0
        registerRouletteEffect(Incarceration.class);
        registerRouletteEffect(RandomEffect.class);
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

    public static String getEffectDisplay(Class<? extends RouletteEffect> effect) {
        for (String string : map.keySet()) {
            if (map.get(string).getLeft().equals(effect)) {
                String display = map.get(string).getLeft().getAnnotation(RouletteManifest.class).display();
                return display.equals("") ? string : display;
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