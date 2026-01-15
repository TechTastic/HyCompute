package dev.cozygalvinism.hycompute.computer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualFilesystem {
    private final FSNode root;
    private final long capacity;
    private long usedSpace;

    public VirtualFilesystem() throws FSException {
        this(1024 * 1024);
    }

    public VirtualFilesystem(long capacity) throws FSException {
        this.capacity = capacity;
        this.usedSpace = 0;
        this.root = new FSNode("/", true);

        mkdir("/rom");
        mkdir("/rom/programs");
        mkdir("/home");
        mkdir("/tmp");
    }

    public String readFile(String path) throws FSException {
        FSNode node = resolve(normalizePath(path));

        if (node == null) {
            throw new FSException("File not found: " + path);
        }

        if (node.isDirectory) {
            throw new FSException("Cannot read directory: " + path);
        }

        return node.content;
    }

    public void writeFile(String path, String content) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.startsWith("/rom/") || normalized.equals("/rom")) {
            throw new FSException("Cannot write to read-only location: " + path);
        }

        FSNode existing = resolve(normalized);
        long existingSize = existing != null ? existing.getSize() : 0;
        long newSize = content.length();
        long deltaSize = newSize - existingSize;

        if (usedSpace + deltaSize > capacity) {
            throw new FSException("Not enough disk space");
        }

        String parentPath = getParentPath(normalized);
        if (!parentPath.isEmpty() && !exists(parentPath)) {
            mkdir(parentPath);
        }

        FSNode parent = resolve(parentPath.isEmpty() ? "/" : parentPath);
        String fileName = getFileName(normalized);

        FSNode fileNode = parent.children.get(fileName);
        if (fileNode == null) {
            fileNode = new FSNode(fileName, false);
            parent.children.put(fileName, fileNode);
        } else if (fileNode.isDirectory) {
            throw new FSException("Cannot overwrite directory with file: " + path);
        }

        fileNode.content = content;
        usedSpace += deltaSize;
    }

    public void deleteFile(String path) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            throw new FSException(("Cannot delete root directory"));
        }

        if (normalized.startsWith("/rom/") || normalized.equals("/rom")) {
            throw new FSException("Cannot delete read-only location: " + path);
        }

        FSNode node = resolve(normalized);
        if (node == null) {
            throw new FSException("File not found: " + path);
        }

        if (node.isDirectory && !node.children.isEmpty()) {
            throw new FSException("Directory not empty: " + path);
        }

        String parentPath = getParentPath(normalized);
        FSNode parent = resolve(parentPath.isEmpty() ? "/" : parentPath);
        String fileName = getFileName(normalized);

        usedSpace -= node.getSize();
        parent.children.remove(fileName);
    }

    public void deleteRecursive(String path) throws FSException {
        String normalized = normalizePath(path);

        if (normalized.equals("/")) {
            throw new FSException("Cannot delete root directory");
        }

        if (normalized.startsWith("/rom/") || normalized.equals("/rom")) {
            throw new FSException("Cannot delete read-only location: " + path);
        }

        FSNode node = resolve(normalized);
        if (node == null) {
            throw new FSException("File not found: " + path);
        }

        String parentPath = getParentPath(normalized);
        FSNode parent = resolve(parentPath.isEmpty() ? "/" : parentPath);
        String fileName = getFileName(normalized);

        usedSpace -= calculateSize(node);
        parent.children.remove(fileName);
    }

    public boolean exists(String path) {
        return resolve(normalizePath(path)) != null;
    }

    public boolean isDirectory(String path) {
        FSNode node = resolve(normalizePath(path));
        return node != null && node.isDirectory;
    }

    public boolean isReadOnly(String path) {
        String normalized = normalizePath(path);
        return normalized.startsWith("/rom/") || normalized.equals("/rom");
    }

    public List<String> list(String path) throws FSException {
        FSNode node = resolve(normalizePath(path));

        if (node == null) {
            throw new FSException("Directory not found: " + path);
        }

        if (!node.isDirectory) {
            throw new FSException("Not a directory: " + path);
        }

        return new ArrayList<>(node.children.keySet());
    }

    public void mkdir(String path) throws FSException {
        String normalized = normalizePath(path);

        if (exists(normalized)) {
            return;
        }

        if (normalized.startsWith("/rom/") && !normalized.equals("/rom/programs")) {
            if (usedSpace > 0) {
                throw new FSException("Cannot create directory in read-only location: " + path);
            }
        }

        String parentPath = getParentPath(normalized);
        if (!parentPath.isEmpty() && !exists(parentPath)) {
            mkdir(parentPath);
        }

        FSNode parent = resolve(parentPath.isEmpty() ? "/" : parentPath);
        if (parent == null) {
            throw new FSException("Parent directory not found: " + parentPath);
        }
        if (!parent.isDirectory) {
            throw new FSException("Parent is not a directory: " + parentPath);
        }

        String dirName = getFileName(normalized);
        parent.children.put(dirName, new FSNode(dirName, true));
    }

    public void copy(String source, String dest) throws FSException {
        String content = readFile(source);
        writeFile(dest, content);
    }

    public void move(String source, String dest) throws FSException {
        copy(source, dest);
        deleteFile(source);
    }

    public long getSize(String path) {
        FSNode node = resolve(normalizePath(path));
        if (node == null) return 0;
        return calculateSize(node);
    }

    public long getFreeSpace() {
        return capacity - usedSpace;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getUsedSpace() {
        return usedSpace;
    }

    public void writeROM(String path, String content) {
        String normalized = normalizePath(path);
        String parentPath = getParentPath(normalized);

        try {
            if (!exists(parentPath)) {
                mkdirInternal(parentPath);
            }
        } catch (FSException e) {

        }

        FSNode parent = resolve(parentPath.isEmpty() ? "/" : parentPath);
        String fileName = getFileName(normalized);

        FSNode fileNode = new FSNode(fileName, false);
        fileNode.content = content;
        parent.children.put(fileName, fileNode);
        usedSpace += content.length();
    }

    private void mkdirInternal(String path) throws FSException {
        String normalized = normalizePath(path);
        if (exists(normalized)) return;

        String parentPath = getParentPath(normalized);
        if (!parentPath.isEmpty() && !exists(parentPath)) {
            mkdirInternal(parentPath);
        }

        FSNode parent = resolve(parentPath.isEmpty() ? "/" : parentPath);
        String dirName = getFileName(normalized);
        parent.children.put(dirName, new FSNode(dirName, true));
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
            if (part.isEmpty() || part.equals(".")) {
                continue;
            } else if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
            } else {
                parts.add(part);
            }
        }

        if (parts.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", parts);
    }

    private String getParentPath(String normalizedPath) {
        int lastSlash = normalizedPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }

        return normalizedPath.substring(0, lastSlash);
    }

    private String getFileName(String normalizedPath) {
        int lastSlash = normalizedPath.lastIndexOf('/');
        return normalizedPath.substring(lastSlash + 1);
    }

    private FSNode resolve(String normalizedPath) {
        if (normalizedPath.equals("/")) {
            return root;
        }

        FSNode current = root;
        String[] parts = normalizedPath.substring(1).split("/");

        for (String part : parts) {
            if (!current.isDirectory) {
                return null;
            }

            current = current.children.get(part);
            if (current == null) {
                return null;
            }
        }

        return current;
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

    private long calculateSize(FSNode node) {
        if (!node.isDirectory) {
            return node.getSize();
        }

        long total = 0;
        for (FSNode child : node.children.values()) {
            total += calculateSize(child);
        }
        return total;
    }

    private static class FSNode {
        @Nonnull final String name;
        final boolean isDirectory;
        @Nullable final Map<String, FSNode> children;
        @Nullable String content;

        FSNode(@Nonnull String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.children = isDirectory ? new HashMap<>() : null;
            this.content = isDirectory ? null : "";
        }

        long getSize() {
            return content != null ? content.length() : 0;
        }
    }

    public static class FSException extends Exception {
        public FSException(String message) {
            super(message);
        }
    }
}
