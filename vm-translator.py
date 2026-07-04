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
        self.file_iter = ""
        self.line_num = 0

        # Open file
        try:
            self.file = open(filename)
        except(FileNotFoundError):
            print("Error file not found: " + filename)
            exit()       
        
    def __del__(self):
        # Close file
        try:
            self.file.close()
        except AttributeError:
            pass

    # Set next command to be equal to current command and remove whitespace.
    def advance(self) -> None:
        try:
            self.current_command = next(self.file).split('//')[0].strip()
            self.line_num += 1
        except StopIteration:
            self.current_command = ""

    # Return a constant representing the type of the current command.
    def commandType(self) -> CommandType: # type: ignore
        # Parse and return enum constant.
        match self.current_command.split(" ")[0]:
            case "push":
                return CommandType("C_PUSH")
            case "pop":
                return CommandType("C_POP")
            case "label":
                return CommandType("C_LABEL")
            case "goto":
                return CommandType("C_GOTO")
            case "if ":
                return CommandType("C_IF")
            case "function":
                return CommandType("C_FUNCTION")
            case "return":
                return CommandType("C_RETURN")
            case "call":
                return CommandType("C_CALL")

            case "add":
                return CommandType("C_ARITHMETIC")
            case "sub":
                return CommandType("C_ARITHMETIC")
            case "neg":
                return CommandType("C_ARITHMETIC")
            case "eq":
                return CommandType("C_ARITHMETIC")
            case "gt":
                return CommandType("C_ARITHMETIC")
            case "lt":
                return CommandType("C_ARITHMETIC")
            case "and":
                return CommandType("C_ARITHMETIC")
            case "or":
                return CommandType("C_ARITHMETIC")
            case "not":
                return CommandType("C_ARITHMETIC")
            
            case _:
                print(f"Invalid command at line {self.line_num}: {self.current_command}")
                exit()  

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
    writer = CodeWriter("out/" + sys.argv[1] + ".asm")
    
    # Runs until file ends
    while parser.advance() or parser.current_command != "":
        print(parser.current_command)
        print(parser.commandType())

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("USAGE: py vm-translator filename")
    else:
        main()
