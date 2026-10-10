package com.github.happyuky7.swi.common.i18n;

import com.github.happyuky7.swi.common.yaml.YamlCodec;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @Test
    void fallsBackLayerByLayerAndReplacesPlaceholders() {
        Messages m = Messages.of("es_ES", Arrays.asList(
                YamlCodec.parse("prefix: \"[ES] \"\nmod:\n  group: \"%prefix%%player% -> %group%\"\n"),
                YamlCodec.parse("prefix: \"[EN] \"\nmod:\n  group: \"unused\"\n  reload: \"%prefix%&aReloaded\"\n")));

        assertEquals("[ES] Alex -> nether", m.get("mod.group", "player", "Alex", "group", "nether"));
        assertEquals("[ES] &aReloaded", m.get("mod.reload"));
        assertEquals("missing.key", m.get("missing.key"));
        assertEquals("§8[§aSWI§8] & x", Messages.colorize("&8[&aSWI&8] & x"));
    }

    @Test
    void listMessages() {
        Messages m = Messages.of("en_US", Arrays.asList(YamlCodec.parse("prefix: P\nlist:\n  - \"%prefix% a %n%\"\n  - b\n")));
        List<String> lines = m.lines("list", "n", 1);
        assertEquals(Arrays.asList("P a 1", "b"), lines);
    }

    /** Every language of the repository parses and en_US / es_ES have every mod message. */
    @Test
    void repositoryLanguageFiles() throws Exception {
        File[] files = new File("../../langs").listFiles((d, n) -> n.endsWith(".yml"));
        assertTrue(files != null && files.length > 0, "langs folder not found");
        for (File f : files) {
            assertNotEquals(null, YamlCodec.read(f).getString("prefix"), f.getName());
        }
        for (String lang : new String[]{"en_US", "es_ES"}) {
            Messages m = Messages.of(lang, Arrays.asList(YamlCodec.read(new File("../../langs/" + lang + ".yml"))));
            for (String key : new String[]{"mod.reload", "mod.reload-warnings", "mod.reload-warning-line", "mod.group",
                    "mod.group-untracked", "mod.player-offline", "backups.force-backup.start"}) {
                assertNotEquals(key, m.get(key), lang + " misses " + key);
            }
            assertEquals(3, m.lines("mod.info").size(), lang + " mod.info");
            assertEquals(4, m.lines("backups.force-backup.completed").size(), lang + " backups.force-backup.completed");
        }
    }
}
