package id.veliora.war.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private TextUtil() {}

    public static Component component(String text) {
        return LEGACY.deserialize(expandHex(text == null ? "" : text));
    }

    public static Component component(String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return component(result);
    }

    public static String plainLegacy(Component component) {
        return LEGACY.serialize(component);
    }

    private static String expandHex(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char character : hex.toCharArray()) replacement.append('&').append(character);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
