package com.promoit.otp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads {@code application.properties} from the classpath and exposes typed
 * accessors. Any property can be overridden by an environment variable whose
 * name is the upper-cased key with dots replaced by underscores
 * (e.g. {@code db.url} -> {@code DB_URL}). Environment variables win.
 */
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final Properties properties = new Properties();

    public AppConfig() {
        this("application.properties");
    }

    public AppConfig(String resourceName) {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Configuration resource not found: " + resourceName);
            }
            properties.load(in);
            log.info("Loaded configuration from classpath resource '{}'", resourceName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load configuration: " + resourceName, e);
        }
    }

    /** Returns the raw string value, or {@code null} if absent. */
    public String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
