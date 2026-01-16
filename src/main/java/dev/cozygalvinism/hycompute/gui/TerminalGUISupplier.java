package dev.cozygalvinism.hycompute.gui;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;
import dev.cozygalvinism.hycompute.components.ComputerBlock;
import dev.cozygalvinism.hycompute.components.ComputerEntity;
import dev.cozygalvinism.hycompute.computer.Computer;
import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.UUID;

public class TerminalGUISupplier implements OpenCustomUIInteraction.CustomPageSupplier {
    private final Path computersRoot;

    public TerminalGUISupplier(Path dataDirectory) {
        this.computersRoot = dataDirectory.resolve("computers");
    }

    @Nullable
    @Override
    public CustomUIPage tryCreate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> componentAccessor, PlayerRef playerRef, InteractionContext interactionContext) {
        BlockPosition blockPos = interactionContext.getTargetBlock();
        if (blockPos == null) {
            return null;
        }

        if (playerRef == null || !playerRef.isValid() || playerRef.getWorldUuid() == null) {
            return null;
        }

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return null;
        }

        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, blockPos.x, blockPos.y, blockPos.z);
        if (blockRef == null || !blockRef.isValid()) {
            return null;
        }
        Store<ChunkStore> store = blockRef.getStore();

        ComputerBlock comp = store.getComponent(blockRef, ComputerBlock.getComponentType());
        if (comp == null) {
            // cheeky trying to create the component
            HyComputePlugin.get().getLogger().atWarning()
                    .log("Adding ComputerBlock to block");
            comp = new ComputerBlock();
        }

        if (comp.getBlockId() == null || comp.getBlockId().isEmpty()) {
            comp.setBlockId(UUID.randomUUID().toString());
            store.putComponent(blockRef, ComputerBlock.getComponentType(), comp);
        }

        UUID computerId = UUID.fromString(comp.getBlockId());
        Path computerPath = computersRoot.resolve(computerId.toString());

        try {
            Computer computer = new Computer(computerId, computerPath);
            return new TerminalGUI(playerRef, computer);
        } catch (VirtualFilesystem.FSException e) {
            throw new RuntimeException("Failed to create computer", e);
        }
    }
}
