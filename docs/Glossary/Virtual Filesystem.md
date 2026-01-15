**Virtual Filesystem** or **VFS** is a completely sandboxed filesystem implementation for HyCompute. It does not support flags, ownership or other metadata commonly found in actual filesystems and relies completely on the plugin to make the distinction on what is writable and what isn't.

In it's current state, it stores very simple information to support basic filesystem operations.

The filesystem consists of a tree of nodes with a single root node. Every node has the minimum required information for itself to function:

- A name (like `/` or `tmp`)
- Whether the node is a directory
- The children of the node as a `Map<String, FSNode>` (if it is a directory)
- The content of the node (if it's not a directory)

As such, the filesystem can easily be serialized into data formats, if necessary.