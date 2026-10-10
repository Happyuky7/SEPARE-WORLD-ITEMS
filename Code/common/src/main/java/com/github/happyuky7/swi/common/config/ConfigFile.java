package com.github.happyuky7.swi.common.config;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.yaml.YamlCodec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * A YAML file on disk that is created from a bundled default resource on first start.
 */
public final class ConfigFile {

    private final File file;
    private final String defaultResource;
    private final ClassLoader loader;

    public ConfigFile(File file, String defaultResource, ClassLoader loader) {
        this.file = file;
        this.defaultResource = defaultResource;
        this.loader = loader;
    }

    public File file() {
        return file;
    }

    public DataNode load() throws IOException {
        if (!file.isFile()) {
            copyDefault();
        }
        return YamlCodec.read(file);
    }

    /**
     * Rewrites the file. Note: SnakeYAML does not keep comments, so only use it for small generated changes.
     */
    public void save(DataNode node) throws IOException {
        YamlCodec.write(file, node);
    }

    private void copyDefault() throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        try (InputStream in = loader.getResourceAsStream(defaultResource)) {
            if (in == null) {
                throw new IOException("Missing bundled resource " + defaultResource);
            }
            Files.copy(in, file.toPath());
        }
    }
}
