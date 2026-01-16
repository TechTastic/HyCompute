package dev.cozygalvinism.hycompute.computer;

import dev.cozygalvinism.hycompute.computer.mounts.DiskMount;
import dev.cozygalvinism.hycompute.computer.mounts.MemoryMount;
import dev.cozygalvinism.hycompute.computer.mounts.ReadOnlyMount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VirtualFilesystem {
    private final Map<String, Mount> mounts = new LinkedHashMap<>();
    private final long capacity;

    public VirtualFilesystem(Path computerRoot, long capacity) throws FSException {
        this.capacity = capacity;

        try {
            Files.createDirectories(computerRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create computer directory", e);
        }

        mounts.put("/rom", new ReadOnlyMount());
        mounts.put("/tmp", new MemoryMount());
        mounts.put("/home", new DiskMount(computerRoot.resolve("home")));
    }

    private MountResult resolveMount(String path) {
        String normalized = normalizePath(path);

        for (Map.Entry<String, Mount> entry : mounts.entrySet()) {
            String mountPoint = entry.getKey();
            if (normalized.equals(mountPoint) || normalized.startsWith(mountPoint + "/")) {
                String relativePath = normalized.equals(mountPoint)
                        ? ""
                        : normalized.substring(mountPoint.length() + 1);
                return new MountResult(entry.getValue(), mountPoint, relativePath);
            }
        }

        return new MountResult(null, "/", normalized);
    }

    public String readFile(String path) throws FSException {
        MountResult mr = resolveMount(path);
        if (mr.mount == null) {
            throw new FSException("Invalid path: " + path);
        }
        return mr.mount.readFile(mr.relativePath);
    }

    public void writeFile(String path, String content) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            throw new FSException("Cannot write to root directory");
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) {
            throw new FSException("Invalid path: " + path);
        }

        if (getUsedSpace() + content.length() > getCapacity()) {
            throw new FSException("Not enough space to write file");
        }

        mr.mount.writeFile(mr.relativePath, content);
    }

    public void delete(String path) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            throw new FSException("Cannot delete root directory");
        }

        for (String mountPoint : mounts.keySet()) {
            if (normalized.equals(mountPoint)) {
                throw new FSException("Cannot delete read-only location: " + path);
            }
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) {
            throw new FSException("Invalid path: " + path);
        }
        mr.mount.delete(mr.relativePath);
    }

    public boolean exists(String path) {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) return true;
        for (String mountPoint : mounts.keySet()) {
            if (normalized.equals(mountPoint)) return true;
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) return false;
        return mr.mount.exists(mr.relativePath);
    }

    public boolean isDirectory(String path) {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) return true;
        for (String mountPoint : mounts.keySet()) {
            if (normalized.equals(mountPoint)) return true;
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) return false;
        return mr.mount.isDirectory(mr.relativePath);
    }

    public boolean isReadOnly(String path) {
        MountResult mr = resolveMount(path);
        if (mr.mount == null) return true;
        return mr.mount.isReadOnly();
    }

    public List<String> list(String path) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            List<String> result = new ArrayList<>();
            for (String mountPoint : mounts.keySet()) {
                result.add(mountPoint.substring(1));
            }
            return result;
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) {
            throw new FSException("Invalid path: " + path);
        }
        return mr.mount.list(mr.relativePath);
    }

    public void mkdir(String path) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            throw new FSException("Cannot modify root directory");
        }

        for (String mountPoint : mounts.keySet()) {
            if (normalized.equals(mountPoint)) {
                return;
            }
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) {
            throw new FSException("Invalid path: " + path);
        }
        mr.mount.mkdir(mr.relativePath);
    }

    public void copy(String source, String dest) throws FSException {
        String content = readFile(source);
        writeFile(dest, content);
    }

    public void move(String source, String dest) throws FSException {
        copy(source, dest);
        delete(source);
    }

    public long getUsedSpace() {
        long total = 0;
        for (Mount mount : mounts.values()) {
            total += mount.getUsedSpace();
        }
        return total;
    }

    public long getFreeSpace() {
        return capacity - getUsedSpace();
    }

    public long getCapacity() {
        return capacity;
    }

    public long getSize(String path) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            return getUsedSpace();
        }

        for (Map.Entry<String, Mount> entry : mounts.entrySet()) {
            if (normalized.equals(entry.getKey())) {
                return entry.getValue().getUsedSpace();
            }
        }

        MountResult mr = resolveMount(path);
        if (mr.mount == null) {
            throw new FSException("Invalid path: " + path);
        }
        return mr.mount.getSize(mr.relativePath);
    }

    public void writeROM(String path, String content) {
        MountResult mr = resolveMount(path);
        if (mr.mount instanceof ReadOnlyMount rom) {
            rom.writeInternal(mr.relativePath, content);
        }
    }

    public String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        List<String> parts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty() && !part.equals(".")) {
                if (part.equals("..")) {
                    if (!parts.isEmpty()) {
                        parts.removeLast();
                    }
                } else {
                    parts.add(part);
                }
            }
        }

        if (parts.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", parts);
    }

    public String resolvePath(String path, String workingDirectory) {
        if (path == null || path.isEmpty()) {
            return normalizePath(workingDirectory);
        }

        if (path.startsWith("/")) {
            return normalizePath(path);
        }

        String combined;
        if (workingDirectory.equals("/")) {
            combined = "/" + path;
        } else {
            combined = workingDirectory + "/" + path;
        }

        return normalizePath(combined);
    }

    private record MountResult(Mount mount, String mountPoint, String relativePath) {}

    public interface Mount {
        String readFile(String path) throws FSException;
        void writeFile(String path, String content) throws FSException;
        void delete(String path) throws FSException;
        boolean exists(String path);
        boolean isDirectory(String path);
        boolean isReadOnly();
        List<String> list(String path) throws FSException;
        void mkdir(String path) throws FSException;
        long getUsedSpace();
        long getSize(String path) throws FSException;
    }

    public static class FSException extends Exception {
        public FSException(String message) {
            super(message);
        }
    }
}
