package cn.fasserver.let_me_in.service;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class JoinServerPerm {
    public static boolean check(CommandSource source, RegisteredServer server){
        return check(source, server.getServerInfo().getName());
    }

    public static boolean check(CommandSource source, String server){
        return source.hasPermission("server.join." + server);
    }
}
