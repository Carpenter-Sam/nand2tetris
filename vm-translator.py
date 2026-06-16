from enum import Enum
import sys

class CommandType(Enum):
    C_ARITHMETIC = "C_ARITHMETIC"
    C_PUSH = "C_PUSH"
    C_POP = "C_POP"
    C_LABEL = "C_LABEL"
    C_GOTO = "C_GOTO"
    C_IF = "C_IF"
    C_FUNCTION = "C_FUNCTION"
    C_RETURN = "C_RETURN"
    C_CALL = "C_CALL"

class Parser:
    def __init__(self, filename: str):
        self.current_command = ""

        # Open file

    def __del__(self):
        # Close file
        pass

    # Are there any more lines left in the input?
    def hasMoreLines(self) -> bool: # type: ignore
        pass

    # Set next command to be equal to current command.
    # Only called if hasMoreLines() is true.
    def advance(self) -> None:
        pass

    # Return a constant representing the type of the current command.
    def commandType(self) -> CommandType: # type: ignore
        # Parse and return enum constant.
        pass

    # Return first argument of the current command.
    # If C_ARITHMETIC, the command itself (add, sub, etc.) is returned.
    # Not called if C_RETURN.
    def arg1(self) -> str: # type: ignore
        # Parse and return argument.
        pass

    # Return second argument of the current command.
    # Only called if current command is C_PUSH, C_POP, C_FUNCTION, C_CALL.
    def arg2(self) -> int: # type: ignore
        # Parse and return argument.
        pass

class CodeWriter:
    def __init__(self, filename: str):
        # Open file
        pass

    def __del__(self):
        # Close file
        pass

    # Write to output arithmetically equivalent assembly.
    def writeArithmetic(self, command: str) -> None:
        pass

    # Write to output logically equivalent push/pop command.
    def writePushPop(self, command:str, segment: str, index: int) -> None:
        pass
        
    
def main():
    parser = Parser("in/" + sys.argv[1] + ".vm")
    writer = Parser("out/" + sys.argv[1] + ".asm")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("USAGE: py vm-translator filename")
    else:
        main()
