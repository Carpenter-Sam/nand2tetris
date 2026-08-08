import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class VMWriter {
    private BufferedWriter writer;
    public int spaceCount = 0;
    private String className = "";
    private int index = 0;

    // Creates a new .vm or XML output file.
    public VMWriter(File write) throws Exception {
        writer = new BufferedWriter(new FileWriter(write));
    }

    public void writeXMLLine(String line) throws Exception {
		for (int i = 0; i < spaceCount; i++) {
			writer.write("  ");
			// System.out.printf("  ");
		}

		// System.err.println(line);
		writer.write(line); writer.newLine();
		writer.flush();
    }

    public void setClassName(String name) {
        className = name;
    }

	// - writePush (CONST|ARG|LOCAL|STATIC|THIS|THAT|POINTER|TEMP segment, int index): Writes a VM push command.
    public void writePush(SegmentType segment, int index) throws Exception {
        writer.write(String.format("    push %s %d\n", segment.name().toLowerCase(), index));
    }

	// - writePop (CONST|ARG|LOCAL|STATIC|THIS|THAT|POINTER|TEMP segment, int index): Writes a VM pop command.
    public void writePop(SegmentType segment, int index) throws Exception {
        writer.write(String.format("    pop %s %d\n", segment.name().toLowerCase(), index));
    }

	// - writeArithmetic (ADD|SUG|NEG|EQ|GT|LT|AND|OR|NOT command): Writes a VM arithmetic-logical command.
    public void writeArithmetic(String command) throws Exception {
        writer.write(String.format("    %s\n", command));
    }

	// Writes a VM label command.
    // Each class has a running index for each label so LoopLabel -> ClassName_i where i is an index starting at 0.
    public void writeLabel() throws Exception {
        writer.write(String.format("label %s_%d\n", className, index++));
    }

	// Writes a VM goto command.
    public void writeGoto(String label) throws Exception {
        writer.write(String.format("    goto %s\n", label));
    }

	// Writes a VM if-goto command.
    public void writeIf(String label) throws Exception {
        writer.write(String.format("    if-goto %s\n", label));
    }

	// Writes a VM call command.
    // name.functionName() -> type.functionName()
    public void writeCall(String name, int nArgs) throws Exception {
        writer.write(String.format("    call %s %d\n", name, nArgs));
    }

	// Writes a VM function command.
    // functionName -> className.functionName
    public void writeFunction(String name, int nLocals) throws Exception {
        writer.write(String.format("function %s %d\n", name, nLocals));
    } 

	// Writes a VM return command.
    public void writeReturn() throws Exception {
        writer.write(String.format("    return\n"));
    }

	// Closes the output file.
    public void close() throws Exception {
        writer.close();
    }
}
