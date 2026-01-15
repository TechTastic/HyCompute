Currently, HyCompute does not have persistent data storage, leading to created files and folders on the computer's filesystem being reset any time the computer is turned on (which currently also does not persist; e.g. any time you interact with a computer, it "boots" up anew again).

The plan is for files under `/home` to persist somewhere in the files and for the [[Virtual Filesystem|VFS]] to map directly to a folder in a safe path[^1]. Files under `/rom` will be persistently generated at the boot of the computer and will stay read-only. Files under `/tmp` will not be mapped anywhere, which will lead to their deletion once the computer reboots.

[^1]: Meaning a path, that persists across restarts and reinstalls of the server files. A location has yet to be determined.
