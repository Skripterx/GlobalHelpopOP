package me.main.globalHelpOp;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HelpOpCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private static final ChannelIdentifier BUNGEE_CHANNEL = new ChannelIdentifier() {
        @Override
        public String getId() {
            return "BungeeCord";
        }
    };

    public HelpOpCommand(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    private String getSenderName(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return "Console";
    }

    private Component getPrefix() {
        String prefixText = configManager.getString("prefix");
        return serializer.deserialize(prefixText.replace('&', '§'));
    }

    private String getStaffPermission() {
        return configManager.getPermission("staff-permission");
    }

    private static final String H_STAFF_PERMISSION = "h.staff";
    private static final String H_BYPASS_PERMISSION = "h.bypass";


    private void sendHelpOpNotification(Player staff, String player, String serverName) {
        CommentedConfigurationNode formatNode = configManager.getRootNode().node("helpop-format");
        CommentedConfigurationNode titleNode = formatNode.node("title");
        CommentedConfigurationNode soundNode = formatNode.node("sound");

        String rawTitleText = titleNode.node("title-text").getString("");
        String rawSubtitleText = titleNode.node("subtitle-text").getString("");

        String formattedTitle = rawTitleText
                .replaceAll("%player%", player)
                .replaceAll("%server%", serverName)
                .replace('&', '§');

        String formattedSubtitle = rawSubtitleText
                .replaceAll("%player%", player)
                .replaceAll("%server%", serverName)
                .replace('&', '§');

        int fadeIn = titleNode.node("fadein").getInt(10);
        int stay = titleNode.node("stay").getInt(60);
        int fadeOut = titleNode.node("fadeout").getInt(10);

        Title title = Title.title(
                serializer.deserialize(formattedTitle),
                serializer.deserialize(formattedSubtitle),
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        );
        staff.showTitle(title);

        if (soundNode.node("enabled").getBoolean(false)) {
            String soundName = soundNode.node("name").getString("ENTITY_PLAYER_LEVELUP");
            double volume = soundNode.node("volume").getDouble(1.0);
            double pitch = soundNode.node("pitch").getDouble(1.0);

            String commandToExecute = "playsound " + soundName + " " + staff.getUsername() + " " + volume + " " + pitch;

            Optional<ServerConnection> serverConnOpt = staff.getCurrentServer();
            serverConnOpt.ifPresent(serverConn -> {
                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                java.io.DataOutputStream out = new java.io.DataOutputStream(stream);

                try {
                    out.writeUTF("Forward");
                    out.writeUTF("ALL");

                    byte[] commandBytes = commandToExecute.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                    out.writeShort(commandBytes.length);
                    out.write(commandBytes);

                } catch (java.io.IOException e) {
                }

                serverConn.sendPluginMessage(
                        BUNGEE_CHANNEL,
                        stream.toByteArray()
                );
            });
        }
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        String commandName = invocation.alias();

        if (commandName.equalsIgnoreCase("helpop")) {
            if (!(source instanceof Player)) {
                String msg = configManager.getString("messages", "only-players").replace('&', '§');
                source.sendMessage(getPrefix().append(serializer.deserialize(msg)));
                return;
            }

            Player player = (Player) source;

            if (args.length < 1) {
                String msg = configManager.getString("messages", "usage-player").replace('&', '§');
                source.sendMessage(getPrefix().append(serializer.deserialize(msg)));
                return;
            }

            String message = String.join(" ", args);

            Optional<RegisteredServer> currentServer = player.getCurrentServer().map(s -> s.getServer());
            String serverName = currentServer.isPresent() ? currentServer.get().getServerInfo().getName() : "Desconocido";

            CommentedConfigurationNode formatNode = configManager.getRootNode().node("helpop-format");
            boolean prefixEnabled = formatNode.node("prefix-enabled").getBoolean(true);

            java.util.List<? extends CommentedConfigurationNode> lines = formatNode.node("format-lines").childrenList();

            for (Player staff : server.getAllPlayers()) {
                if (staff.hasPermission(getStaffPermission())) {

                    sendHelpOpNotification(staff, player.getUsername(), serverName);

                    for (CommentedConfigurationNode lineNode : lines) {

                        if (lineNode.hasChild("button") || lineNode.hasChild("suggest")) {

                            CommentedConfigurationNode buttonContainerNode;
                            boolean isSuggestCommand;

                            if (lineNode.hasChild("button")) {
                                buttonContainerNode = lineNode.node("button");
                                isSuggestCommand = false;
                            } else {
                                buttonContainerNode = lineNode.node("suggest");
                                isSuggestCommand = true;
                            }

                            String buttonText = buttonContainerNode.node("text").getString("").replace('&', '§');
                            String hoverText = buttonContainerNode.node("hover-text").getString("")
                                    .replaceAll("%player%", player.getUsername())
                                    .replaceAll("%server%", serverName)
                                    .replace('&', '§');
                            String command = buttonContainerNode.node("command").getString("").replaceAll("%player%", player.getUsername());

                            TextComponent goComponent = serializer.deserialize(buttonText)
                                    .hoverEvent(HoverEvent.showText(serializer.deserialize(hoverText)));

                            if (isSuggestCommand) {
                                goComponent = goComponent.clickEvent(ClickEvent.suggestCommand(command));
                            } else {
                                goComponent = goComponent.clickEvent(ClickEvent.runCommand(command));
                            }

                            staff.sendMessage(goComponent);

                        } else {
                            String line = lineNode.getString("");

                            String formattedLine = line
                                    .replaceAll("%player%", player.getUsername())
                                    .replaceAll("%server%", serverName)
                                    .replaceAll("%message%", message)
                                    .replace('&', '§');

                            Component lineComponent = prefixEnabled
                                    ? getPrefix().append(serializer.deserialize(formattedLine))
                                    : serializer.deserialize(formattedLine);

                            staff.sendMessage(lineComponent);
                        }
                    }
                }
            }

            String successMsg = configManager.getString("messages", "success").replace('&', '§');
            source.sendMessage(getPrefix().append(serializer.deserialize(successMsg)));


        } else if (commandName.equalsIgnoreCase("h")) {
            if (!source.hasPermission(getStaffPermission())) {
                source.sendMessage(getPrefix().append(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED)));
                return;
            }

            if (args.length < 2) {
                String msg = configManager.getString("messages", "usage-staff-response").replace('&', '§');
                source.sendMessage(getPrefix().append(serializer.deserialize(msg)));
                return;
            }

            String targetName = args[0];
            Optional<Player> targetOpt = server.getPlayer(targetName);
            String response = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

            if (targetOpt.isPresent()) {
                Player target = targetOpt.get();

                CommentedConfigurationNode responseFormatNode = configManager.getRootNode().node("staff-response-format");
                boolean responsePrefixEnabled = responseFormatNode.node("prefix-enabled").getBoolean(true);
                java.util.List<? extends CommentedConfigurationNode> responseLines = responseFormatNode.node("format-lines").childrenList();

                for (CommentedConfigurationNode lineNode : responseLines) {
                    String line = lineNode.getString("");
                    String formattedLine = line
                            .replaceAll("%staff%", getSenderName(source))
                            .replaceAll("%message%", response)
                            .replace('&', '§');

                    Component lineComponent = responsePrefixEnabled
                            ? getPrefix().append(serializer.deserialize(formattedLine))
                            : serializer.deserialize(formattedLine);

                    target.sendMessage(lineComponent);
                }

                String sentMsg = configManager.getString("messages", "response-sent")
                        .replaceAll("%player%", targetName)
                        .replace('&', '§');
                source.sendMessage(getPrefix().append(serializer.deserialize(sentMsg)));
            } else {
                String notFoundMsg = configManager.getString("messages", "player-not-found")
                        .replaceAll("%player%", targetName)
                        .replace('&', '§');
                source.sendMessage(getPrefix().append(serializer.deserialize(notFoundMsg)));
            }

        } else if (commandName.equalsIgnoreCase("helpopteleport")) {
            if (!source.hasPermission(getStaffPermission())) return;
            if (!(source instanceof Player)) return;
            if (args.length != 1) return;

            Player staff = (Player) source;
            String targetName = args[0];
            Optional<Player> targetOpt = server.getPlayer(targetName);

            if (targetOpt.isPresent()) {
                Player target = targetOpt.get();
                Optional<RegisteredServer> targetServer = target.getCurrentServer().map(s -> s.getServer());

                if (targetServer.isPresent()) {
                    staff.createConnectionRequest(targetServer.get()).connectWithIndication().thenAccept(success -> {
                        String serverName = targetServer.get().getServerInfo().getName();
                        if (success) {
                            String successMsg = configManager.getString("messages", "teleport-success")
                                    .replaceAll("%server%", serverName)
                                    .replaceAll("%player%", target.getUsername())
                                    .replace('&', '§');
                            staff.sendMessage(getPrefix().append(serializer.deserialize(successMsg)));
                        } else {
                            String errorMsg = configManager.getString("messages", "teleport-error").replace('&', '§');
                            staff.sendMessage(getPrefix().append(serializer.deserialize(errorMsg)));
                        }
                    });
                } else {
                    String invalidServerMsg = configManager.getString("messages", "teleport-invalid-server").replace('&', '§');
                    staff.sendMessage(getPrefix().append(serializer.deserialize(invalidServerMsg)));
                }
            } else {
                String notFoundMsg = configManager.getString("messages", "player-not-found")
                        .replaceAll("%player%", targetName)
                        .replace('&', '§');
                staff.sendMessage(getPrefix().append(serializer.deserialize(notFoundMsg)));
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String commandName = invocation.alias();
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (commandName.equalsIgnoreCase("h") && args.length == 1 && source.hasPermission(getStaffPermission())) {

            String partialName = args[0].toLowerCase();

            boolean canBypass = source.hasPermission(H_BYPASS_PERMISSION);

            return server.getAllPlayers().stream()
                    .filter(player -> {
                        if (!player.getUsername().toLowerCase().startsWith(partialName)) {
                            return false;
                        }

                        if (canBypass) {
                            return true;
                        }

                        return !player.hasPermission(H_STAFF_PERMISSION);
                    })
                    .map(Player::getUsername)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }


    @Override
    public boolean hasPermission(Invocation invocation) {
        String alias = invocation.alias();
        if (alias.equalsIgnoreCase("helpop")) {
            return true;
        }

        return invocation.source().hasPermission(getStaffPermission());
    }
}