package dev.cozygalvinism.hycompute.computer;

import dev.cozygalvinism.hycompute.HyComputePlugin;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LuaExecutor {
    private final Computer computer;
    private final LuaState state;

    public LuaExecutor(Computer computer) {
        this.computer = computer;
        this.state = LuaState.builder().build();

        setupSandbox();
        setupTermAPI();
        setupFsAPI();
        setupOsAPI();
        setupShellAPI();

        // alias some commonly used functions
        try {
            state.globals().rawset("print", state.globals().rawget("term").checkTable().rawget("print").checkFunction());
        } catch (LuaError e) {
            HyComputePlugin.get().getLogger()
                    .atSevere()
                    .log("Unable to copy print from term", e);
        }
    }

    private void setupSandbox() {
        try {
            CoreLibraries.standardGlobals(state);
        } catch (LuaError e) {
            HyComputePlugin.get().getLogger()
                    .atSevere()
                    .log("Unable to initialize core libs", e);
        }
    }

    private void setupTermAPI() {
        LuaTable term = new LuaTable();

        term.rawset("write", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.count(); i++) {
                    if (i > 1) sb.append("\t");
                    sb.append(args.arg(i).toString());
                }
                computer.printInline(sb.toString());
                return Constants.NIL;
            }
        });

        term.rawset("print", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.count(); i++) {
                    if (i > 1) sb.append("\t");
                    sb.append(args.arg(i).toString());
                }
                computer.print(sb.toString());
                return Constants.NIL;
            }
        });

        term.rawset("clear", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) {
                computer.clearScreen();
                return Constants.NIL;
            }
        });

        term.rawset("getSize", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) {
                return ValueFactory.varargsOf(
                        ValueFactory.valueOf(80),
                        ValueFactory.valueOf(25)
                );
            }
        });

        state.globals().rawset("term", term);
    }

    private void setupFsAPI() {
        LuaTable fs = new LuaTable();
        VirtualFilesystem vfs = computer.getFilesytem();

        fs.rawset("exists", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                return ValueFactory.valueOf(vfs.exists(path));
            }
        });

        fs.rawset("isDir", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                return ValueFactory.valueOf(vfs.isDirectory(path));
            }
        });

        fs.rawset("isReadOnly", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                return ValueFactory.valueOf(vfs.isReadOnly(path));
            }
        });

        fs.rawset("list", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).optString("/");
                try {
                    List<String> files = vfs.list(path);
                    LuaTable result = new LuaTable();
                    for (int i = 0; i < files.size(); i++) {
                        result.rawset(i + 1, ValueFactory.valueOf(files.get(i)));
                    }
                    return result;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        VarArgFunction mkdir = new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                try {
                    vfs.mkdir(path);
                    return Constants.NIL;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        };
        fs.rawset("makeDir", mkdir);
        fs.rawset("mkdir", mkdir);

        fs.rawset("delete", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                try {
                    vfs.delete(path);
                    return Constants.NIL;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        fs.rawset("copy", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String src = args.arg(1).checkString();
                String dest = args.arg(2).checkString();
                try {
                    vfs.copy(src, dest);
                    return Constants.NIL;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        fs.rawset("move", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String src = args.arg(1).checkString();
                String dest = args.arg(2).checkString();
                try {
                    vfs.move(src, dest);
                    return Constants.NIL;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        fs.rawset("getSize", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                try {
                    return ValueFactory.valueOf(vfs.getSize(path));
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        fs.rawset("getFreeSpace", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) {
                return ValueFactory.valueOf(vfs.getFreeSpace());
            }
        });

        fs.rawset("getCapacity", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) {
                return ValueFactory.valueOf(vfs.getCapacity());
            }
        });

        // File reading
        fs.rawset("readAll", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                try {
                    return ValueFactory.valueOf(vfs.readFile(path));
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        // File writing
        fs.rawset("writeAll", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                String content = args.arg(2).checkString();
                try {
                    vfs.writeFile(path, content);
                    return Constants.NIL;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        // Append to file
        fs.rawset("appendAll", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                String content = args.arg(2).checkString();
                try {
                    String existing = vfs.exists(path) ? vfs.readFile(path) : "";
                    vfs.writeFile(path, existing + content);
                    return Constants.NIL;
                } catch (VirtualFilesystem.FSException e) {
                    throw new LuaError(e.getMessage());
                }
            }
        });

        state.globals().rawset("fs", fs);
    }

    private void setupOsAPI() {
        LuaTable os = new LuaTable();

        os.rawset("clock", new VarArgFunction() {
            private final long startTime = System.currentTimeMillis();

            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) {
                return ValueFactory.valueOf((System.currentTimeMillis() - startTime) / 1000.0);
            }
        });

        os.rawset("time", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) {
                return ValueFactory.valueOf((double) System.currentTimeMillis() / 1000);
            }
        });

        os.rawset("date", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) {
                return ValueFactory.valueOf(java.time.LocalDateTime.now().toString());
            }
        });

        os.rawset("getComputerID", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) {
                return ValueFactory.valueOf(computer.getId().toString());
            }
        });

        os.rawset("shutdown", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) {
                computer.shutdown();
                return Constants.NIL;
            }
        });

        os.rawset("reboot", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) {
                computer.reboot();
                return Constants.NIL;
            }
        });

        state.globals().rawset("os", os);
    }

    private void setupShellAPI() {
        LuaTable shell = new LuaTable();
        VirtualFilesystem vfs = computer.getFilesytem();

        shell.rawset("resolve", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                return ValueFactory.valueOf(computer.resolvePath(path));
            }
        });

        shell.rawset("dir", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) {
                return ValueFactory.valueOf(computer.getWorkingDirectory());
            }
        });

        shell.rawset("setDir", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError {
                String path = args.arg(1).checkString();
                String resolved = computer.resolvePath(path);

                if (!vfs.exists(resolved)) {
                    throw new LuaError("Directory not found: " + path);
                }
                if (!vfs.isDirectory(resolved)) {
                    throw new LuaError("Not a directory: " + path);
                }

                computer.setWorkingDirectory(resolved);
                return Constants.NIL;
            }
        });

        state.globals().rawset("shell", shell);
    }

    public ExecutionResult execute(String code) {
        return execute(code, "input");
    }

    public ExecutionResult execute(String code, String chunkName) {
        String exprCode = "return " + code;
        try {
            InputStream stream = new ByteArrayInputStream(exprCode.getBytes(StandardCharsets.UTF_8));
            LuaClosure closure = LoadState.load(state, stream, chunkName, state.globals());
            Varargs result = Dispatch.call(state, closure);

            // If we got a result, print it
            if (result != Constants.NONE && !result.arg(1).isNil()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= result.count(); i++) {
                    if (i > 1) sb.append("\t");
                    sb.append(result.arg(i).toString());
                }
                computer.print(sb.toString());
            }
            return new ExecutionResult(true, null, result);
        } catch (CompileException | LuaError | UnwindThrowable e) {
            // Expression didn't work, try as regular statement
        }

        // Try as regular statement
        try {
            InputStream stream = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
            LuaClosure closure = LoadState.load(state, stream, chunkName, state.globals());
            Varargs result = Dispatch.call(state, closure);
            return new ExecutionResult(true, null, result);
        } catch (CompileException e) {
            return new ExecutionResult(false, "Syntax error: " + e.getMessage(), null);
        } catch (LuaError e) {
            return new ExecutionResult(false, "Runtime error: " + e.getMessage(), null);
        } catch (UnwindThrowable e) {
            return new ExecutionResult(false, "Unwind error: " + e.getMessage(), null);
        }
    }

    public ExecutionResult executeFile(String path) {
        try {
            String code = computer.getFilesytem().readFile(path);
            return execute(code, path);
        } catch (VirtualFilesystem.FSException e) {
            return new ExecutionResult(false, "File error: " + e.getMessage(), null);
        }
    }

    public LuaTable getGlobals() {
        return state.globals();
    }

    public static class ExecutionResult {
        private final boolean success;
        private final String error;
        private final Varargs result;

        public ExecutionResult(boolean success, String error, Varargs result) {
            this.success = success;
            this.error = error;
            this.result = result;
        }

        public boolean isError() { return !success; }
        public String getError() { return error; }
        public Varargs getResult() { return result; }
    }
}
