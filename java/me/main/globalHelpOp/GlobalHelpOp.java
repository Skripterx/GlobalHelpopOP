package me.main.globalHelpOp;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import org.slf4j.Logger;

@Plugin(
        id = "globalhelpop",
        name = "GlobalHelpOp",
        version = "1.0",
        authors = {"NovaPuto"}
)
public class GlobalHelpOp {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;

    @Inject
    public GlobalHelpOp(ProxyServer server, Logger logger, ConfigManager configManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GlobalHelpOp se está inicializando.");

        configManager.loadConfig();

        try {
            server.getChannelRegistrar().register(new ChannelIdentifier() {
                @Override
                public String getId() {
                    return "BungeeCord";
                }
            });
        } catch (IllegalArgumentException e) {
            logger.warn("El canal BungeeCord ya estaba registrado o es un identificador desconocido, ignorado. Error: " + e.getMessage());
        }

        CommandManager commandManager = server.getCommandManager();

        HelpOpCommand helpOpCommand = new HelpOpCommand(server, configManager);
        ReloadCommand reloadCommand = new ReloadCommand(configManager);

        commandManager.register("helpop", helpOpCommand);
        commandManager.register("h", helpOpCommand);
        commandManager.register("helpopteleport", helpOpCommand);
        commandManager.register("rhelpop", reloadCommand);

        logger.info("GlobalHelpOp se ha habilitado.");
    }
}
