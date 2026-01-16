package dev.cozygalvinism.hycompute.computer;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.ValueFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Computer {
    private final UUID id;
    private final VirtualFilesystem filesystem;
    private final List<String> outputBuffer;
    private final StringBuilder currentLine = new StringBuilder();
    private String workingDirectory = "/home";
    private boolean running;
    private LuaExecutor luaExecutor;

    public Computer(UUID id, Path storagePath) throws VirtualFilesystem.FSException {
        this.id = id;
        this.filesystem = new VirtualFilesystem(storagePath, 1024 * 1024);
        this.outputBuffer = new ArrayList<>();
        this.running = false;
    }

    public UUID getId() { return id; }
    public VirtualFilesystem getFilesytem() { return filesystem; }
    public List<String> getOutputBuffer() { return outputBuffer; }
    public boolean isRunning() { return running; }

    public void print(String text) {
        if (!currentLine.isEmpty()) {
            outputBuffer.add(currentLine.toString());
            currentLine.setLength(0);
        }
        outputBuffer.add(text);
    }

    public void clearScreen() {
        outputBuffer.clear();
        currentLine.setLength(0);
    }

    public String getScreenContent() {
        if (!currentLine.isEmpty()) {
            return String.join("\n", outputBuffer) + "\n" + currentLine.toString();
        }
        return String.join("\n", outputBuffer);
    }

    public void flushCurrentLine() {
        if (!currentLine.isEmpty()) {
            outputBuffer.add(currentLine.toString());
            currentLine.setLength(0);
        }
    }

    public void boot() {
        running = true;
        clearScreen();

        ROMManager.populateFilesystem(filesystem);

        luaExecutor = new LuaExecutor(this);
        print("HyCompute OS v0.1");
        print("Type 'help' for commands.");
        print("");

        if (filesystem.exists("/home/startup.lua")) {
            LuaExecutor.ExecutionResult result = luaExecutor.executeFile("/home/startup.lua");
            if (result.isError()) {
                print("Startup error: " + result.getError());
            }
        }
    }

    public void reboot() {
        shutdown();
        boot();
    }

    public void printInline(String text) {
        currentLine.append(text);
    }

    public void shutdown() {
        running = false;
    }

    public LuaExecutor getLuaExecutor() { return luaExecutor; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = filesystem.normalizePath(workingDirectory);
    }

    public String resolvePath(String path) {
        return filesystem.resolvePath(path, workingDirectory);
    }

    public String executeCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();
        String[] parts = trimmed.split("\\s+", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        if (command.equals("lua")) {
            if (args.isEmpty()) {
                return "Usage: lua <code>";
            }
            LuaExecutor.ExecutionResult result = luaExecutor.execute(args);
            if (result.isError()) {
                return result.getError();
            }
            return null;
        }

        // TODO: implement something like a PATH variable
        String localProgramPath = resolvePath(command + ".lua");
        if (filesystem.exists(localProgramPath)) {
            return runProgram(localProgramPath, args);
        }

        String programPath = "/rom/programs/" + command + ".lua";
        if (filesystem.exists(programPath)) {
            return runProgram(programPath, args);
        }

        String userProgramPath = "/home/" + command + ".lua";
        if (filesystem.exists(userProgramPath)) {
            return runProgram(userProgramPath, args);
        }

        LuaExecutor.ExecutionResult result = luaExecutor.execute(trimmed);
        if (result.isError()) {
            return "Unknown command: " + command + ". Type 'help' for available commands.";
        }

        return null;
    }

    private String runProgram(String path, String args) {
        String[] argParts = args.isEmpty() ? new String[0] : args.split("\\s+");
        LuaTable argTable = new LuaTable();
        argTable.rawset(0, ValueFactory.valueOf(path));
        for (int i = 0; i < argParts.length; i++) {
            argTable.rawset(i + 1, ValueFactory.valueOf(argParts[i]));
        }
        luaExecutor.getGlobals().rawset("arg", argTable);

        LuaExecutor.ExecutionResult result = luaExecutor.executeFile(path);
        if (result.isError()) {
            return result.getError();
        }
        return null;
    }
}
