package id.veliora.war.queue;
import id.veliora.war.storage.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ArmorMigrationTest {
    @Test void removesOnlyConflictingBlastProtectionAndIsIdempotent() {
        var yaml = new YamlConfiguration();
        String path = "modes.cpvp.items.helmet.enchantments";
        yaml.set(path, List.of("PROTECTION:4", "BLAST_PROTECTION:4", "UNBREAKING:3"));
        yaml.set("modes.all_mode.items.legs.enchantments", List.of("BLAST_PROTECTION:4"));
        ConfigManager.normalizeArmor(yaml);
        ConfigManager.normalizeArmor(yaml);
        assertEquals(List.of("PROTECTION:4", "UNBREAKING:3"), yaml.getStringList(path));
        assertEquals(List.of("BLAST_PROTECTION:4"), yaml.getStringList("modes.all_mode.items.legs.enchantments"));
    }
}
