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
        self.current_command_type = ""
        self.current_command_arg1 = ""
        self.current_command_arg2 = -1
        self.line_num = 0
        self.iteration_ended = False

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
            if self.current_command[0] == "/" and self.current_command[1] == "/":
                raise IndexError
            self.line_num += 1

            self.current_command_type = self.commandType()
            self.current_command_arg1 = self.arg1()
            self.current_command_arg2 = self.arg2()
            self.current_command = self.current_command.split()[0]
        except IndexError:
            self.current_command = ""
            self.current_command_type = ""
            self.current_command_arg1 = ""
            self.current_command_arg2 = -1
        except StopIteration:
            self.current_command = ""
            self.current_command_type = ""
            self.current_command_arg1 = ""
            self.current_command_arg2 = -1
            self.iteration_ended = True

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
    def arg1(self) -> str:
        # Parse and return argument.
        if self.current_command_type == CommandType.C_ARITHMETIC:
            return self.current_command.split(" ")[0]
        elif self.current_command == "" or self.current_command_type == CommandType.C_RETURN:
            return ""
        else:
            return self.current_command.split(" ")[1]

    # Return second argument of the current command.
    # Only called if current command is C_PUSH, C_POP, C_FUNCTION, C_CALL.
    def arg2(self) -> int:
        if self.current_command == "":
            return -1
        elif self.current_command_type == CommandType.C_PUSH or self.current_command_type == CommandType.C_POP or \
             self.current_command_type == CommandType.C_FUNCTION or self.current_command_type == CommandType.C_CALL:
            return int(self.current_command.split(" ")[2])
        else:
            return -1

class CodeWriter:
    def __init__(self, filename: str):
        # Open file
        try:
            self.file = open(filename, "w")
        except:
            print("Error occured while creating/opening file: " + filename)
            exit()   

    def __del__(self):
        # Close file
        try:
            self.file.close()
        except AttributeError:
            pass

    # Write to output arithmetically equivalent assembly.
    def writeArithmetic(self, command: str) -> None:
        if command == "add":
            # SP--
            self.file.write("@SP\n")
            self.file.write("M=M-1\n")
            # Pop first value
            self.file.write("@SP\n")
            self.file.write("A=M\n")
            self.file.write("D=M\n")
            # SP--
            self.file.write("SP\n")
            self.file.write("M=M-1\n")
            # Add first value onto second value
            self.file.write("A=M\n")
            self.file.write("D=D+M\n")
            self.file.write("M=D\n")
            # SP++
            self.file.write("@SP\n")
            self.file.write("M=M+1\n")

        elif command == "sub":
            # SP--
            self.file.write("@SP\n")
            self.file.write("M=M-1\n")
            # Pop first value
            self.file.write("@SP\n")
            self.file.write("A=M\n")
            self.file.write("D=M\n")
            # SP--
            self.file.write("SP\n")
            self.file.write("M=M-1\n")
            # Subtract first value onto second value
            self.file.write("A=M\n")
            self.file.write("D=D-M\n")
            self.file.write("M=D\n")
            # SP++
            self.file.write("@SP\n")
            self.file.write("M=M+1\n")

        elif command == "neg":
            # SP--
            self.file.write("@SP\n")
            self.file.write("M=M-1\n")
            # Pop first value
            self.file.write("@SP\n")
            self.file.write("A=M\n")
            self.file.write("D=M\n")
            # Subtract that value twice
            self.file.write("A=D\n")
            self.file.write("A=A-D\n")
            self.file.write("A=A-D\n")
            # Push back onto stack
            self.file.write("D=A\n")
            self.file.write("@0\n")
            self.file.write("A=M\n")
            self.file.write("M=D\n")
            # SP++
            self.file.write("@SP\n")
            self.file.write("M=M+1\n")


        elif command == "eq":
            # equal if you subtract them from one another and get zero
            pass

        elif command == "gt":
            pass

        elif command == "lt":
            pass

        elif command == "and":
            pass

        elif command == "or":
            pass

        elif command == "not":
            pass

        else:
            print(f"Incorrect line, should be a valid arithmetic command (add, sub, neg, eq, gt, lt, and, or, not): {command}")
            exit()  

    def basicPush(self):
        pass 

    def basicPop(self):
        pass

    # Write to output logically equivalent push/pop command.
    def writePushPop(self, command:str, segment: str, index: int) -> None:
        if command == "push":
            # addr=SEGMENT+i
            self.file.write(f"@{segment}\n")
            self.file.write("D=M\n")
            self.file.write(f"@{index}\n")
            self.file.write("D=D+A\n")
            self.file.write("@addr\n")
            self.file.write("M=D\n")
            # *SP=*addr
            self.file.write("@addr\n")
            self.file.write("D=M\n")
            self.file.write("@SP\n")
            self.file.write("M=D\n")
            # SP++
            self.file.write("@SP\n")
            self.file.write("M=M+1\n")
        elif command == "pop":
            # addr=SEGMENT+i
            self.file.write(f"@{segment}\n")
            self.file.write("D=M\n")
            self.file.write(f"@{index}\n")
            self.file.write("D=D+A\n")
            self.file.write("@addr\n")
            self.file.write("M=D\n")
            # SP--
            self.file.write("@SP\n")
            self.file.write("M=M-1\n")
            # *addr=*SP
            self.file.write("@SP\n")
            self.file.write("D=M\n")
            self.file.write("@addr\n")
            self.file.write("M=D\n")
        else:
            print(f"Incorrect line, should be push or pop command: {command} {segment} {index}")
            exit()   

def main():
    parser = Parser("in/" + sys.argv[1] + ".vm")
    writer = CodeWriter("out/" + sys.argv[1] + ".asm")
    
    # Runs until file ends
    while not parser.advance() and not parser.iteration_ended:
        print(parser.current_command, parser.current_command_type == CommandType.C_ARITHMETIC)
        if parser.current_command == "":
            # print(parser.current_command)
            # print(parser.current_command_arg1)
            # print(parser.current_command_arg2, "\n")
            pass
        elif parser.current_command_type == CommandType.C_ARITHMETIC:
            writer.writeArithmetic(parser.current_command)
        elif parser.current_command_type == CommandType.C_PUSH or parser.current_command_type == CommandType.C_PUSH:
            writer.writePushPop(parser.current_command, parser.current_command_arg1, parser.current_command_arg2)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("USAGE: py vm-translator filename")
    else:
        main()
