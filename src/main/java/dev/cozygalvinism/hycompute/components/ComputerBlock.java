package dev.cozygalvinism.hycompute.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;
import javax.annotation.Nullable;

public class ComputerBlock implements Component<ChunkStore> {
    public static final BuilderCodec<ComputerBlock> CODEC = BuilderCodec.builder(ComputerBlock.class, ComputerBlock::new)
            .append(new KeyedCodec<>("ComputerId", Codec.STRING),
                    (comp, id) -> comp.blockId = id,
                    comp -> comp.blockId)
            .add()
            .build();
    private String blockId;

    public ComputerBlock() {
    }

    public ComputerBlock(String blockId) {
        this.blockId = blockId;
    }

    public String getBlockId() {
        return this.blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public static ComponentType<ChunkStore, ComputerBlock> getComponentType() {
        return HyComputePlugin.get().getComputerComponentType();
    }

    @Nullable
    public Component<ChunkStore> clone() {
        return new ComputerBlock(this.blockId);
    }
}