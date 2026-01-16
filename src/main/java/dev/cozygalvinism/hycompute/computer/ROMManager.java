package dev.cozygalvinism.hycompute.computer;

import java.util.HashMap;
import java.util.Map;

public class ROMManager {
    private static final Map<String, String> ROM_FILES = new HashMap<>();

    public static void populateFilesystem(VirtualFilesystem fs) {
        for (Map.Entry<String, String> entry : ROM_FILES.entrySet()) {
            fs.writeROM(entry.getKey(), entry.getValue());
        }
    }

    private static final String HELP_PROGRAM = """
            local programs = fs.list("/rom/programs")
            print("Available commands:")
            print("")
            for _, name in ipairs(programs) do
                local cmd = name:gsub("%.lua$", "")
                print("  " .. cmd)
            end
            print("")
            print("Type '<command>' to run a program.")
            print("Type 'lua <code>' to run Lua directly")
            """;

    private static final String LS_PROGRAM = """
            local path = arg[1]
            
            if not path then
                path = shell.dir()
            else
                path = shell.resolve(path)
            end
            
            if not fs.exists(path) then
                print("No such directory: " .. path)
                return
            end
            
            if not fs.isDir(path) then
                print(path)
                return
            end
            
            local files = fs.list(path)
            for _, name in ipairs(files) do
                local fullPath = path .. "/" .. name
                if path == "/" then
                    fullPath = "/" .. name
                end
            
                if fs.isDir(fullPath) then
                    print("[" .. name .. "]")
                else
                    print(name)
                end
            end
            """;

    private static final String CAT_PROGRAM = """
            local path = arg[1]
            
            if not path then
                print("Usage: cat <file>")
                return
            end
            
            path = shell.resolve(path)
            
            if not fs.exists(path) then
                print("No such file: " .. path)
                return
            end
            
            if fs.isDir(path) then
                print("Is a directory")
                return
            end
            
            print(fs.readAll(path))
            """;

    private static final String MKDIR_PROGRAM = """
            local path = arg[1]
            
            if not path then
                print("Usage: mkdir <directory>")
                return
            end
            
            path = shell.resolve(path)
            
            if fs.exists(path) then
                print("Directory already exists: " .. path)
                return
            end
            
            fs.mkdir(path)
            print("Created: " .. path)
            """;

    private static final String RM_PROGRAM = """
            local path = arg[1]
            
            if not path then
                print("Usage: rm <file|directory>")
                return
            end
            
            path = shell.resolve(path)
            
            if not fs.exists(path) then
                print("No such file: " .. path)
                return
            end
            
            if fs.isReadOnly(path) then
                print("Cannot delete read-only file")
                return
            end
            
            fs.delete(path)
            print("Deleted: " .. path)
            """;

    private static final String CP_PROGRAM = """
            local src = arg[1]
            local dest = arg[2]
            
            if not src or not dest then
                print("Usage: cp <source> <destination>")
                return
            end
            
            src = shell.resolve(src)
            dest = shell.resolve(dest)
            
            if not fs.exists(src) then
                print("No such file: " .. src)
                return
            end
            
            if fs.isDir(src) then
                print("Cannot copy directories (yet)")
                return
            end
            
            fs.copy(src, dest)
            print("Copied: " .. src .. " -> " .. dest)
            """;

    private static final String MV_PROGRAM = """
        local src = arg[1]
        local dest = arg[2]
        
        if not src or not dest then
            print("Usage: mv <source> <destination>")
            return
        end
        
        src = shell.resolve(src)
        dest = shell.resolve(dest)
        
        if not fs.exists(src) then
            print("No such file: " .. src)
            return
        end
        
        if fs.isReadOnly(src) then
            print("Cannot move read-only file")
            return
        end
        
        fs.move(src, dest)
        print("Moved: " .. src .. " -> " .. dest)
        """;

    private static final String ECHO_PROGRAM = """
        local text = table.concat(arg, " ")
        print(text)
        """;

    private static final String CLEAR_PROGRAM = """
        term.clear()
        """;

    private static final String REBOOT_PROGRAM = """
        print("Rebooting...")
        os.reboot()
        """;

    private static final String SHUTDOWN_PROGRAM = """
        print("Shutting down...")
        os.shutdown()
        """;

    private static final String DF_PROGRAM = """
        local used = fs.getCapacity() - fs.getFreeSpace()
        local total = fs.getCapacity()
        local percent = math.floor((used / total) * 100)
        
        print("Disk usage:")
        print(string.format("  Used:  %d bytes", used))
        print(string.format("  Free:  %d bytes", fs.getFreeSpace()))
        print(string.format("  Total: %d bytes", total))
        print(string.format("  Usage: %d%%", percent))
        """;

    private static final String CD_PROGRAM = """
            local path = arg[1]
            
            if not path then
                shell.setDir("/home")
                return
            end
            
            if path == "~" then
                shell.setDir("/home")
                return
            end
            
            local resolved = shell.resolve(path)
            
            if not fs.exists(resolved) then
                print("No such directory: " .. path)
                return
            end
            
            if not fs.isDir(resolved) then
                print("Not a directory: " .. path)
                return
            end
            
            shell.setDir(resolved)
            """;

    private static final String PWD_PROGRAM = """
            print(shell.dir())
            """;

    static {
        // shell programs
        ROM_FILES.put("/rom/programs/help.lua", HELP_PROGRAM);
        ROM_FILES.put("/rom/programs/ls.lua", LS_PROGRAM);
        ROM_FILES.put("/rom/programs/cat.lua", CAT_PROGRAM);
        ROM_FILES.put("/rom/programs/mkdir.lua", MKDIR_PROGRAM);
        ROM_FILES.put("/rom/programs/rm.lua", RM_PROGRAM);
        ROM_FILES.put("/rom/programs/cp.lua", CP_PROGRAM);
        ROM_FILES.put("/rom/programs/mv.lua", MV_PROGRAM);
        ROM_FILES.put("/rom/programs/echo.lua", ECHO_PROGRAM);
        ROM_FILES.put("/rom/programs/clear.lua", CLEAR_PROGRAM);
        ROM_FILES.put("/rom/programs/reboot.lua", REBOOT_PROGRAM);
        ROM_FILES.put("/rom/programs/shutdown.lua", SHUTDOWN_PROGRAM);
        ROM_FILES.put("/rom/programs/df.lua", DF_PROGRAM);
        ROM_FILES.put("/rom/programs/cd.lua", CD_PROGRAM);
        ROM_FILES.put("/rom/programs/pwd.lua", PWD_PROGRAM);
    }
}
