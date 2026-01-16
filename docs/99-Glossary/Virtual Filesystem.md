The **Virtual Filesystem** or **VFS** is a unified file system interface for in-game computers. It uses a mount-based architecture where different directories can be backed by different storage implementations.

## Components

### VirtualFilesystem

The main class that orchestrates all filesystem operations. It manages multiple mount points and provides a single interface for file operations.

By default, it creates 3 mount points: `/rom` for read-only memory, like builtin programs, `/tmp` for temporary files that will be deleted when the computer boots again and `/home` for persistent file storage.

It also provides path normalization and resolution across the managed mount points, as well as a set of methods for reading, writing and deleting files on the filesystem.

## Mount Types

### ReadOnlyMount

A read-only filesystem that wraps a MemoryMount internally. Used for the `/rom` directory to store system files and programs that shouldn't be modified by users.

### MemoryMount

An in-memory filesystem implementation that stores all data in RAM. Used for the `/tmp` directory. It does not persist any data across reboots and is considered volatile.

### DiskMount

A persistent filesystem that stores data on the actual disk. Used for the `/home` directory. It internally uses Java's NIO Path and Files APIs to read and write files on the actual host filesystem.