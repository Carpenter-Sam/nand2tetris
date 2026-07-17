Symbol table
- HashMap (Symbol, location)

Code
- Two passes:
    - First pass, labels to symbol table.
    - Second pass:
        - If @Symbol found, look up in symbol table.
        - If C-instruction, complete translation.
- Mneomic -> Code (Use lookup tbles for dest, jump, comp)

Parser (Read file and split up components)
- Read file given name.
- Move to next line.
- Get fields of current command (A, C, L).
