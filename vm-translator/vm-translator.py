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
            case "if-goto":
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
    def __init__(self, filename: str, filename_strict: str):
        # Open file
        try:
            self.file = open(filename, "w")
            self.file_strict = filename_strict
            self.current_function = ""

            self.egl = 0
            self.call = -1
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
            self.addOrSub(True)

        elif command == "sub":
            self.addOrSub(False)

        elif command == "neg":
            self.file.write("@SP\n")
            self.file.write("M=M-1\n")
            self.file.write("@SP\n")
            self.file.write("A=M\n")
            self.file.write("D=M\n")
            self.file.write("D=-D\n")
            self.file.write("M=D\n")
            self.file.write("@SP\n")
            self.file.write("M=M+1\n")

        elif command == "eq":
            # equal if you subtract them from one another and get zero
            self.eglArithmetic(command)

        elif command == "gt":
            # greater than if you subtract them and the result is above zero
            self.eglArithmetic(command)

        elif command == "lt":
            # greater than if you subtract them and the result is below zero
            self.eglArithmetic(command)

        # and, or, not assume that the value(s) checked on the stack are boolean (0 = False or -1 = True)
        elif command == "and": # due to the above assumption the and command is equivalent to eq
            self.andOrLogic("and")

        elif command == "or":
            self.andOrLogic("or")

        elif command == "not":
            # SP--
            self.file.write("@SP\n")
            self.file.write("M=M-1\n")
            # Not value
            self.file.write("@0\n")
            self.file.write("A=M\n")
            self.file.write("M=!M\n")
            # SP++
            self.file.write("@SP\n")
            self.file.write("M=M+1\n")

        else:
            print(f"Incorrect line, should be a valid arithmetic command (add, sub, neg, eq, gt, lt, and, or, not): {command}")
            exit()  

    def addOrSub(self, adding: bool):
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Pop first value
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("D=M\n")
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Add/subtract first value onto second value
        self.file.write("A=M\n")
        if adding:
            self.file.write("D=D+M\n")
        else:
            self.file.write("D=M-D\n")
        self.file.write("M=D\n")
        # SP++
        self.file.write("@SP\n")
        self.file.write("M=M+1\n")
    
    def eglArithmetic(self, type: str):
        self.addOrSub(False) # subtract top two values

        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Pop result into D
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("D=M\n")

        # pushes -1 if result is true else pushes 0
        self.file.write(f"@{self.file_strict}{type}{self.egl}\n")
        if type == "eq":
            self.file.write(f"D;JEQ\n")
        elif type == "gt":
            self.file.write(f"D;JGT\n")
        elif type == "lt":
            self.file.write(f"D;JLT\n")
        self.writePushPop("push", "constant", 0)
        self.file.write(f"@{self.file_strict}{type}END{self.egl}\n")
        self.file.write("0;JMP\n")
        self.file.write(f"({self.file_strict}{type}{self.egl})\n")
        self.writePushPop("push", "constant", -1)
        self.file.write(f"({self.file_strict}{type}END{self.egl})\n")
        self.egl += 1
    
    def andOrLogic(self, type: str):
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Store in D
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("D=M\n")
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Store in A
        self.file.write("A=M\n")
        self.file.write("A=M\n")
        # D=D|A or D=D&A
        if type == "and":
            self.file.write("D=D&A\n")
        else:
            self.file.write("D=D|A\n")
        # Store in SP
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("M=D\n")
        # SP++
        self.file.write("@SP\n")
        self.file.write("M=M+1\n")

    # Write to output logically equivalent push/pop command.
    def writePushPop(self, command:str, segment: str, index: int) -> None:
        match segment:
                case "local":
                    self.lattAddr("LCL", index)
                case "argument":
                    self.lattAddr("ARG", index)
                case "this":
                    self.lattAddr("THIS", index)
                case "that":
                    self.lattAddr("THAT", index)
                case "constant":
                    if command == "pop":
                        print(f"Incorrect line, cannot pop a constant: {command} {segment} {index}")
                        exit()
                    self.pushConstant(index)
                    return
                case "static":
                    self.constantAddr(index)
                case "temp":
                    if index > 7:
                        print(f"Out of bounds temp segment. There are only 8 temp segments (starts at temp 0): {command} {segment} {index}")
                        exit() 
                    self.tempAddr(index)
                case "pointer":
                    if index != 0 and index != 1:
                        print(f"Incorrect command, index must be 0 or 1 when referring to pointer segment: {command} {segment} {index}")
                        exit() 
                    # pointer 0 = THIS, pointer 1 = THAT
                    if index == 0:
                        pointer = "THIS"
                    elif index == 1:
                        pointer = "THAT"
                    else:
                        print(f"Incorrect command, index must be 0 or 1 when referring to pointer segment: {command} {segment} {index}")
                        exit() 

                    if command == "push":
                        self.pushPointer(pointer)
                    elif command == "pop":
                        self.popPointer(pointer)
                    else:
                        print(f"Incorrect command, must push or pop on pointer segment: {command} {segment} {index}")
                        exit() 
                    return
            
                case _:
                    print(f"Incorrect line, wrong segment: {command} {segment} {index}")
                    exit() 

        if command == "push":
            self.pushValue()

        elif command == "pop":
            self.popValue()

        else:
            print(f"Incorrect line, should be push or pop command: {command} {segment} {index}")
            exit()   
    
    def lattAddr(self, translated_segment: str, index: int):
        self.file.write(f"@{translated_segment}\n")
        self.file.write("D=M\n")
        self.file.write(f"@{index}\n")
        self.file.write("D=D+A\n")
        self.file.write("@addr\n")
        self.file.write("M=D\n")
    
    def pushConstant(self, value: int): # Simply pushes constant
        # *SP = i
        if value == -1:
            self.file.write("A=-1\n")
        else:
            self.file.write(f"@{value}\n")
        self.file.write("D=A\n")
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("M=D\n")
        # SP++
        self.file.write("@SP\n")
        self.file.write("M=M+1\n")
    
    def constantAddr(self, index: int): # Puts location of constant into addr
        self.file.write(f"@{self.file_strict}.{index}\n")
        self.file.write("D=A\n")
        self.file.write("@addr\n")
        self.file.write("M=D\n")
    
    def tempAddr(self, index: int):
        self.file.write(f"@{index + 5}\n")
        self.file.write("D=A\n")
        self.file.write("@addr\n")
        self.file.write("M=D\n")
    
    def pushPointer(self, pointer: str):
        # *SP = THIS/THAT
        self.file.write(f"@{pointer}\n")
        self.file.write("D=M\n")
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("M=D\n")
        # SP++
        self.file.write("@SP\n")
        self.file.write("M=M+1\n")
    
    def popPointer(self, pointer: str):
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # THIS/THAT = *SP
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("D=M\n")
        self.file.write(f"@{pointer}\n")
        self.file.write("M=D\n")

    # Pushes value at location @addr onto stack
    def pushValue(self):
        # Pushes value on stack
        self.file.write("@addr\n")
        self.file.write("A=M\n")
        self.file.write("D=M\n")
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("M=D\n")
        # SP++
        self.file.write("@SP\n")
        self.file.write("M=M+1\n")

    # Pops value into location @addr
    def popValue(self):
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Store value from stack in @addr
        self.file.write("@SP\n")
        self.file.write("A=M\n") 
        self.file.write("D=M\n") 
        self.file.write("@addr\n")
        self.file.write("A=M\n")
        self.file.write("M=D\n")

    def setFileName(self, fileName: str):
        pass

    def writeInit(self):
        # SP = 256
        self.file.write("@256\n")
        self.file.write("D=A\n")
        self.file.write("@SP\n")
        self.file.write("M=D\n")
        # Call Sys.init
        self.writeCall("Sys.init", 0)

    def writeLabel(self, label: str):
        self.file.write(f"({label})\n")

    def writeGoto(self, label: str):
        self.file.write(f"@{label}\n")
        self.file.write("0;JMP\n")

    def writeIf(self, label: str):
        # SP--
        self.file.write("@SP\n")
        self.file.write("M=M-1\n")
        # Place value in D
        self.file.write("A=M\n")
        self.file.write("D=M\n")
        # Jump if equal
        self.file.write(f"@{label}\n")
        self.file.write("D;JGT\n")

    def writeFunction(self, functionName: str, numVars: int):
        # (fileName.functionName)
        self.file.write(f"@{self.file_strict}.{functionName}\n")
        self.current_function = functionName
        # repeat nVars times: push 0
        for i in range(numVars):
            self.pushConstant(0)

    def writeCall(self, functionName: str, numArgs: int):
        # push returnAddress // (using label declared below)
        self.file.write(f"@{self.translateReturnName(functionName)}\n")
        self.file.write("D=A\n")
        self.file.write("@SP\n")
        self.file.write("A=M\n")
        self.file.write("D=M\n")
        self.file.write("@SP\n")
        self.file.write("M=M+1\n")

        # push LCL, ARG, THIS and then THAT
        for i in ["LCL", "ARG", "THIS", "THAT"]:
            self.file.write(f"@{i}\n")
            self.file.write("D=M\n")
            self.file.write("@addr\n")
            self.file.write("M=D\n")
            self.pushValue()

        # ARG = SP - 5 - nArgs
        self.file.write("@SP\n")
        self.file.write("D=M\n")
        self.file.write("@5\n")
        self.file.write("D=D-A\n")
        self.file.write(f"@{numArgs}\n")
        self.file.write("D=D-A\n")
        self.file.write("@ARG\n")
        self.file.write("M=D\n")

        # LCL = SP
        self.file.write("@SP\n")
        self.file.write("D=M\n")
        self.file.write("@LCL\n")
        self.file.write("M=D\n")

        # goto functionName
        self.writeGoto(functionName)

        # (returnAddress)
        self.file.write(f"({self.translateReturnName(functionName)})\n")

        pass

    def writeReturn(self):
        # endFrame = LCL // endframe is a temp var
        self.file.write("@LCL\n")
        self.file.write("D=M\n")
        self.file.write("@endFrame\n")
        self.file.write("M=D\n")

        # retAddr = *(endFrame - 5) // get return address
        self.file.write("@endFrame\n")
        self.file.write("D=M\n")
        self.file.write("@5\n")
        self.file.write("D=D-A\n")
        self.file.write("@retAddr\n")
        self.file.write("M=D\n")

        # *ARG = pop() // repositions the return value for the caller, aka arg 0 = top value
        self.writePushPop("pop", "argument", 0)

        # SP = ARG + 1 // repositions SP
        self.file.write("@ARG\n")
        self.file.write("D=M\n")
        self.file.write("@SP\n")
        self.file.write("M=D\n")
        self.file.write("M=M+1\n")

        # Restores THAT, THIS, ARG and finally LCL
        for i in [("THAT", 1), ("THIS", 2), ("ARG", 3), ("LCL", 4)]:
            # i[0] = *(endFrame - i[1]) // restores THAT
            self.file.write("@endFrame\n")
            self.file.write("D=M\n")
            self.file.write(f"@{i[1]}\n")
            self.file.write("D=D-A\n")
            self.file.write(f"@{i[0]}\n")
            self.file.write("M=D\n")

        # goto retAddr // return address
        self.file.write("@retAddr\n")
        self.file.write("0;JMP\n")
    
    def translateLabel(self, label: str):
        # Xxx.foo$bar where Xxx = VM file name, foo = function name, bar = label
        return "{Xxx}.{foo}${bar}".format(Xxx = self.file_strict, foo = self.current_function, bar = label)
    
    def translateReturnName(self, functionName: str):
        # Xxx.foo$ret.i where Xxx = VM file name, foo = function name, i = running tally
        self.call += 1
        return "{Xxx}.{foo}$ret.{i}".format(Xxx = self.file_strict, foo = functionName, i = self.call)
        

def main():
    parser = Parser("in/" + sys.argv[1] + ".vm")
    writer = CodeWriter("out/" + sys.argv[1] + ".asm", sys.argv[1])
    
    # Runs until file ends
    while not parser.advance() and not parser.iteration_ended:
        print(parser.current_command, parser.current_command_arg1, parser.current_command_arg2, parser.current_command_type)
        if parser.current_command == "":
            # print(parser.current_command)
            # print(parser.current_command_arg1)
            # print(parser.current_command_arg2, "\n")
            pass
        elif parser.current_command_type == CommandType.C_ARITHMETIC:
            writer.writeArithmetic(parser.current_command)
        elif parser.current_command_type == CommandType.C_PUSH or parser.current_command_type == CommandType.C_POP:
            writer.writePushPop(parser.current_command, parser.current_command_arg1, parser.current_command_arg2)
        elif parser.current_command_type == CommandType.C_LABEL:
            writer.writeLabel(writer.translateLabel(parser.current_command_arg1))
        elif parser.current_command_type == CommandType.C_GOTO:
            writer.writeGoto(writer.translateLabel(parser.current_command_arg1))
        elif parser.current_command_type == CommandType.C_IF:
            writer.writeIf(writer.translateLabel(parser.current_command_arg1))
        elif parser.current_command_type == CommandType.C_FUNCTION:
            writer.writeFunction(parser.current_command_arg1, parser.current_command_arg2)
        elif parser.current_command_type == CommandType.C_RETURN:
            writer.writeReturn()
        elif parser.current_command_type == CommandType.C_CALL:
            writer.writeCall(parser.current_command_arg1, parser.current_command_arg2)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("USAGE: py vm-translator filename")
    else:
        main()
