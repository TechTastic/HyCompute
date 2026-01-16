**Module:** `term`

| Entry        | Description                                                                                                                                                                    |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `write(...)` | Writes the arguments to the terminal. Arguments are converted to Strings and separated by tab characters. This function currently modifies the last line of the output buffer! |
| `print(...)` | Writes the arguments to the terminal. Arguments are converted to Strings and separated by tab characters. The entire String is appended as a new line to the output buffer.    |
| `clear()`    | Clears the terminal by emptying the output buffer.                                                                                                                             |
| `getSize()`  | Currently always returns `80, 25`                                                                                                                                              |
