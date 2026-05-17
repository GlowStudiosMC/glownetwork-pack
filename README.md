# GlowNetwork — Velocity Plugin

A Velocity plugin that **hosts a resource pack locally** and **automatically sends it to players on join**. Ideal for SMP networks running multiple backend servers where the pack should only be downloaded once.

## Features

- Built-in HTTP server that serves the pack straight from the plugin folder — no external web server needed
- Automatic pack delivery on player login (`PostLoginEvent`)
- Automatic SHA-1 hash computation → players never end up with a stale cached version
- External configuration (no recompile needed to change host or port)
- `/glownetwork reload` command to hot-reload the pack without restarting the proxy
- `/health` HTTP endpoint to confirm the server is up

## Requirements

- Java **21+**
- A Velocity proxy (3.5.x recommended)

## Build

```bash
./gradlew shadowJar
```

The output jar will be at `build/libs/GlowNetwork-1.0.0.jar`.

## Installation

1. Drop `GlowNetwork-1.0.0.jar` into the `plugins/` folder of your Velocity proxy
2. Start the proxy once → the `plugins/glownetwork/` folder is created with a default `config.properties`
3. Stop the proxy
4. Drop your `pack.zip` into `plugins/glownetwork/`
5. Edit `plugins/glownetwork/config.properties`:
   - `public-host` = your server's public IP or domain
   - `http-port` = the port you allocated for HTTP
6. Start the proxy again

## Configuration

```properties
# Pack file name inside plugins/glownetwork/
pack-filename=pack.zip

# Port the built-in HTTP server listens on
# Must match an open allocation if you're on Pterodactyl
http-port=25566

# Public host used to build the URL sent to players
# Can be an IP (e.g. 51.83.42.10) or a domain (e.g. pack.example.com)
# DO NOT use 127.0.0.1 or localhost — clients won't be able to connect
public-host=your-public-ip

# Use HTTPS in the URL? (only if you have a reverse proxy with SSL in front)
use-https=false

# Force the player to accept the pack (kicks them if they decline)?
force-pack=true

# Message shown on the pack acceptance prompt
prompt=Please accept the server resource pack
```

## Backend servers (IMPORTANT)

In every `server.properties` of your Paper/Spigot backends:

```properties
resource-pack=
resource-pack-sha1=
require-resource-pack=false
```

Otherwise each backend will push its own pack and the client will reload it on every server switch.

## Updating the pack

1. Upload the new `pack.zip` into `plugins/glownetwork/`
2. Run `/glownetwork reload` in the proxy console
3. Players will download the new version on their next login

## Permissions

| Permission             | Description                                    | Default |
|------------------------|------------------------------------------------|---------|
| `glownetwork.reload`   | Allows running `/glownetwork reload`           | op      |

## Troubleshooting

**Players don't receive the pack**
- Make sure the HTTP port is open and reachable from the public internet
- Test the URL in a browser: `http://YOUR_IP:25566/pack.zip` should download the file
- Test the health endpoint: `http://YOUR_IP:25566/health` should return `OK`

**Players see an HTTP / insecure-content warning**
- Put a domain + Cloudflare in front for free HTTPS
- Set `use-https=true` and `public-host=your-domain.com` in the config

**The pack reloads every time a player switches server**
- Make sure `resource-pack=` is **empty** in every backend `server.properties`

**`Broken pipe` / `Connection reset` in the proxy log**
- Harmless — happens when a client cancels the download (declined, disconnected, or already had the pack cached). Logged at `DEBUG` level.

## License

MIT — see [LICENSE](LICENSE).

## Credits

Built by [GlowStudios](https://discord.gg/glowsmp).
Developed with the help of [Claude Code](https://claude.com/claude-code).
