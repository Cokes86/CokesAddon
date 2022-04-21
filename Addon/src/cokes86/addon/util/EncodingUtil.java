package cokes86.addon.util;

public class EncodingUtil {

    public static String xor_encoding(String value, int key) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            builder.append((char)(c^key));
        }
        return builder.toString();
    }
}
