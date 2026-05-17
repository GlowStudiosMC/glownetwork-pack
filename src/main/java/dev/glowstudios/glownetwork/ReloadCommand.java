package dev.glowstudios.glownetwork;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand implements SimpleCommand {

    private final GlowNetwork plugin;

    public ReloadCommand(GlowNetwork plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            source.sendMessage(Component.text("Usage: /glownetwork reload", NamedTextColor.YELLOW));
            return;
        }

        source.sendMessage(Component.text("Reloading pack...", NamedTextColor.GRAY));

        if (plugin.reload()) {
            source.sendMessage(Component.text("✔ Pack reloaded successfully!", NamedTextColor.GREEN));
        } else {
            source.sendMessage(Component.text("✘ Reload failed. Check the console.", NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("glownetwork.reload");
    }
}
