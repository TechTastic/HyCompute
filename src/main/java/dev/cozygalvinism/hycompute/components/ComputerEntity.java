package dev.cozygalvinism.hycompute.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nullable;

public class ComputerEntity implements Component<EntityStore> {
    public static final BuilderCodec<ComputerEntity> CODEC = BuilderCodec.builder(ComputerEntity.class, ComputerEntity::new)
            .append(new KeyedCodec<>("ComputerId", Codec.STRING),
                    (comp, id) -> comp.computerId = id,
                    comp -> comp.computerId)
            .add()
            .build();

    private String computerId = "";
    private boolean isOn = false;

    public ComputerEntity() {}

    public ComputerEntity(String computerId, boolean isOn) {
        this.computerId = computerId;
        this.isOn = isOn;
    }

    public String getComputerId() {
        return computerId;
    }

    public void setComputerId(String computerId) {
        this.computerId = computerId;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }

    public static ComponentType<EntityStore, ComputerEntity> getComponentType() {
        return HyComputePlugin.get().getComputerEntityComponentType();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new ComputerEntity(this.computerId, this.isOn);
    }
}
