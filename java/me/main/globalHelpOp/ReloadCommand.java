package me.main.globalHelpOp;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        String reloadPermission = configManager.getString("reload", "permission");

        if (!source.hasPermission(reloadPermission)) {
            source.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return;
        }

        configManager.loadConfig();

        String successMessage = configManager.getString("reload", "success");
        Component message = serializer.deserialize(successMessage.replace('&', '§'));
        source.sendMessage(message);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String reloadPermission = configManager.getString("reload", "permission");
        return invocation.source().hasPermission(reloadPermission);
    }
}

