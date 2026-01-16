package dev.cozygalvinism.hycompute.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionProvider;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;
import dev.cozygalvinism.hycompute.computer.Computer;
import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;
import dev.cozygalvinism.hycompute.gui.TerminalGUI;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OpenTerminalCommand extends AbstractAsyncPlayerCommand {
    private final RequiredArg<String> computerIdArg;

    public OpenTerminalCommand() {
        super("terminal", "Opens the HyCompute terminal");

        this.computerIdArg = withRequiredArg("computerId", "The computer terminal to open", ArgTypes.STRING)
                .suggest(new SuggestionProvider() {
                    @Override
                    public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered, int numParametersTyped, @Nonnull SuggestionResult result) {
                        HyComputePlugin.get().listComputers()
                                .forEach(c -> {
                                    if (c.toLowerCase().startsWith(textAlreadyEntered.toLowerCase())) {
                                        result.suggest(c);
                                    }
                                });
                    }
                });
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World w) {
        return CompletableFuture.runAsync(() -> {
            if (playerRef != null) {
                Player p = (Player) context.sender();

                String computerId = this.computerIdArg.get(context);
                Path computerPath = HyComputePlugin.get().getComputersRoot().resolve(computerId);

                try {
                    Computer computer = new Computer(UUID.fromString(computerId), computerPath);
                    p.getPageManager().openCustomPage(
                            ref,
                            store,
                            new TerminalGUI(playerRef, computer)
                    );
                } catch (VirtualFilesystem.FSException e) {
                    context.sendMessage(Message.raw("Unable to get computer"));
                }
            } else {
                context.sendMessage(Message.raw("Could not get player reference."));
            }
        }, w);
    }
}
