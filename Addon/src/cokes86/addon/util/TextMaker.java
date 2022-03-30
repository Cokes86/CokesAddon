package cokes86.addon.util;

import daybreak.google.common.base.Strings;

public class TextMaker {

    public static String repeatWithTwoColor(String repeat, char color1, int count1, char color2, int count2) {
        return "§"+Character.toString(color1).concat(Strings.repeat(repeat, count1)).concat("§"+ color2).concat(Strings.repeat(repeat, count2));
    }
}
