package com.github.happyuky7.separeWorldItems.commands;

import org.bukkit.command.CommandSender;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight confirmation registry for critical commands.
 *
 * <p>Stores pending actions in memory with a short TTL.</p>
 */
public final class CommandConfirmations {

    public enum ActionType {
        BACKUP_FORCE,
        MIGRATE
    }

    public record PendingAction(ActionType type, String[] args, long expiresAtMillis, String token) {
        public boolean isExpired(long now) {
            return now > expiresAtMillis;
        }
    }

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Map<String, PendingAction> PENDING = new ConcurrentHashMap<>();

    private CommandConfirmations() {
    }

    public static PendingAction create(CommandSender sender, ActionType type, String[] originalArgs, long ttlMillis) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(type, "type");

        long now = System.currentTimeMillis();
        String token = generateToken(6);

        // store a copy, normalized to avoid later mutation
        String[] argsCopy = originalArgs != null ? Arrays.copyOf(originalArgs, originalArgs.length) : new String[0];
        PendingAction action = new PendingAction(type, argsCopy, now + ttlMillis, token);
        PENDING.put(key(sender), action);
        return action;
    }

    public static PendingAction get(CommandSender sender) {
        PendingAction action = PENDING.get(key(sender));
        if (action == null) {
            return null;
        }
        if (action.isExpired(System.currentTimeMillis())) {
            PENDING.remove(key(sender));
            return null;
        }
        return action;
    }

    public static PendingAction consumeIfTokenMatches(CommandSender sender, String token) {
        PendingAction action = get(sender);
        if (action == null) {
            return null;
        }
        if (token == null || token.isBlank()) {
            return null;
        }
        if (!action.token().equalsIgnoreCase(token.trim())) {
            return null;
        }
        PENDING.remove(key(sender));
        return action;
    }

    public static void cancel(CommandSender sender) {
        PENDING.remove(key(sender));
    }

    private static String key(CommandSender sender) {
        // Works for console + players; unique enough for confirmation flow.
        return sender.getName().toLowerCase(Locale.ROOT);
    }

    private static String generateToken(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(TOKEN_CHARS[RNG.nextInt(TOKEN_CHARS.length)]);
        }
        return sb.toString();
    }
}
