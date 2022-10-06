package cn.fasserver.more_perm.joinServer;

import cn.fasserver.more_perm.MorePerm;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;

public class JoinServerListener {
    private final MorePerm plugin;

    public static void init(MorePerm morePerm){
        new JoinServerListener(morePerm);
    }
    JoinServerListener(MorePerm morePerm){
        plugin = morePerm;
        morePerm.getServer().getEventManager().register(plugin, JoinServerListener.class);
        morePerm.getServer().getCommandManager().unregister("server");
        morePerm.getServer().getCommandManager().register("server", new ServerCommand(plugin.getServer()));
    }

    @Subscribe
    void onServerPreConnectEvent(ServerPreConnectEvent event) {
        // If connection is not allowed, do nothing
        if (!event.getResult().isAllowed()) return;
        // else, check whether the player has permission to join server
        if (event.getResult().getServer().isPresent() && canPlayerJoinServer(event.getPlayer(), event.getResult().getServer().get())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Component.translatable("more-perms.perm_deny.server_join", NamedTextColor.DARK_RED));
        }
    }

    @Subscribe
    void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event){
        Optional<RegisteredServer> initialServer = event.getInitialServer();
        if(initialServer.isPresent() && !canPlayerJoinServer(event.getPlayer(), initialServer.get())){
            List<String> connOrder = plugin.getServer().getConfiguration().getAttemptConnectionOrder().stream().filter(
                    s -> canPlayerJoinServer(event.getPlayer(), s)
            ).toList();
            event.setInitialServer(null);
            for (String conn: connOrder){
                Optional<RegisteredServer> serverOptional = plugin.getServer().getServer(conn);
                if(serverOptional.isPresent()){
                    event.setInitialServer(serverOptional.get());
                    return;
                }
            }

        }
    }

    private boolean canPlayerJoinServer(Player player, RegisteredServer server){
        return canPlayerJoinServer(player, server.getServerInfo().getName());
    }

    private boolean canPlayerJoinServer(Player player, String server){
        return player.hasPermission("server.join." + server);
    }
}
