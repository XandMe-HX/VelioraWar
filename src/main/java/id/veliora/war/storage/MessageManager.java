package id.veliora.war.storage;

import id.veliora.war.util.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.Map;

public final class MessageManager {
    private final ConfigManager configs;

    public MessageManager(ConfigManager configs) {
        this.configs = configs;
    }

    public String raw(String key) {
        return configs.file("messages.yml").getString(key, "&cPesan tidak ditemukan: " + key);
    }

    public Component component(String key) {
        return TextUtil.component(raw(key));
    }

    public Component component(String key, Map<String, String> placeholders) {
        return TextUtil.component(raw(key), placeholders);
    }

    public void send(Audience audience, String key) {
        audience.sendMessage(TextUtil.component(raw("prefix")).append(component(key)));
    }

    public void send(Audience audience, String key, Map<String, String> placeholders) {
        audience.sendMessage(TextUtil.component(raw("prefix")).append(component(key, placeholders)));
    }
}
