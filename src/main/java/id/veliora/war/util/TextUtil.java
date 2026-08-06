package id.veliora.war.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public final class TextUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {}

    public static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
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
}
