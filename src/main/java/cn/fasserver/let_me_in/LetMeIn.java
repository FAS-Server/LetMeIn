package cn.fasserver.let_me_in;

import cn.fasserver.let_me_in.joinServer.JoinServerPerm;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

@Plugin(
        id = "@id@",
        name = "@name@",
        version = "@version@",
        description = "Check permission from LuckPerm before join an server",
        url = "https://github.com/FAS-Server/LetMeIn",
        authors = {"YehowahLiu"}
)
public class LetMeIn {
    private final Logger logger;
    private final ProxyServer server;

    @Inject
    public LetMeIn(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        logger.info("More permission node enabled!");
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    @Subscribe
    void onProxyInitializeEvent(ProxyInitializeEvent event) {
        registerTranslations();
        JoinServerPerm.init(this);
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
        URL knownResource = LetMeIn.class.getClassLoader().getResource("l10n/lang.properties");

        if (knownResource == null) {
            throw new IllegalStateException("lang.properties does not exist, don't know where we are");
        }
        if (knownResource.getProtocol().equals("jar")) {
            // Running from a JAR
            // 如果在 jar 中路径类似 my.jar!/Main.class
            String jarPathRaw = knownResource.toString().split("!")[0];
            URI path = URI.create(jarPathRaw + "!/");

            try (FileSystem fileSystem = FileSystems.newFileSystem(path, Map.of("create", "true"))){
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
