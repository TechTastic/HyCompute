package dev.cozygalvinism.hycompute.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderField;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.cozygalvinism.hycompute.HyComputePlugin;
import dev.cozygalvinism.hycompute.computer.Computer;
import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TerminalGUI extends InteractiveCustomUIPage<TerminalGUI.TerminalData> {
    private final Computer computer;

    // private final List<String> outputLines = new ArrayList<>();
    private String currentInput = "";

    public TerminalGUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, TerminalData.CODEC);

        try {
            this.computer = new Computer(UUID.randomUUID());
        } catch (VirtualFilesystem.FSException e) {
            throw new RuntimeException(e);
        }
        this.computer.boot();
    }

//    private void appendOutput(String line) {
//        outputLines.add(line);
//    }

//    private String buildOutputText() {
//        StringBuilder sb = new StringBuilder();
//        for (String line : outputLines) {
//            sb.append(line).append("\n");
//        }
//        sb.append("> ");
//        return sb.toString();
//    }

//    private void handleCommand(String input) {
//        if (input == null || input.trim().isEmpty()) {
//            return;
//        }
//
//        String trimmed = input.trim();
//        appendOutput("> " + trimmed);
//
//        String response = processCommand(input);
//        if (response != null && !response.isEmpty()) {
//            for (String line : response.split("\n")) {
//                appendOutput(line);
//            }
//        }
//        appendOutput("");
//    }
//
//    private String processCommand(String input) {
//        String cmd = input.toLowerCase();
//        String[] parts = input.split(" ", 2);
//        String command = parts[0].toLowerCase();
//        String args = parts.length > 1 ? parts[1] : "";
//
//        return switch (command) {
//            case "help" -> "Available commands:\n  help  - Show this message\n  echo  - Echo text back\n  clear - Clear screen\n  about - About HyCompute";
//            case "echo" -> args;
//            case "clear" -> {
//                outputLines.clear();
//                yield "Screen cleared.";
//            }
//            case "about" -> "HyCompute v0.1\nA ComputerCraft-inspired mod for Hytale\nBy cozyGalvinism";
//            default -> "Unknown command: " + command + "\nType 'help' for available commands.";
//        };
//    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/HyCompute_Terminal.ui");

        String prompt = computer.getWorkingDirectory() + "> ";
        cmd.set("#OutputText.Text", computer.getScreenContent() + "\n" + prompt);
        cmd.set("#InputField.Value", "");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SubmitButton",
                new EventData()
                        .append("Action", "Submit")
                        .append("@Input", "#InputField.Value"),
                false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull TerminalData data
    ) {
        super.handleDataEvent(ref, store, data);

        if (data.input != null) {
            this.currentInput = data.input;
        }

        if ("Submit".equals(data.action) && currentInput != null) {
            String prompt = computer.getWorkingDirectory() + "> ";
            computer.print(prompt + currentInput);

            String response = computer.executeCommand(data.input);
            if (response != null && !response.isEmpty()) {
                computer.print(response);
            }
            this.currentInput = "";

            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder events = new UIEventBuilder();
            build(ref, cmd, events, store);
            sendUpdate(cmd, events, true);
        }
    }

    public static class TerminalData {
        public static final BuilderCodec<TerminalData> CODEC = BuilderCodec
                .builder(TerminalData.class, TerminalData::new)
                .append(
                        new KeyedCodec<>("@Input", Codec.STRING),
                        (d, s) -> d.input = s,
                        d -> d.input
                )
                .add()
                .append(
                        new KeyedCodec<>("Action", Codec.STRING),
                        (d, s) -> d.action = s,
                        d -> d.action
                )
                .add()
                .append(
                        new KeyedCodec<>("Key", Codec.STRING),
                        (d, s) -> d.keyData = s,
                        d -> d.keyData
                )
                .add()
                .build();

        private String input;
        private String action;
        private String keyData;

        public TerminalData() {}
    }
}
