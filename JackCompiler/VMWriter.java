import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class VMWriter {
    private BufferedWriter writer;
    public int spaceCount = 0;

    // Creates a new .vm output file.
    public VMWriter(File write) throws Exception {
        writer = new BufferedWriter(new FileWriter(write));
    }

    void writeLine(String line) throws Exception {
		for (int i = 0; i < spaceCount; i++) {
			writer.write("  ");
			// System.out.printf("  ");
		}

		// System.err.println(line);
		writer.write(line); writer.newLine();
		writer.flush();
	}

	// - writePush (CONST|ARG|LOCAL|STATIC|THIS|THAT|POINTER|TEMP segment, int index): Writes a VM push command.
	// - writePop (CONST|ARG|LOCAL|STATIC|THIS|THAT|POINTER|TEMP segment, int index): Writes a VM pop command.
	// - writeArithmetic (ADD|SUG|NEG|EQ|GT|LT|AND|OR|NOT command): Writes a VM arithmetic-logical command.
	// - writeLabel (String label): Writes a VM label command.
	// - writeGoto (String label): Writes a VM goto command.
	// - writeIf (String label): Writes a VM if-goto command.
	// - writeCall (String name, nArgs int): Writes a VM call command.
	// - writeFunction (String name, nLocals int): Writes a VM function command.
	// - writeReturn (): Writes a VM return command.
	// - close: Closes the output file.
    public void close() throws Exception {
        writer.close();
    }
}
