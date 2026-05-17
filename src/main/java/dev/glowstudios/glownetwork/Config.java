package dev.glowstudios.glownetwork;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class Config {

    private final String packFilename;
    private final int httpPort;
    private final String publicHost;
    private final boolean useHttps;
    private final boolean forced;
    private final String prompt;

    private Config(Properties props) {
        this.packFilename = props.getProperty("pack-filename", "pack.zip");
        this.httpPort = Integer.parseInt(props.getProperty("http-port", "25566"));
        this.publicHost = props.getProperty("public-host", "127.0.0.1");
        this.useHttps = Boolean.parseBoolean(props.getProperty("use-https", "false"));
        this.forced = Boolean.parseBoolean(props.getProperty("force-pack", "true"));
        this.prompt = props.getProperty("prompt", "Please accept the server resource pack");
    }

    public static Config load(Path dataDirectory, Logger logger) throws IOException {
        Path configFile = dataDirectory.resolve("config.properties");
        if (!Files.exists(configFile)) {
            try (InputStream in = Config.class.getResourceAsStream("/config.properties")) {
                if (in != null) {
                    Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("config.properties created. Edit it and restart the proxy.");
                }
            }
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        }

        return new Config(props);
    }

    public String getPackFilename() {
        return packFilename;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getPublicHost() {
        return publicHost;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public boolean isForced() {
        return forced;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getPackUrl() {
        String protocol = useHttps ? "https" : "http";
        boolean omitPort = (useHttps && httpPort == 443) || (!useHttps && httpPort == 80);

        if (omitPort) {
            return protocol + "://" + publicHost + "/" + packFilename;
        }
        return protocol + "://" + publicHost + ":" + httpPort + "/" + packFilename;
    }
}
