package dev.glowstudios.glownetwork;

import com.google.inject.Inject;
import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

@Plugin(
    id = "glownetwork",
    name = "GlowNetwork",
    version = "1.0.0",
    description = "Hosts and serves a resource pack from the Velocity proxy",
    authors = {"GlowStudios"}
)
public class GlowNetwork {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Config config;
    private File packFile;
    private byte[] packHash;
    private HttpServer httpServer;

    @Inject
    public GlowNetwork(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            config = Config.load(dataDirectory, logger);

            packFile = dataDirectory.resolve(config.getPackFilename()).toFile();
            if (!packFile.exists()) {
                logger.error("#######################################################");
                logger.error(" Pack not found: " + packFile.getAbsolutePath());
                logger.error(" Drop your pack.zip into plugins/glownetwork/ and restart.");
                logger.error("#######################################################");
                return;
            }

            packHash = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(packFile.toPath()));
            logger.info("Pack loaded: " + packFile.getName() + " (" + (packFile.length() / 1024) + " KB)");
            logger.info("SHA-1: " + bytesToHex(packHash));

            startHttpServer();

            CommandManager commandManager = server.getCommandManager();
            CommandMeta meta = commandManager.metaBuilder("glownetwork").plugin(this).build();
            commandManager.register(meta, new ReloadCommand(this));

        } catch (Exception e) {
            logger.error("Failed to initialize GlowNetwork", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (httpServer != null) {
            httpServer.stop(0);
            logger.info("HTTP server stopped");
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (packHash == null) {
            logger.warn("Pack unavailable, skipping send for " + event.getPlayer().getUsername());
            return;
        }

        Player player = event.getPlayer();
        String url = config.getPackUrl();

        ResourcePackInfo.Builder builder = server.createResourcePackBuilder(url)
            .setHash(packHash)
            .setShouldForce(config.isForced());

        if (config.getPrompt() != null && !config.getPrompt().isEmpty()) {
            builder.setPrompt(Component.text(config.getPrompt(), NamedTextColor.GOLD));
        }

        player.sendResourcePackOffer(builder.build());
        logger.info("Pack sent to " + player.getUsername());
    }

    private void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);

        httpServer.createContext("/" + config.getPackFilename(), exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
                exchange.sendResponseHeaders(200, packFile.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(packFile.toPath(), os);
                }
            } catch (IOException e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("Broken pipe")
                        || msg.contains("Connection reset")
                        || msg.contains("insufficient bytes"))) {
                    logger.debug("Download aborted by client: {}", msg);
                } else {
                    logger.error("Failed to send pack", e);
                }
            } catch (Exception e) {
                logger.error("Failed to send pack", e);
            } finally {
                exchange.close();
            }
        });

        httpServer.createContext("/health", exchange -> {
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            exchange.close();
        });

        httpServer.setExecutor(null);
        httpServer.start();

        logger.info("HTTP server started on port " + config.getHttpPort());
        logger.info("Pack URL: " + config.getPackUrl());
    }

    public boolean reload() {
        try {
            config = Config.load(dataDirectory, logger);

            packFile = dataDirectory.resolve(config.getPackFilename()).toFile();
            if (!packFile.exists()) {
                logger.error("Pack not found on reload: " + packFile.getAbsolutePath());
                return false;
            }

            packHash = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(packFile.toPath()));
            logger.info("Pack reloaded. New SHA-1: " + bytesToHex(packHash));

            return true;
        } catch (Exception e) {
            logger.error("Reload failed", e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
