package dev.cozygalvinism.hycompute;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.cozygalvinism.hycompute.components.ComputerBlock;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;

public class GlobalSystems {
    public static class CheckComputerQueue extends TickingSystem<ChunkStore> {

        @Override
        public void tick(float dt, int index, @NonNullDecl Store<ChunkStore> _store) {
            List<HyComputePlugin.QueuedComputerId> processed = new ArrayList<>();
            HyComputePlugin.get().getQueuedComputers()
                    .forEach(queuedComp -> {
                        World w = Universe.get().getWorld(queuedComp.worldId());
                        if (w == null) return;
                        Store<ChunkStore> chunkStore = w.getChunkStore().getStore();
                        Vector3i targetBlock = queuedComp.blockPos();
                        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
                        Ref<ChunkStore> chunkRef = chunkStore.getExternalData().getChunkReference(chunkIndex);
                        if (chunkRef == null || !chunkRef.isValid()) return;

                        BlockComponentChunk bcu = chunkStore.getComponent(
                                chunkRef,
                                BlockComponentChunk.getComponentType()
                        );
                        if (bcu == null) return;

                        int blockIndex = ChunkUtil.indexBlockInColumn(targetBlock.x, targetBlock.y, targetBlock.z);

                        Ref<ChunkStore> blockEntityRef = bcu.getEntityReference(blockIndex);
                        if (blockEntityRef == null || !blockEntityRef.isValid()) return;

                        ComputerBlock comp = new ComputerBlock(queuedComp.computerId());
                        chunkStore.putComponent(blockEntityRef, ComputerBlock.getComponentType(), comp);
                        processed.add(queuedComp);
                    });
            HyComputePlugin.get().removeQueuedComputers(processed);
        }
    }
}
