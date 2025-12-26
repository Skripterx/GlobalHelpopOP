package me.main.globalHelpOp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class ConfigManager {

    private final Path configPath;
    private final Logger logger;
    private final PluginContainer plugin;

    private CommentedConfigurationNode configNode;
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Inject
    public ConfigManager(ProxyServer server, Logger logger, PluginContainer plugin, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.plugin = plugin;

        this.configPath = dataDirectory;

        Path configFile = configPath.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
    }

    public void loadConfig() {
        try {
            Path configFile = configPath.resolve("config.yml");

            Files.createDirectories(configPath);

            if (Files.notExists(configFile)) {
                try (InputStream is = plugin.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (is != null) {
                        Files.copy(is, configFile);
                        logger.info("Copiado 'config.yml' de los recursos internos.");
                    } else {
                        logger.warn("No se encontró 'config.yml' en los recursos del JAR. Creando archivo vacío.");
                        Files.createFile(configFile);
                    }
                }
            }

            configNode = loader.load();
            logger.info("Configuración de GlobalHelpOp cargada exitosamente.");

        } catch (IOException e) {
            logger.error("Error al cargar o crear el archivo de configuración.", e);
            configNode = CommentedConfigurationNode.root();
        }
    }

    public String getString(Object... path) {
        String value = configNode.node(path).getString("");
        return value;
    }

    public String getPermission(Object... path) {
        return getString(path);
    }

    public CommentedConfigurationNode getRootNode() {
        return configNode;
    }
}