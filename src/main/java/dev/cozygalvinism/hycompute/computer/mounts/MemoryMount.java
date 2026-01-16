package dev.cozygalvinism.hycompute.computer.mounts;

import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class MemoryMount implements VirtualFilesystem.Mount {
    private final FSNode root;

    public MemoryMount() {
        this.root = new FSNode("", true);
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

    private FSNode resolve(String normalizedPath) {
        if (normalizedPath.equals("/") || normalizedPath.isEmpty()) {
            return root;
        }

        FSNode current = root;
        String[] parts = normalizedPath.startsWith("/")
                ? normalizedPath.substring(1).split("/")
                : normalizedPath.split("/");

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

    private String getParent(String path) {
        int slash = path.lastIndexOf('/');
        return slash <= 0 ? "" : path.substring(0, slash);
    }

    private String getFileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash == -1 ? path : path.substring(slash + 1);
    }

    @Override
    public String readFile(String path) throws VirtualFilesystem.FSException {
        FSNode node = resolve(path);

        if (node == null) {
            throw new VirtualFilesystem.FSException("File not found: " + path);
        }

        if (node.isDirectory) {
            throw new VirtualFilesystem.FSException("Cannot read directory: " + path);
        }

        return node.content;
    }

    @Override
    public void writeFile(String path, String content) throws VirtualFilesystem.FSException {
        if (path.isEmpty()) {
            throw new VirtualFilesystem.FSException("Cannot write to mount root");
        }

        String parentPath = getParent(path);
        String fileName = getFileName(path);

        if (!parentPath.isEmpty()) {
            mkdir(parentPath);
        }

        FSNode parent = parentPath.isEmpty() ? root : resolve(parentPath);
        if (parent == null || !parent.isDirectory) {
            throw new VirtualFilesystem.FSException("Parent directory not found");
        }

        FSNode fileNode = parent.children.get(fileName);
        if (fileNode == null) {
            fileNode = new FSNode(fileName, false);
            parent.children.put(fileName, fileNode);
        } else if (fileNode.isDirectory) {
            throw new VirtualFilesystem.FSException("Cannot overwrite directory with file: " + path);
        }

        fileNode.content = content;
    }

    @Override
    public void delete(String path) throws VirtualFilesystem.FSException {
        if (path.isEmpty()) {
            throw new VirtualFilesystem.FSException("Cannot delete mount root");
        }

        String parentPath = getParent(path);
        String name = getFileName(path);

        FSNode parent = parentPath.isEmpty() ? root : resolve(parentPath);
        if (parent == null) {
            throw new VirtualFilesystem.FSException("Not found: " + path);
        }

        if (!parent.children.containsKey(name)) {
            throw new VirtualFilesystem.FSException("Not found: " + path);
        }

        parent.children.remove(name);
    }

    @Override
    public boolean exists(String path) {
        if (path.isEmpty()) return true;
        return resolve(path) != null;
    }

    @Override
    public boolean isDirectory(String path) {
        if (path.isEmpty()) return true;
        FSNode node = resolve(path);
        return node != null && node.isDirectory;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public List<String> list(String path) throws VirtualFilesystem.FSException {
        FSNode node = path.isEmpty() ? root : resolve(path);
        if (node == null) {
            throw new VirtualFilesystem.FSException("Directory not found: " + path);
        }
        if (!node.isDirectory) {
            throw new VirtualFilesystem.FSException("Not a directory: " + path);
        }
        return new ArrayList<>(node.children.keySet());
    }

    @Override
    public void mkdir(String path) throws VirtualFilesystem.FSException {
        if (path.isEmpty()) return;

        String[] parts = path.split("/");
        FSNode current = root;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            FSNode child = current.children.get(part);
            if (child == null) {
                child = new FSNode(part, true);
                current.children.put(part, child);
            } else if (!child.isDirectory) {
                throw new VirtualFilesystem.FSException("Cannot create directory, file exists: " + part);
            }
            current = child;
        }
    }

    @Override
    public long getUsedSpace() {
        return calculateSize(root);
    }

    @Override
    public long getSize(String path) throws VirtualFilesystem.FSException {
        FSNode node = path.isEmpty() ? root : resolve(path);
        if (node == null) {
            throw new VirtualFilesystem.FSException("Not found: " + path);
        }
        return calculateSize(node);
    }

    private static class FSNode {
        @Nonnull
        final String name;
        final boolean isDirectory;
        @Nullable
        final Map<String, FSNode> children;
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
}
