**Module:** `fs`

| Entry                      | Description                                                                                                          |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `exists(path)`             | Returns whether the given path exists on the filesystem                                                              |
| `isDir(path)`              | Returns whether the given path is a directory                                                                        |
| `isReadOnly(path)`         | Returns whether the given path is read-only                                                                          |
| `list(<path>)`             | Returns the files and folders in the given path. If path is not specified, it will list the files and folders in `/` |
| `mkdir(path)`              | Recursively creates the folder(s) in the given path, if they don't exist                                             |
| `makeDir(path)`            | Alias for `mkdir(path)`                                                                                              |
| `delete(path)`             | Recursively deletes the file or folder at the specified path                                                         |
| `copy(src, dest)`          | Copies a file from `src` to `dest`. This function does not copy folders                                              |
| `move(src, dest)`          | Moves a file from `src` to `dest` by copying it from `src` to `dest` and then deleting `dest`                        |
| `getSize(path)`            | Returns the size of the specified path.                                                                              |
| `getFreeSpace()`           | Returns the amount of free space on this computer in bytes                                                           |
| `getCapacity()`            | Returns the total amount of space on this computer in bytes                                                          |
| `readAll(path)`            | Returns the contents of the file at `path`                                                                           |
| `writeAll(path, content)`  | Writes `content` to `path`, creating a new file or overwriting an existing one                                       |
| `appendAll(path, content)` | Writes `content` to `path`, creating a new file or appending to an existing one                                      |
