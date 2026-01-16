package dev.cozygalvinism.hycompute.computer.mounts;

import dev.cozygalvinism.hycompute.computer.VirtualFilesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiskMount implements VirtualFilesystem.Mount {
    private final Path root;

    public DiskMount(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create mount directory: " + root, e);
        }
    }

    private Path resolve(String path) {
        if (path.isEmpty()) return root;
        return root.resolve(path);
    }

    @Override
    public String readFile(String path) throws VirtualFilesystem.FSException {
        try {
            return Files.readString(resolve(path));
        } catch (IOException e) {
            throw new VirtualFilesystem.FSException("Failed to read file: " + e.getMessage());
        }
    }

    @Override
    public void writeFile(String path, String content) throws VirtualFilesystem.FSException {
        try {
            Path target = resolve(path);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new VirtualFilesystem.FSException("Failed to write file: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) throws VirtualFilesystem.FSException {
        try {
            Path target = resolve(path);
            if (Files.isDirectory(target)) {
                try (Stream<Path> walk = Files.walk(target)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.delete(p); }
                                catch (IOException ignored) {}
                            });
                }
            } else {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            throw new VirtualFilesystem.FSException("Failed to delete: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolve(path));
    }

    @Override
    public boolean isDirectory(String path) {
        return Files.isDirectory(resolve(path));
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public List<String> list(String path) throws VirtualFilesystem.FSException {
        try {
            Path target = resolve(path);
            if (!Files.isDirectory(target)) {
                throw new VirtualFilesystem.FSException("Not a directory");
            }
            try (Stream<Path> stream = Files.list(target)) {
                return stream
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new VirtualFilesystem.FSException("Failed to list directory: " + e.getMessage());
        }
    }

    @Override
    public void mkdir(String path) throws VirtualFilesystem.FSException {
        try {
            Files.createDirectories(resolve(path));
        } catch (IOException e) {
            throw new VirtualFilesystem.FSException("Failed to create directory: " + e.getMessage());
        }
    }

    @Override
    public long getUsedSpace() {
        try {
            try (Stream<Path> walk = Files.walk(root)) {
                return walk
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException _) {
                                return 0;
                            }
                        })
                        .sum();
            }
        } catch (IOException _) {
            return 0;
        }
    }

    @Override
    public long getSize(String path) throws VirtualFilesystem.FSException {
        Path target = resolve(path);
        if (!Files.exists(target)) {
            throw new VirtualFilesystem.FSException("Not found: " + path);
        }

        try {
            if (Files.isDirectory(target)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    return walk
                            .filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try {
                                    return Files.size(p);
                                } catch (IOException e) {
                                    return 0;
                                }
                            })
                            .sum();
                }
            } else {
                return Files.size(target);
            }
        } catch (IOException e) {
            throw new VirtualFilesystem.FSException("Failed to get size: " + e.getMessage());
        }
    }
}
