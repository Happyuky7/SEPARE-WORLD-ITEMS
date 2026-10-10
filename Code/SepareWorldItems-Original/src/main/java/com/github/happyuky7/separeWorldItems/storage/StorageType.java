package com.github.happyuky7.separeWorldItems.storage;

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
}
