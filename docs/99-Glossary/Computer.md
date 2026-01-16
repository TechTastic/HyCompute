A computer is a block (soon bench) which allows you to write programs in Lua. They are backed by the [[Virtual Filesystem|VFS]] to store files and folders on them.

Once a computer is turned on for the first time, it will create a folder in `mods/HyCompute_HyCompute/computers/{computer_id}` and populate it with folders. The computer's ID is then stored in the block of the computer via a component. This component persists across server restarts.

> [!warning]
> The computer's ID currently does **NOT** persist when the block is broken and will lose the ID it's attached to, leading to a new computer ID being generated when it is booted again.
