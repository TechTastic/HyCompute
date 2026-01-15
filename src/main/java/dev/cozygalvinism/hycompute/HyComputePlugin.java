package dev.cozygalvinism.hycompute;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.cozygalvinism.hycompute.commands.ExampleCommand;
import dev.cozygalvinism.hycompute.commands.OpenTerminalCommand;
import dev.cozygalvinism.hycompute.gui.TerminalGUI;
import dev.cozygalvinism.hycompute.gui.TerminalGUISupplier;

import javax.annotation.Nonnull;

public class HyComputePlugin extends JavaPlugin {
    private static HyComputePlugin INSTANCE;

    public HyComputePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        HyComputePlugin.INSTANCE = this;
    }

    public static HyComputePlugin get() {
        return HyComputePlugin.INSTANCE;
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand("example", "An example command"));
        this.getCommandRegistry().registerCommand(new OpenTerminalCommand());

        OpenCustomUIInteraction.registerCustomPageSupplier(this, TerminalGUI.class, "ComputerTerminal", new TerminalGUISupplier());
    }

    @Override
    protected void start() {
        super.start();
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }
}