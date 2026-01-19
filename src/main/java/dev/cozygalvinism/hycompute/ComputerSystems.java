package dev.cozygalvinism.hycompute;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.component.system.System;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.cozygalvinism.hycompute.components.ComputerBlock;
import dev.cozygalvinism.hycompute.components.ComputerOn;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

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
}
