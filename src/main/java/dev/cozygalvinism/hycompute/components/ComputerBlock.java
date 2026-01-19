package dev.cozygalvinism.hycompute.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;
import dev.cozygalvinism.hycompute.computer.Computer;
import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.UUID;

public class ComputerBlock implements Component<ChunkStore> {
    public static final BuilderCodec<ComputerBlock> CODEC = BuilderCodec.builder(ComputerBlock.class, ComputerBlock::new)
            .append(new KeyedCodec<>("ComputerId", Codec.STRING),
                    (comp, id) -> comp.blockId = id,
                    comp -> comp.blockId)
            .add()
            .build();
    private String blockId;
    private Computer computer;

    public ComputerBlock() {
    }

    public ComputerBlock(String blockId) {
        this.blockId = blockId;

        try {
            this.computer = new Computer(UUID.fromString(this.blockId), HyComputePlugin.get().getComputerPath(this.blockId));
        } catch (VirtualFilesystem.FSException e) {
            throw new RuntimeException(e);
        }
    }

    public ComputerBlock(String blockId, Computer computer) {
        this.blockId = blockId;
        this.computer = computer;
    }

    public String getBlockId() {
        return this.blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public Computer getComputer() {
        if (this.blockId != null && this.computer == null) {
            try {
                this.computer = new Computer(UUID.fromString(this.blockId), HyComputePlugin.get().getComputerPath(this.blockId));
            } catch (VirtualFilesystem.FSException e) {
                throw new RuntimeException(e);
            }
        }
        return computer;
    }

    public static ComponentType<ChunkStore, ComputerBlock> getComponentType() {
        return HyComputePlugin.get().getComputerBlockComponentType();
    }

    @Nullable
    public Component<ChunkStore> clone() {
        if (this.blockId == null || this.blockId.isEmpty()) {
            return new ComputerBlock();
        } else {
            return new ComputerBlock(this.blockId);
        }
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> cloneSerializable() {
        return new ComputerBlock(this.blockId, this.computer);
    }

    @Override
    public String toString() {
        if (computer != null) {
            return "ComputerBlock{" +
                    "blockId='" + blockId + '\'' +
                    ", computer=" + computer.getId() +
                    '}';
        } else {
            return "ComputerBlock{" +
                    "blockId='" + blockId + '\'' +
                    ", computer=null" +
                    '}';
        }
    }
}