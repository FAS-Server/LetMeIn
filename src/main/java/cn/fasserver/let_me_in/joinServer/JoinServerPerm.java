package cn.fasserver.let_me_in.joinServer;

import cn.fasserver.let_me_in.LetMeIn;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class JoinServerPerm {
    public static void init(LetMeIn plugin){
        plugin.getServer().getEventManager().register(plugin, new JoinServerListener(plugin));
        plugin.getServer().getCommandManager().unregister("server");
        plugin.getServer().getCommandManager().register("server", new ServerCommand(plugin.getServer()));
    }

    public static boolean check(CommandSource source, RegisteredServer server){
        return check(source, server.getServerInfo().getName());
    }

    public static boolean check(CommandSource source, String server){
        return source.hasPermission("server.join." + server);
    }
}
