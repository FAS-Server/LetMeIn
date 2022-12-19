package cn.fasserver.let_me_in;

import cn.fasserver.let_me_in.command.ServerCommand;
import cn.fasserver.let_me_in.service.JoinServerPerm;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

@Plugin(
        id = "@id@",
        name = "@name@",
        version = "@version@",
        description = "Check permission before join an server",
        url = "https://github.com/FAS-Server/LetMeIn",
        authors = {"YehowahLiu"},
        dependencies = { @Dependency(id = "luckperms", optional = true) }
)
public class LetMeIn {
    private final Logger logger;
    private final ProxyServer server;

    @Inject
    public LetMeIn(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        logger.info("Plugin LetMeIn is enabled!");
    }

    public ProxyServer getServer() {
        return server;
    }

    // Listeners
    @Subscribe
    void onProxyInitializeEvent(ProxyInitializeEvent event) {
        registerTranslations();
        server.getCommandManager().unregister("server");
        server.getCommandManager().register("server", new ServerCommand(server));
    }

    @Subscribe
    void onServerPreConnectEvent(ServerPreConnectEvent event) {
        // If connection is not allowed, do nothing
        if (!event.getResult().isAllowed()) return;
        // else, check whether the player has permission to join server
        event.getResult().getServer().ifPresent(server1 -> {
            if(!JoinServerPerm.check(event.getPlayer(), server1)){
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                event.getPlayer().sendMessage(Component.translatable("let-me-in.perm_deny.server_join", NamedTextColor.DARK_RED));
            }
        });
    }

    @Subscribe(order = PostOrder.LAST)
    void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event){
        Optional<RegisteredServer> initialServer = event.getInitialServer();
        initialServer.ifPresent(server1 -> {
            List<String> connOrderString = new ArrayList<>(List.of(server1.getServerInfo().getName()));
            connOrderString.addAll(getServer().getConfiguration().getAttemptConnectionOrder());
            List<RegisteredServer> targets = connOrderString.stream().distinct().map(s -> getServer().getServer(s))
                    .filter(Optional::isPresent).map(Optional::get)
                    .filter(s -> JoinServerPerm.check(event.getPlayer(), s)).toList();
            RegisteredServer target = null;
            for (RegisteredServer s : targets) {
                try {
                    s.ping().join();
                } catch (CancellationException | CompletionException exception){
                    continue;
                }
                target = s;
                break;
            }

            if(target != null){
                logger.info("Sending player to " + target.getServerInfo().getName());
                event.setInitialServer(target);
            } else {
                event.setInitialServer(null);
                logger.info("No server for player to join!");
                event.getPlayer().disconnect(Component.translatable("let-me-in.no_available_server"));
            }
        });
    }

    // Translations
    private void registerTranslations() {
        logger.info("Loading localizations...");
        final TranslationRegistry translationRegistry = TranslationRegistry
                .create(Key.key("let-me-in", "translations"));
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
        URL knownResource = LetMeIn.class.getClassLoader().getResource("l10n/lang.properties");

        if (knownResource == null) {
            throw new IllegalStateException("lang.properties does not exist, don't know where we are");
        }
        if (knownResource.getProtocol().equals("jar")) {
            // Running from a JAR
            // 如果在 jar 中路径类似 my.jar!/Main.class
            String jarPathRaw = knownResource.toString().split("!")[0];
            URI path = URI.create(jarPathRaw + "!/");

            try {
                FileSystem fileSystem = FileSystems.newFileSystem(path, Map.of("create", "true"));
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
                URL url = LetMeIn.class.getClassLoader().getResource("l10n");
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
}
