package dev.cozygalvinism.hycompute;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.cozygalvinism.hycompute.commands.ExampleCommand;
import dev.cozygalvinism.hycompute.commands.OpenTerminalCommand;
import dev.cozygalvinism.hycompute.components.ComputerBlock;
import dev.cozygalvinism.hycompute.components.ComputerOn;
import dev.cozygalvinism.hycompute.gui.TerminalGUI;
import dev.cozygalvinism.hycompute.gui.TerminalGUISupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class HyComputePlugin extends JavaPlugin {
    private static HyComputePlugin INSTANCE;
    private ComponentType<ChunkStore, ComputerBlock> computerBlockComponentType;
    private ComponentType<ChunkStore, ComputerOn> computerOnComponentType;

    private final List<QueuedComputerId> _queuedComputers;

    public record QueuedComputerId(Vector3i blockPos, String computerId, UUID worldId) {
    }

    public HyComputePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        HyComputePlugin.INSTANCE = this;
        this._queuedComputers = new ArrayList<>();
    }

    public static HyComputePlugin get() {
        return HyComputePlugin.INSTANCE;
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new OpenTerminalCommand());

        OpenCustomUIInteraction.registerCustomPageSupplier(this, TerminalGUI.class, "ComputerTerminal", new TerminalGUISupplier());

        this.computerBlockComponentType = this.getChunkStoreRegistry()
                .registerComponent(ComputerBlock.class, "ComputerBlock", ComputerBlock.CODEC);
        this.computerOnComponentType = this.getChunkStoreRegistry()
                .registerComponent(ComputerOn.class, ComputerOn::new);

        this.getChunkStoreRegistry().registerSystem(new ComputerSystems.ComputerStateSystem());
        this.getChunkStoreRegistry().registerSystem(new ComputerSystems.DebugSystem());
        this.getChunkStoreRegistry().registerSystem(new ComputerSystems.ComputerTurnOffSystem());
        this.getChunkStoreRegistry().registerSystem(new GlobalSystems.CheckComputerQueue());
        this.getEntityStoreRegistry().registerSystem(new ComputerSystems.BreakComputerBlockSystem());
        this.getEntityStoreRegistry().registerSystem(new ComputerSystems.PlaceComputerBlockSystem());
    }

    public ComponentType<ChunkStore, ComputerBlock> getComputerBlockComponentType() {
        return this.computerBlockComponentType;
    }

    public ComponentType<ChunkStore, ComputerOn> getComputerOnComponentType() {
        return this.computerOnComponentType;
    }

    public Path getComputerPath(String id) {
        return this.getDataDirectory().resolve("computers").resolve(id);
    }

    public void queueComputer(Vector3i blockPos, String computerId, UUID worldId) {
        _queuedComputers.add(new QueuedComputerId(blockPos, computerId, worldId));
    }

    public Stream<QueuedComputerId> getQueuedComputers() {
        return _queuedComputers.stream();
    }

    public void removeQueuedComputers(List<QueuedComputerId> queuedComputers) {
        _queuedComputers.removeAll(queuedComputers);
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