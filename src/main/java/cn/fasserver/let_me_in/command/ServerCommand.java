package cn.fasserver.let_me_in.command;

import cn.fasserver.let_me_in.service.JoinServerPerm;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.event.HoverEvent.showText;

// Override '/server' command
public class ServerCommand implements SimpleCommand {

    public static final int MAX_SERVERS_TO_LIST = 50;
    private final ProxyServer server;

    public ServerCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        final CommandSource source = invocation.source();
        final String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(Identity.nil(), Component.translatable("velocity.command.players-only", NamedTextColor.RED));
            return;
        }

        if (args.length == 1) {
            // Trying to connect to a server.
            String serverName = args[0];
            Optional<RegisteredServer> toConnect = server.getServer(serverName);
            if (toConnect.isEmpty()) {
                player.sendMessage(Identity.nil(), Component.translatable("velocity.command.server-does-not-exist", NamedTextColor.RED).args(Component.text(serverName)));
                return;
            }

            if (!JoinServerPerm.check(source, serverName)) {
                player.sendMessage(Component.translatable("let-me-in.perm_deny.server_join", NamedTextColor.DARK_RED));
                return;
            }

            player.createConnectionRequest(toConnect.get()).fireAndForget();
        } else {
            outputServerInformation(player);
        }
    }

    private void outputServerInformation(Player executor) {
        String currentServer = executor.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse("<unknown>");
        executor.sendMessage(Identity.nil(), Component.translatable("velocity.command.server-current-server", NamedTextColor.YELLOW, Component.text(currentServer)));

        List<RegisteredServer> servers = server.getAllServers().stream().sorted(Comparator.comparing(RegisteredServer::getServerInfo)).toList();
        if (servers.size() > MAX_SERVERS_TO_LIST) {
            executor.sendMessage(Identity.nil(), Component.translatable("velocity.command.server-too-many", NamedTextColor.RED));
            return;
        }

        // Assemble the list of servers as components
        TextComponent.Builder serverListBuilder = Component.text().append(Component.translatable("velocity.command.server-available", NamedTextColor.YELLOW)).append(Component.space());
        for (int i = 0; i < servers.size(); i++) {
            RegisteredServer rs = servers.get(i);
            serverListBuilder.append(formatServerComponent(executor, currentServer, rs));
            if (i != servers.size() - 1) {
                serverListBuilder.append(Component.text(", ", NamedTextColor.GRAY));
            }
        }

        executor.sendMessage(Identity.nil(), serverListBuilder.build());
    }

    private TextComponent formatServerComponent(Player player, String currentPlayerServer, RegisteredServer server) {
        ServerInfo serverInfo = server.getServerInfo();
        TextComponent serverTextComponent = Component.text(serverInfo.getName());

        int connectedPlayers = server.getPlayersConnected().size();
        TranslatableComponent playersTextComponent;
        if (connectedPlayers == 1) {
            playersTextComponent = Component.translatable("velocity.command.server-tooltip-player-online");
        } else {
            playersTextComponent = Component.translatable("velocity.command.server-tooltip-players-online");
        }
        playersTextComponent = playersTextComponent.args(Component.text(connectedPlayers));
        if (serverInfo.getName().equals(currentPlayerServer)) {
            serverTextComponent = serverTextComponent.color(NamedTextColor.GREEN).hoverEvent(showText(Component.translatable("velocity.command.server-tooltip-current-server").append(Component.newline()).append(playersTextComponent)));
        } else if (JoinServerPerm.check(player, server)) {
            serverTextComponent = serverTextComponent.color(NamedTextColor.GRAY).clickEvent(ClickEvent.runCommand("/server " + serverInfo.getName())).hoverEvent(showText(Component.translatable("velocity.command.server-tooltip-offer-connect-server").append(Component.newline()).append(playersTextComponent)));
        } else {
            serverTextComponent = serverTextComponent.color(NamedTextColor.DARK_RED).hoverEvent(showText(Component.translatable("more-perms.perm_deny.server_join").append(Component.newline()).append(playersTextComponent)));
        }
        return serverTextComponent;
    }

    @Override
    public List<String> suggest(final SimpleCommand.Invocation invocation) {
        final String[] currentArgs = invocation.arguments();
        Stream<String> possibilities = server.getAllServers().stream().filter(
                rs -> JoinServerPerm.check(invocation.source(), rs)
        ).map(rs -> rs.getServerInfo().getName());

        if (currentArgs.length == 0) {
            return possibilities.collect(Collectors.toList());
        } else if (currentArgs.length == 1) {
            return possibilities.filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length())).collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().getPermissionValue("velocity.command.server") != Tristate.FALSE;
    }
}