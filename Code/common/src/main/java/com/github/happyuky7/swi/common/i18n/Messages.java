package com.github.happyuky7.swi.common.i18n;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Language files shared with the plugin ({@code langs/<lang>.yml} of the GitHub repository).
 *
 * <p>Lookup order for every key: the server's {@code langs/<lang>.yml}, the same language bundled in the jar,
 * then the bundled {@code en_US}. So a translation that misses new keys still works.
 * Colors use {@code &} codes; {@code %prefix%} and {@code %name%} placeholders are replaced.</p>
 */
public final class Messages {

    public static final String DEFAULT_LANG = "en_US";
    private static final String DOWNLOAD_URL = "https://raw.githubusercontent.com/Happyuky7/SEPARE-WORLD-ITEMS/master/langs/";
    private static final Pattern LANG_ID = Pattern.compile("[A-Za-z]{2,3}_[A-Za-z]{2,3}");

    private final List<DataNode> layers;
    private final String lang;

    private Messages(String lang, List<DataNode> layers) {
        this.lang = lang;
        this.layers = layers;
    }

    /**
     * @param langsDir     folder with the server's language files (created when needed)
     * @param lang         language id, e.g. es_ES
     * @param download     try to download the newest file from GitHub first
     * @param bundledRoot  resource folder with the bundled files, e.g. {@code separeworlditems/langs/}
     */
    public static Messages load(File langsDir, String lang, boolean download, ClassLoader loader, String bundledRoot, SwiLogger logger) {
        if (lang == null || !LANG_ID.matcher(lang).matches()) {
            logger.warn("[Langs] Invalid settings.langs.lang '" + lang + "', using " + DEFAULT_LANG);
            lang = DEFAULT_LANG;
        }
        File file = new File(langsDir, lang + ".yml");
        if (download) {
            try {
                downloadTo(DOWNLOAD_URL + lang + ".yml", file);
            } catch (IOException e) {
                logger.debug("[Langs] Could not download " + lang + ".yml (" + e.getMessage() + "), using the local/bundled file");
            }
        }
        List<DataNode> layers = new ArrayList<>();
        if (file.isFile()) {
            try {
                layers.add(YamlCodec.read(file));
            } catch (IOException | RuntimeException e) {
                logger.warn("[Langs] Cannot read " + file + ": " + e.getMessage());
            }
        }
        addBundled(layers, loader, bundledRoot + lang + ".yml", logger);
        if (!DEFAULT_LANG.equals(lang)) {
            addBundled(layers, loader, bundledRoot + DEFAULT_LANG + ".yml", logger);
        }
        if (layers.isEmpty()) {
            logger.warn("[Langs] No language file found for " + lang + ": messages will show their keys");
        }
        return new Messages(lang, Collections.unmodifiableList(layers));
    }

    /** Built from in-memory files (tests). */
    public static Messages of(String lang, List<DataNode> layers) {
        return new Messages(lang, layers);
    }

    private static void addBundled(List<DataNode> layers, ClassLoader loader, String resource, SwiLogger logger) {
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in != null) layers.add(YamlCodec.read(in));
        } catch (IOException | RuntimeException e) {
            logger.warn("[Langs] Cannot read bundled " + resource + ": " + e.getMessage());
        }
    }

    private static void downloadTo(String url, File target) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        c.setRequestProperty("User-Agent", "SepareWorldItems");
        if (c.getResponseCode() != 200) {
            throw new IOException("HTTP " + c.getResponseCode());
        }
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        File tmp = new File(parent, target.getName() + ".tmp");
        try (InputStream in = c.getInputStream()) {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public String lang() {
        return lang;
    }

    /**
     * @param placeholders pairs: {@code "player", name, "group", group}
     * @return the message with {@code &} color codes (or the key itself when missing everywhere)
     */
    public String get(String key, Object... placeholders) {
        String raw = raw(key);
        if (raw == null) {
            return key;
        }
        String prefix = raw(("prefix"));
        String out = raw.replace("%prefix%", prefix != null ? prefix : "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            out = out.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
        }
        return out;
    }

    /** Lines of a list message (or a single-line list for a plain string). */
    public List<String> lines(String key, Object... placeholders) {
        for (DataNode layer : layers) {
            Object v = layer.get(key);
            if (v instanceof List) {
                List<String> out = new ArrayList<>();
                for (Object line : (List<?>) v) {
                    String text = String.valueOf(line);
                    String prefix = raw("prefix");
                    text = text.replace("%prefix%", prefix != null ? prefix : "");
                    for (int i = 0; i + 1 < placeholders.length; i += 2) {
                        text = text.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
                    }
                    out.add(text);
                }
                return out;
            }
        }
        return Collections.singletonList(get(key, placeholders));
    }

    private String raw(String key) {
        for (DataNode layer : layers) {
            Object v = layer.get(key);
            if (v != null && !(v instanceof DataNode) && !(v instanceof List)) {
                return String.valueOf(v);
            }
        }
        return null;
    }

    /** {@code &a} -> {@code §a} (Minecraft legacy formatting codes). */
    public static String colorize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(text.charAt(i + 1)) >= 0) {
                sb.append('§');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
