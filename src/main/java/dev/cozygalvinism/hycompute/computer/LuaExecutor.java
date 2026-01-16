package dev.cozygalvinism.hycompute.computer;

import dev.cozygalvinism.hycompute.HyComputePlugin;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.MathLib;
import org.squiddev.cobalt.lib.StringLib;
import org.squiddev.cobalt.lib.TableLib;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LuaExecutor {
    private final Computer computer;
    private final LuaState state;
    private final LuaTable globals;

    public LuaExecutor(Computer computer) {
        this.computer = computer;
        this.state = LuaState.builder().build();
        this.globals = new LuaTable();

        setupSandbox();
        setupTermAPI();
        setupFsAPI();
        setupOsAPI();
        setupShellAPI();
    }

    private void setupSandbox() {
        globals.rawset("_G", globals);
        globals.rawset("_VERSION", ValueFactory.valueOf("Lua 5.1 (Cobalt)"));

        BaseLib.add(globals);

        try {
            StringLib.add(state, globals);
        } catch (LuaError e) {
            HyComputePlugin.get().getLogger()
                    .atSevere()
                    .log("Unable to initialize string lib", e);
        }

        try {
            TableLib.add(state, globals);
        } catch (LuaError e) {
            HyComputePlugin.get().getLogger()
                    .atSevere()
                    .log("Unable to initialize table lib", e);
        }

        try {
            MathLib.add(state, globals);
        } catch (LuaError e) {
            HyComputePlugin.get().getLogger()
                    .atSevere()
                    .log("Unable to initialize math lib", e);
        }

        globals.rawset("dofile", Constants.NIL);
        globals.rawset("loadfile", Constants.NIL);
        globals.rawset("load", Constants.NIL);
        globals.rawset("loadstring", Constants.NIL);

        globals.rawset("print", new PrintFunction());

        globals.rawset("tonumber", new ToNumberFunction());
        globals.rawset("tostring", new ToStringFunction());
        globals.rawset("type", new TypeFunction());
        globals.rawset("pairs", new PairsFunction());
        globals.rawset("ipairs", new IPairsFunction());
        globals.rawset("next", new NextFunction());
        globals.rawset("select", new SelectFunction());
        globals.rawset("unpack", new UnpackFunction());
        globals.rawset("pcall", new PcallFunction());
        globals.rawset("error", new ErrorFunction());
    }

    private void setupTermAPI() {
        LuaTable term = new LuaTable();

        term.rawset("write", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
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
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
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
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
                computer.clearScreen();
                return Constants.NIL;
            }
        });

        term.rawset("getSize", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
                return ValueFactory.varargsOf(
                        ValueFactory.valueOf(80),
                        ValueFactory.valueOf(25)
                );
            }
        });

        globals.rawset("term", term);
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

        globals.rawset("fs", fs);
    }

    private void setupOsAPI() {
        LuaTable os = new LuaTable();

        os.rawset("clock", new VarArgFunction() {
            private final long startTime = System.currentTimeMillis();

            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                return ValueFactory.valueOf((System.currentTimeMillis() - startTime) / 1000.0);
            }
        });

        os.rawset("time", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                return ValueFactory.valueOf(System.currentTimeMillis() / 1000);
            }
        });

        os.rawset("date", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                return ValueFactory.valueOf(java.time.LocalDateTime.now().toString());
            }
        });

        os.rawset("getComputerID", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                return ValueFactory.valueOf(computer.getId().toString());
            }
        });

        os.rawset("shutdown", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                computer.shutdown();
                return Constants.NIL;
            }
        });

        os.rawset("reboot", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                computer.reboot();
                return Constants.NIL;
            }
        });

        globals.rawset("os", os);
    }

    private void setupShellAPI() {
        LuaTable shell = new LuaTable();
        VirtualFilesystem vfs = computer.getFilesytem();

        shell.rawset("resolve", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
                String path = args.arg(1).checkString();
                return ValueFactory.valueOf(computer.resolvePath(path));
            }
        });

        shell.rawset("dir", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
                return ValueFactory.valueOf(computer.getWorkingDirectory());
            }
        });

        shell.rawset("setDir", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
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

        globals.rawset("shell", shell);
    }

    public ExecutionResult execute(String code) {
        return execute(code, "input");
    }

    public ExecutionResult execute(String code, String chunkName) {
        String exprCode = "return " + code;
        try {
            InputStream stream = new ByteArrayInputStream(exprCode.getBytes(StandardCharsets.UTF_8));
            LuaClosure closure = LoadState.load(state, stream, chunkName, globals);
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
            LuaClosure closure = LoadState.load(state, stream, chunkName, globals);
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

    public LuaState getState() {
        return state;
    }

    public LuaTable getGlobals() {
        return globals;
    }

    private class PrintFunction extends VarArgFunction {
        @Override
        protected Varargs invoke(LuaState luaState, Varargs args) throws LuaError, UnwindThrowable {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= args.count(); i++) {
                if (i > 1) sb.append("\t");
                sb.append(args.arg(i).toString());
            }
            computer.print(sb.toString());
            return Constants.NIL;
        }
    }

    private class ToNumberFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaValue val = args.arg(1);
            int base = args.arg(2).optInteger(10);

            if (val.isNumber()) {
                return val;
            }
            if (val.isString()) {
                try {
                    if (base == 10) {
                        return ValueFactory.valueOf(Double.parseDouble(val.toString()));
                    } else {
                        return ValueFactory.valueOf(Integer.parseInt(val.toString(), base));
                    }
                } catch (NumberFormatException e) {
                    return Constants.NIL;
                }
            }
            return Constants.NIL;
        }
    }

    private class ToStringFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            return ValueFactory.valueOf(args.arg(1).toString());
        }
    }

    private class TypeFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaValue val = args.arg(1);
            return ValueFactory.valueOf(val.typeName());
        }
    }

    private class PairsFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaTable table = args.arg(1).checkTable();
            return ValueFactory.varargsOf(new NextFunction(), table, Constants.NIL);
        }
    }

    private class IPairsFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            final LuaTable table = args.arg(1).checkTable();
            return ValueFactory.varargsOf(new VarArgFunction() {
                @Override
                public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                    int i = args.arg(2).checkInteger() + 1;
                    LuaValue val = table.rawget(i);
                    if (val.isNil()) {
                        return Constants.NIL;
                    }
                    return ValueFactory.varargsOf(ValueFactory.valueOf(i), val);
                }
            }, table, ValueFactory.valueOf(0));
        }
    }

    private class NextFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaTable table = args.arg(1).checkTable();
            LuaValue key = args.arg(2);
            Varargs next = table.next(key);
            return next;
        }
    }

    private class SelectFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaValue selector = args.arg(1);
            if (selector.isString() && selector.toString().equals("#")) {
                return ValueFactory.valueOf(args.count() - 1);
            }
            int index = selector.checkInteger();
            if (index < 0) {
                index = args.count() + index + 1;
            }
            if (index <= 0) {
                throw new LuaError("bad argument #1 to 'select' (index out of range)");
            }
            return args.subargs(index + 1);
        }
    }

    private class UnpackFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaTable table = args.arg(1).checkTable();
            int start = args.arg(2).optInteger(1);
            int end = args.arg(3).optInteger(table.length());

            if (start > end) {
                return Constants.NONE;
            }

            LuaValue[] values = new LuaValue[end - start + 1];
            for (int i = start; i <= end; i++) {
                values[i - start] = table.rawget(i);
            }
            return ValueFactory.varargsOf(values);
        }
    }

    private class PcallFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            LuaFunction func = args.arg(1).checkFunction();
            try {
                Varargs result = Dispatch.invoke(state, func, args.subargs(2));
                return ValueFactory.varargsOf(Constants.TRUE, result);
            } catch (LuaError | UnwindThrowable e) {
                return ValueFactory.varargsOf(Constants.FALSE, ValueFactory.valueOf(e.getMessage()));
            }
        }
    }

    private class ErrorFunction extends VarArgFunction {
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            String message = args.arg(1).optString("error");
            throw new LuaError(message);
        }
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

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public Varargs getResult() { return result; }
    }
}
