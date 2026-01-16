package dev.cozygalvinism.hycompute;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeFloat;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.commands.ExampleCommand;
import dev.cozygalvinism.hycompute.commands.OpenTerminalCommand;
import dev.cozygalvinism.hycompute.components.ComputerBlock;
import dev.cozygalvinism.hycompute.components.ComputerEntity;
import dev.cozygalvinism.hycompute.gui.TerminalGUI;
import dev.cozygalvinism.hycompute.gui.TerminalGUISupplier;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class HyComputePlugin extends JavaPlugin {
    private static HyComputePlugin INSTANCE;
    private ComponentType<ChunkStore, ComputerBlock> computerComponentType;
    private ComponentType<EntityStore, ComputerEntity> computerEntityComponentType;

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

        OpenCustomUIInteraction.registerCustomPageSupplier(this, TerminalGUI.class, "ComputerTerminal", new TerminalGUISupplier(this.getDataDirectory()));

        this.computerComponentType = this.getChunkStoreRegistry()
                .registerComponent(ComputerBlock.class, "ComputerBlock", ComputerBlock.CODEC);
        this.computerEntityComponentType = this.getEntityStoreRegistry()
                .registerComponent(ComputerEntity.class, "ComputerEntity", ComputerEntity.CODEC);
    }

    public ComponentType<ChunkStore, ComputerBlock> getComputerComponentType() {
        return this.computerComponentType;
    }

    public ComponentType<EntityStore, ComputerEntity> getComputerEntityComponentType() {
        return this.computerEntityComponentType;
    }

    public List<String> listComputers() {
        Path computersRoot = this.getDataDirectory().resolve("computers");

        if (!Files.exists(computersRoot)) {
            return List.of();
        }

        try {
            try (Stream<Path> computers = Files.list(computersRoot).filter(Files::isDirectory)) {
                return computers.map(Path::getFileName).map(Path::toString).toList();
            }
        } catch (IOException e) {
            getLogger().atSevere().withCause(e).log("Unable to list computers");
        }

        return List.of();
    }

    @Override
    protected void start() {
        super.start();
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }

    public Path getComputersRoot() {
        return this.getDataDirectory().resolve("computers");
    }
}