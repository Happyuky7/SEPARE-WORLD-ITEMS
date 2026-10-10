package com.github.happyuky7.swi.common.yaml;

import com.github.happyuky7.swi.api.data.DataNode;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * YAML <-> {@link DataNode} using SnakeYAML (only constructors that exist in SnakeYAML 1.x and 2.x).
 */
public final class YamlCodec {

    private YamlCodec() {
    }

    private static Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setWidth(Integer.MAX_VALUE);
        // SnakeYAML instances are not thread-safe: create one per call.
        return new Yaml(options);
    }

    public static DataNode parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return DataNode.create();
        }
        Object root = yaml().load(text);
        return root instanceof Map ? DataNode.of((Map<?, ?>) root) : DataNode.create();
    }

    public static DataNode read(InputStream in) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Object root = yaml().load(reader);
            return root instanceof Map ? DataNode.of((Map<?, ?>) root) : DataNode.create();
        }
    }

    public static DataNode read(File file) throws IOException {
        if (!file.isFile()) {
            return DataNode.create();
        }
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return read(in);
        }
    }

    public static String dump(DataNode node) {
        return yaml().dump(node.toMap());
    }

    /**
     * Writes atomically (temp file + move) so a crash never leaves a half-written player file.
     */
    public static void write(File file, DataNode node) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create folder " + parent);
        }
        File tmp = new File(parent, file.getName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)) {
            writer.write(dump(node));
        }
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
