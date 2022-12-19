package cn.fasserver.let_me_in.service;

import cn.fasserver.let_me_in.LetMeIn;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;

import java.util.List;

public class LuckpermsListener {
    private final LetMeIn plugin;

    public LuckpermsListener(LetMeIn plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        EventBus eventBus = luckPerms.getEventBus();

        eventBus.subscribe(this.plugin, UserDataRecalculateEvent.class, e -> this.checkAndReassignServer(e.getUser()));

        eventBus.subscribe(this.plugin, GroupDataRecalculateEvent.class, e -> {
            luckPerms.getUserManager().getLoadedUsers().stream()
                    .filter(user -> user.getPrimaryGroup().equals(e.getGroup().getName()))
                    .forEach(this::checkAndReassignServer);
        });
    }

    private void checkAndReassignServer(User user) {
        plugin.getServer()
                .getPlayer(user.getUniqueId())
                .ifPresent(player -> {
                    player.getCurrentServer().ifPresent(currentServer -> {
                        if (!JoinServerPerm.check(player, currentServer.getServerInfo().getName())) {
                            List<RegisteredServer> availableServers = plugin.getServer().getAllServers()
                                    .stream().filter(server -> JoinServerPerm.check(player, server))
                                    .toList();
                            RegisteredServer target = plugin.checkFirstAvailableServer(availableServers);

                            if (target != null) {
                                plugin.getLogger().info("Sending player to " + target.getServerInfo().getName());
                                player.createConnectionRequest(target).fireAndForget();
                            } else {
                                plugin.getLogger().info("No server for player to join!");
                                player.disconnect(Component.translatable("let-me-in.no_available_server"));
                            }
                        }
                    });
                });
    }
}
