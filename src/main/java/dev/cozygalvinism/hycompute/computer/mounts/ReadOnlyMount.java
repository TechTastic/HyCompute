package dev.cozygalvinism.hycompute.computer.mounts;

import dev.cozygalvinism.hycompute.HyComputePlugin;
import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;

import java.util.List;

public class ReadOnlyMount implements VirtualFilesystem.Mount {

    private final MemoryMount internal = new MemoryMount();

    public void writeInternal(String path, String content) {
        try {
            internal.writeFile(path, content);
        } catch (VirtualFilesystem.FSException e) {
            HyComputePlugin.get().getLogger().atWarning()
                    .log("Failed to write to internal filesystem: " + e.getMessage());
        }
    }

    public void mkdirInternal(String path) {
        try {
            internal.mkdir(path);
        } catch (VirtualFilesystem.FSException e) {
            HyComputePlugin.get().getLogger().atWarning()
                    .log("Failed to create directory in internal filesystem: " + e.getMessage());
        }
    }

    @Override
    public String readFile(String path) throws VirtualFilesystem.FSException {
        return internal.readFile(path);
    }

    @Override
    public void writeFile(String path, String content) throws VirtualFilesystem.FSException {
        throw new VirtualFilesystem.FSException("Read-only filesystem");
    }

    @Override
    public void delete(String path) throws VirtualFilesystem.FSException {
        throw new VirtualFilesystem.FSException("Read-only filesystem");
    }

    @Override
    public boolean exists(String path) {
        return internal.exists(path);
    }

    @Override
    public boolean isDirectory(String path) {
        return internal.isDirectory(path);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public List<String> list(String path) throws VirtualFilesystem.FSException {
        return internal.list(path);
    }

    @Override
    public void mkdir(String path) throws VirtualFilesystem.FSException {
        throw new VirtualFilesystem.FSException("Read-only filesystem");
    }

    @Override
    public long getUsedSpace() {
        return internal.getUsedSpace();
    }

    @Override
    public long getSize(String path) throws VirtualFilesystem.FSException {
        return internal.getSize(path);
    }
}
