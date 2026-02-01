package dev.cozygalvinism.hycompute;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.components.ComputerBlock;
import dev.cozygalvinism.hycompute.components.ComputerOn;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;

public class ComputerSystems {

    private static void setState(ComputerStateSystem.States state, Ref<ChunkStore> ref, Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {
        BlockModule.BlockStateInfo info = commandBuffer.getComponent(
                ref,
                BlockModule.BlockStateInfo.getComponentType()
        );

        if (info != null) {
            Ref<ChunkStore> chunkRef = info.getChunkRef();

            if (chunkRef.isValid()) {
                WorldChunk chunk = store.getComponent(chunkRef, WorldChunk.getComponentType());

                if (chunk != null) {
                    int index = info.getIndex();

                    int chunkBaseX = chunk.getX() << 5;
                    int chunkBaseZ = chunk.getZ() << 5;

                    int x = chunkBaseX + ChunkUtil.xFromBlockInColumn(index);
                    int y = ChunkUtil.yFromBlockInColumn(index);
                    int z = chunkBaseZ + ChunkUtil.zFromBlockInColumn(index);

                    BlockType blockType = chunk.getBlockType(x, y, z);
                    if (blockType != null) {
                        chunk.setBlockInteractionState(x, y, z, blockType, state.toString(), true);
                        chunk.markNeedsSaving();
                    }
                }
            }
        }
    }

    public static class DebugSystem extends RefSystem<ChunkStore> {

        @Override
        public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason addReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            HyComputePlugin.get().getLogger().atInfo()
                    .log("wow, a computer!!!");
        }

        @Override
        public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return Query.and(ComputerBlock.getComponentType(), BlockModule.BlockStateInfo.getComponentType());
        }
    }

    public static class ComputerTurnOffSystem extends RefSystem<ChunkStore> {

        @Override
        public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason addReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            ComputerOn on = commandBuffer.getComponent(ref, ComputerOn.getComponentType());
            if (on == null) {
                setState(ComputerStateSystem.States.OFF, ref, store, commandBuffer);
            }
        }

        @Override
        public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return Query.and(
                    ComputerBlock.getComponentType(),
                    BlockModule.BlockStateInfo.getComponentType()
            );
        }
    }

    public static class ComputerStateSystem extends RefChangeSystem<ChunkStore, ComputerOn> {

        public enum States {
            ON,
            OFF;

            @Override
            public String toString() {
                return switch (this) {
                    case ON -> "On";
                    case OFF -> "Off";
                };
            }
        }

        @NonNullDecl
        @Override
        public ComponentType<ChunkStore, ComputerOn> componentType() {
            return ComputerOn.getComponentType();
        }

        @Override
        public void onComponentAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl ComputerOn computerOn, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            ComputerBlock comp = commandBuffer.getComponent(ref, ComputerBlock.getComponentType());
            if (comp != null) {
                if (comp.getComputer().isRunning()) {
                    setState(States.ON, ref, store, commandBuffer);
                } else {
                    setState(States.OFF, ref, store, commandBuffer);
                }
            }
        }

        @Override
        public void onComponentSet(@NonNullDecl Ref<ChunkStore> ref, @NullableDecl ComputerOn computerOn, @NonNullDecl ComputerOn t1, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            ComputerBlock comp = commandBuffer.getComponent(ref, ComputerBlock.getComponentType());
            if (comp != null) {
                if (comp.getComputer().isRunning()) {
                    setState(States.ON, ref, store, commandBuffer);
                } else {
                    setState(States.OFF, ref, store, commandBuffer);
                }
            }
        }

        @Override
        public void onComponentRemoved(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl ComputerOn computerOn, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            setState(States.OFF, ref, store, commandBuffer);
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return Query.and(
                    ComputerOn.getComponentType(),
                    ComputerBlock.getComponentType(),
                    BlockModule.BlockStateInfo.getComponentType()
            );
        }
    }

    public static class BreakComputerBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public BreakComputerBlockSystem() {
            super(BreakBlockEvent.class);
        }

        @Override
        public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                           @NonNullDecl BreakBlockEvent breakBlockEvent) {
            if (breakBlockEvent.isCancelled()) return;

            Vector3i targetBlock = breakBlockEvent.getTargetBlock();

            World w = archetypeChunk.getReferenceTo(i).getStore().getExternalData().getWorld();
            Store<ChunkStore> chunkStore = w.getChunkStore().getStore();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
            Ref<ChunkStore> chunkRef = chunkStore.getExternalData().getChunkReference(chunkIndex);
            if (chunkRef == null || !chunkRef.isValid()) return;

            BlockComponentChunk blockComponentChunk = chunkStore.getComponent(
                    chunkRef,
                    BlockComponentChunk.getComponentType()
            );
            if (blockComponentChunk == null) return;

            int blockIndex = ChunkUtil.indexBlockInColumn(targetBlock.x, targetBlock.y, targetBlock.z);

            Ref<ChunkStore> blockEntityRef = blockComponentChunk.getEntityReference(blockIndex);
            if (blockEntityRef == null || !blockEntityRef.isValid()) {
                return;
            }

            ComputerBlock comp = chunkStore.getComponent(blockEntityRef, ComputerBlock.getComponentType());
            if (comp == null) return;

            BlockType blockType = breakBlockEvent.getBlockType();
            Item item = blockType.getItem();
            if (item == null) return;

            ItemStack itemStack = new ItemStack(item.getId(), 1)
                    .withMetadata("computer_id", Codec.STRING, comp.getBlockId());
            Vector3d dropPos = targetBlock.toVector3d().add(0.5, 0.0, 0.5);

            Holder<EntityStore>[] itemEntityHolders = ItemComponent.generateItemDrops(
                    store,
                    List.of(itemStack),
                    dropPos,
                    Vector3f.ZERO
            );

            if (itemEntityHolders.length > 0) {
                w.execute(() -> store.addEntities(itemEntityHolders, AddReason.SPAWN));
            }

            w.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, "Empty", SetBlockSettings.NO_DROP_ITEMS);
            breakBlockEvent.setCancelled(true);
        }

        @NullableDecl
        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    public static class PlaceComputerBlockSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        public PlaceComputerBlockSystem() {
            super(PlaceBlockEvent.class);
        }

        @Override
        public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                           @NonNullDecl PlaceBlockEvent placeBlockEvent) {

            if (placeBlockEvent.isCancelled()) return;

            ItemStack itemStack = placeBlockEvent.getItemInHand();
            if (itemStack == null) return;

            String computerId = itemStack.getFromMetadataOrNull("computer_id", Codec.STRING);
            if (computerId == null) return;

            HyComputePlugin.get().getLogger().atInfo()
                    .log("Placing computer with ID %s", computerId);

            Vector3i pos = placeBlockEvent.getTargetBlock();
            HyComputePlugin.get().queueComputer(pos, computerId, store.getExternalData().getWorld().getWorldConfig().getUuid());
        }

        @NullableDecl
        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }
}
