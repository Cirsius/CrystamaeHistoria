package io.github.sefiraat.crystamaehistoria.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SaveManager {

    private final Object lock = new Object();
    private final Path dataFolder;
    private final Logger logger;
    private final ExecutorService executor;
    private SaveData pendingSave;
    private boolean running;
    private boolean closing;

    public SaveManager(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "CrystamaeHistoria-Save");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void save(String config, FileConfiguration playerStats) {
        queue(snapshot(config, playerStats, null));
    }

    public void shutdown(String config, FileConfiguration playerStats) {
        final CompletableFuture<Void> completion = new CompletableFuture<>();
        synchronized (lock) {
            closing = true;
            queue(snapshot(config, playerStats, completion));
        }
        try {
            completion.join();
        } catch (CompletionException exception) {
            final Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            logger.log(Level.SEVERE, "Could not complete the final Crystamae data save", cause);
        }
    }

    private void queue(SaveData save) {
        synchronized (lock) {
            if (closing && save.completion == null) {
                return;
            }
            pendingSave = save;
            if (!running) {
                running = true;
                executor.execute(this::writePendingSaves);
            }
        }
    }

    private void writePendingSaves() {
        while (true) {
            final SaveData save;
            synchronized (lock) {
                save = pendingSave;
                pendingSave = null;
                if (save == null) {
                    running = false;
                    if (closing) {
                        executor.shutdown();
                    }
                    return;
                }
            }

            try {
                write(save);
                if (save.completion != null) {
                    save.completion.complete(null);
                }
            } catch (Throwable throwable) {
                logger.log(Level.SEVERE, "Could not save Crystamae data", throwable);
                if (save.completion != null) {
                    save.completion.completeExceptionally(throwable);
                }
            }
        }
    }

    private SaveData snapshot(String config, FileConfiguration playerStats, CompletableFuture<Void> completion) {
        try {
            final Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : playerStats.getValues(true).entrySet()) {
                if (!(entry.getValue() instanceof ConfigurationSection)) {
                    values.put(entry.getKey(), copy(entry.getValue()));
                }
            }
            return new SaveData(config, values, null, completion);
        } catch (UnsupportedValueException exception) {
            return new SaveData(config, null, playerStats.saveToString(), completion);
        }
    }

    private Object copy(Object value) {
        if (value == null
            || value instanceof String
            || value instanceof Boolean
            || value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long
            || value instanceof Float
            || value instanceof Double
            || value instanceof BigInteger
            || value instanceof BigDecimal
            || value instanceof Character
            || value instanceof Enum<?>) {
            return value;
        }
        if (value instanceof List<?> list) {
            final List<Object> result = new ArrayList<>(list.size());
            for (Object element : list) {
                result.add(copy(element));
            }
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            final Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(copy(entry.getKey()), copy(entry.getValue()));
            }
            return result;
        }
        throw new UnsupportedValueException();
    }

    private void write(SaveData save) throws IOException {
        final String playerStats;
        if (save.serializedPlayerStats == null) {
            final YamlConfiguration yaml = new YamlConfiguration();
            save.playerStats.forEach(yaml::set);
            playerStats = yaml.saveToString();
        } else {
            playerStats = save.serializedPlayerStats;
        }

        IOException failure = null;
        try {
            writeFile(dataFolder.resolve("player_stats.yml"), playerStats);
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            writeFile(dataFolder.resolve("config.yml"), save.config);
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void writeFile(Path destination, String contents) throws IOException {
        Files.createDirectories(destination.getParent());
        final Path temporaryFile = Files.createTempFile(
            destination.getParent(),
            destination.getFileName().toString(),
            ".tmp"
        );
        try {
            final ByteBuffer buffer = StandardCharsets.UTF_8.encode(contents);
            try (FileChannel channel = FileChannel.open(
                temporaryFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(
                    temporaryFile,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private record SaveData(
        String config,
        Map<String, Object> playerStats,
        String serializedPlayerStats,
        CompletableFuture<Void> completion
    ) {
    }

    private static final class UnsupportedValueException extends RuntimeException {
    }
}
