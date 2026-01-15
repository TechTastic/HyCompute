package dev.cozygalvinism.hycompute.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.gui.TerminalGUI;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OpenTerminalCommand extends AbstractAsyncPlayerCommand {
    public OpenTerminalCommand() {
        super("terminal", "Opens the HyCompute terminal");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World w) {
        return CompletableFuture.runAsync(() -> {
            if (playerRef != null) {
                Player p = (Player) context.sender();
                p.getPageManager().openCustomPage(
                        ref,
                        store,
                        new TerminalGUI(playerRef)
                );
            } else {
                context.sendMessage(Message.raw("Could not get player reference."));
            }
        }, w);
    }
}
