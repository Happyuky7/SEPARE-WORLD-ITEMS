package com.github.happyuky7.swi.common.storage;

import java.util.Locale;

public enum StorageType {
    YAML,
    SQLITE,
    MYSQL,
    MARIADB,
    MONGODB,
    REDIS;

    public static StorageType fromConfig(String value) {
        if (value == null) {
            return YAML;
        }
        try {
            return StorageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return YAML;
        }
    }

    /**
     * Remote backends do IO asynchronously; local ones (files, SQLite) are fast enough for the server thread.
     */
    public boolean isRemote() {
        return this != YAML && this != SQLITE;
    }
}
