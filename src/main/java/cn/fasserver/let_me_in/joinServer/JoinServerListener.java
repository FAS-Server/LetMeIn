package cn.fasserver.let_me_in.joinServer;

import cn.fasserver.let_me_in.LetMeIn;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JoinServerListener {
    private final LetMeIn plugin;

    JoinServerListener(LetMeIn morePerm){
        plugin = morePerm;
    }

    @Subscribe
    void onServerPreConnectEvent(ServerPreConnectEvent event) {
        // If connection is not allowed, do nothing
        if (!event.getResult().isAllowed()) return;
        // else, check whether the player has permission to join server
        if (event.getResult().getServer().isPresent() && JoinServerPerm.check(event.getPlayer(), event.getResult().getServer().get())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Component.translatable("let-me-in.perm_deny.server_join", NamedTextColor.DARK_RED));
        }
    }

    @Subscribe(order = PostOrder.LAST)
    void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event){
        Optional<RegisteredServer> initialServer = event.getInitialServer();
        if(initialServer.isPresent() && !JoinServerPerm.check(event.getPlayer(), initialServer.get())){
            plugin.getLogger().info("Initial server not permitted!");
            List<String> connOrder = plugin.getServer().getConfiguration().getAttemptConnectionOrder().stream().filter(
                    s -> JoinServerPerm.check(event.getPlayer(), s)
            ).toList();
            plugin.getLogger().info("Permitted servers are: " + Arrays.toString(connOrder.toArray()));
            event.setInitialServer(null);
            Optional<RegisteredServer> target = connOrder.stream().map(conn->plugin.getServer().getServer(conn))
                    .filter(Optional::isPresent).map(Optional::get)
                    .findFirst();
            if(target.isPresent()){
                event.setInitialServer(target.get());
                plugin.getLogger().info("Sending player to " + target.get());
            }else {
                event.setInitialServer(null);
            }
        }
    }
}
