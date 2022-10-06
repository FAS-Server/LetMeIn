package cn.fasserver.more_perm;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Plugin(
        id = "more-perm",
        name = "More Permission",
        version = "@version@",
        description = "Manage your server with more permission node",
        url = "https://github.com/FAS-Server/MorePerm",
        authors = {"YehowahLiu"}
)
public class MorePerm {
    private final Logger logger;
    private final ProxyServer server;

    @Inject
    public MorePerm(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        logger.info("More permission node enabled!");
    }

    @Subscribe
    void onProxyInitializeEvent(ProxyInitializeEvent event) {
        registerTranslations();
    }

    private void registerTranslations() {
        logger.info("Loading localizations...");
        final TranslationRegistry translationRegistry = TranslationRegistry
                .create(Key.key("more-perms", "translations"));
        translationRegistry.defaultLocale(Locale.US);

        Path i18nPath = getL10nPath();

        try (Stream<Path> pathStream = Files.walk(i18nPath)) {
            pathStream.forEach(file -> {
                if (!Files.isRegularFile(file)) {
                    return;
                }
                String localeName = file.getFileName().toString()
                        .replace(".properties", "")
                        .replace("lang_", "")
                        .replace("lang", "")
                        .replace('_', '-');
                Locale locale;
                if (localeName.isEmpty()) {
                    locale = Locale.US;
                } else {
                    locale = Locale.forLanguageTag(localeName);
                }
                translationRegistry.registerAll(locale,
                        ResourceBundle.getBundle("l10n/lang",
                                locale, UTF8ResourceBundleControl.get()), false);
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        GlobalTranslator.get().addSource(translationRegistry);
        logger.info("Localizations loaded!");
    }


    private static Path getL10nPath() {
        Path l10nPath;
        URL knownResource = MorePerm.class.getClassLoader().getResource("l10n/lang.properties");

        if (knownResource == null) {
            throw new IllegalStateException("lang.properties does not exist, don't know where we are");
        }
        if (knownResource.getProtocol().equals("jar")) {
            // Running from a JAR
            // 如果在 jar 中路径类似 my.jar!/Main.class
            String jarPathRaw = knownResource.toString().split("!")[0];
            URI path = URI.create(jarPathRaw + "!/");

            try(FileSystem fileSystem = FileSystems.newFileSystem(path, Map.of("create", "true"))){
                l10nPath = fileSystem.getPath("l10n");
                if (!Files.exists(l10nPath)) {
                    throw new IllegalStateException("l10n does not exist, don't know where we are");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        } else {
            // Running from the file system
            URI uri;
            try {
                URL url = MorePerm.class.getClassLoader().getResource("l10n");
                if (url == null) {
                    throw new IllegalStateException("l10n does not exist, don't know where we are");
                }
                uri = url.toURI();
            } catch (URISyntaxException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            l10nPath = Paths.get(uri);
        }
        return l10nPath;
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
            List<String> connOrder = server.getConfiguration().getAttemptConnectionOrder().stream().filter(
                    s -> canPlayerJoinServer(event.getPlayer(), s)
            ).toList();
            event.setInitialServer(null);
            for (String conn: connOrder){
                Optional<RegisteredServer> serverOptional = server.getServer(conn);
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
