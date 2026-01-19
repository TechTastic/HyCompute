package dev.cozygalvinism.hycompute.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;

import javax.annotation.Nullable;

public class ComputerOn implements Component<ChunkStore> {
    public static final BuilderCodec<ComputerOn> CODEC = BuilderCodec
            .builder(ComputerOn.class, ComputerOn::new).build();

    public static ComponentType<ChunkStore, ComputerOn> getComponentType() {
        return HyComputePlugin.get().getComputerOnComponentType();
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ComputerOn();
    }
}
