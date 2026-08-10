package com.example.minitask_pacto_mais.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotEnvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);

    private DotEnvLoader() {}

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile == null) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile);
            int loaded = 0;
            for (String raw : lines) {
                if (applyLine(raw)) {
                    loaded++;
                }
            }
            log.info("Arquivo .env carregado ({} variáveis): {}", loaded, envFile.toAbsolutePath());
        } catch (IOException ex) {
            log.warn("Não foi possível ler {}: {}", envFile, ex.getMessage());
        }
    }

    private static Path resolveEnvFile() {
        Path cwd = Path.of(".env");
        if (Files.isRegularFile(cwd)) {
            return cwd;
        }
        Path module = Path.of("minitask-pacto-mais", ".env");
        if (Files.isRegularFile(module)) {
            return module;
        }
        return null;
    }

    private static boolean applyLine(String raw) {
        if (raw == null) {
            return false;
        }
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return false;
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return false;
        }
        String key = line.substring(0, eq).trim();
        if (key.isEmpty() || System.getenv(key) != null || System.getProperty(key) != null) {
            return false;
        }
        String value = stripQuotes(line.substring(eq + 1).trim());
        System.setProperty(key, value);
        return true;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
