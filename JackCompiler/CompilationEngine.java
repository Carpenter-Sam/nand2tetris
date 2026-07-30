import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;

class CompilationEngine {
	private BufferedReader reader;
	private BufferedWriter writer;
	int lineNumber = 0;

	// Creates a new compilation engine with the given input and output.
	// The next routine called must be compileClass.
	public CompilationEngine(File read, File write) throws Exception {
		try {
			reader = new BufferedReader(new FileReader(read));
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			throw new Exception("ERROR: CompilationEngine cannot find intermediate file: " + read);
		}

		try {
			writer = new BufferedWriter(new FileWriter(write));
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			throw new Exception("ERROR: CompilationEngine cannot write to file: " + read);
		}

		// Check if file starts correctly.
		lineNumber++;
		if (!reader.readLine().equals("<tokens>")) {
			throw new Exception("ERROR: CompilationEngine file input expected to start with '<tokens>'.");
		}

		compileClass();

		// Check if file ends correctly.
		lineNumber++;
		if (!reader.readLine().equals("<tokens>")) {
			throw new Exception("ERROR: CompilationEngine  file input expected to start with '<tokens>'.");
		}
	}
	
	// Compiles a complete class.
	void compileClass() throws Exception {
		expect("class", TokenType.keyword);
	}
	
	// Compiles a static variable declaration, of a field declaration.
	// compileClassVarDec()
	
	// Compiles a complete method, function, or a constructor.
	// compileSubroutineDec()
	
	// Compiles a (possible empty) parameter list.
	// Does not handle the enclosing '()'.
	// compileParameterList()
	
	// Compiles a subroutine's body.
	// compileSubroutineBody()
	
	// Compiles a var declaration.
	// compileVarDec()
	
	// Compiles a sequence of statements.
	// Does not handle the enclosing '{}'.
	// compileStatements() {
	// 	// code for compiling statements
	// 	// Uses a loop to handle 0 or more statment instances, according to the left-most token.
	// 	// If left-most token is 'if' then 'compileIfStatement' is called.
	// }
	
	// Compiles a let statement.
	// compileLet()
	
	// Compiles an if statment, possibly with a trailing else clause.
	// compileIfStatement() {
	// 	// code for compiling an if statement
	// }
	
	// Compiles a while statement.
	// compileWhileStatement() {
	// 	// code for compiling a while statement
	// 	eat('while');  code to handle 'while';
	// 	eat('('); code to handle '(';
	// 	compileExpression();
	// 	eat(')'); code to handle ')';
	// 	...
	// }
	
	// Compiles a do statement.
	// compileDo()
	
	// Compiles a return statement.
	// compileReturn()
	
	// Compiles an expression.
	// compileExpression()
	
	// Compiles a term.
	// If the current token is an identifier, the routine must distinguish between a variable, an array entry, or a subroutine call.
	// As single look-ahead token, which may be one of '[', '(', or '.', suffices to distinguish between possibilities.
	// Any other token is not part of this term and should not be advanced over.
	// compileTerm() {
			// code for compiling a term
			// When the current token is a varName(some identifier), it can either be a variable name, an array entry of a rubroutine call.
	// }
	
	// Compiles a (possible empty) comma-separated list of expressions.
	// compileExpressionList()

	// eat(string) {
	// if (currentToken <> string)
	// 	error...
	// else
	// 	advance...
	// }

	private String expect(String expectedToken, TokenType expectedTokenType) throws Exception {
		lineNumber++;
		String line;
		try {
			line = reader.readLine(); 
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			throw new Exception("ERROR: Compilation Engine unable to read next line of file.");
		}

		if (line.isEmpty()) {
			throw new Exception(String.format("ERROR: Compilation Engine got back empty line when expecting '%s' at line %d.",
								expectedToken, lineNumber));
		} else if (line.equals("-1")) {
			throw new Exception(String.format("ERROR: Compilation Engine file input ended early when expecting '%s' at line %d.",
								expectedToken, lineNumber));
		}
		// throw new Exception(String.format("ERROR: Compilation Engine expected '%s' at line %d.",
		// 					expectedToken, lineNumber));
		
		try {
			splitLine(line);
		} catch (Exception e) {
			System.err.println(e);
			throw new Exception(String.format("ERROR: Compilation Engine error occured when splitting line expecting '%s' at line %d.",
								expectedToken, lineNumber));
		}
		
	

		return "";
	}

	public String[] splitLine(String line) throws Exception {
		int endOfElement1 = -1;
		int startOfToken = -1;
		int endOfToken = -1;
		int startOfElement2 = -1;
		int endOfElement2 = -1;

		int lineIter = 0;
		int lineEnd = line.length();
		
		// Check the line is of minimum appropriate length.
		if (lineEnd < 10) {
			throw new Exception("ERROR: Compilation Engine expected longer line in format '<e> t </e>'.");
		}

		// Need to check the following format <element> token </element>
		
		// Check for starting <
		if (line.charAt(lineIter++) != '<') {
			throw new Exception("ERROR: Compilation Engine expected line to start with '<'.");
		} 

		// Check for closing > or end of line
		while (lineIter != lineEnd && line.charAt(lineIter) != '>') {lineIter++;}
		// Check if end of line
		if (lineIter == lineEnd) {
			throw new Exception("ERROR: Compilation Engine expected longer line in format '<e> t </e>'.");
		// If not end of line, it means that the current lineIter is pointing at the closing >.
		} else {
			endOfElement1 = lineEnd++ - 1;
		}

		// Check for space after first set of angle brackets
		if (line.charAt(lineIter++) != ' ') {
			throw new Exception("ERROR: Compilation Engine expected space after first set of angle brackets, in the line.");
		} else {
			startOfToken = lineIter;
		}

		// Check for closing ending space or end of line
		while (lineIter != lineEnd && line.charAt(lineIter) != ' ') {lineIter++;}
		// Check if end of line
		if (lineIter == lineEnd) {
			throw new Exception("ERROR: Compilation Engine expected longer line in format '<e> t </e>'.");
		// If not end of line, it means that the current lineIter is pointing at the closing space.
		} else {
			endOfToken = lineEnd++ - 1;
		}

		// Check for opening angle bracket and then slash after token.
		if (line.charAt(lineIter++) != '<') {
			throw new Exception("ERROR: Compilation Engine expected open bracket after token, in the line.");
		} else if (line.charAt(lineIter++) != '/') {
			throw new Exception("ERROR: Compilation Engine expected slash after open bracket, in the line.");
		} else {
			startOfElement2 = lineIter;
		}

		// Check for closing > or end of line
		while (lineIter != lineEnd && line.charAt(lineIter) != '>') {lineIter++;}
		// Check if end of line
		if (lineIter == lineEnd) {
			throw new Exception("ERROR: Compilation Engine expected longer line in format '<e> t </e>'.");
		// If not end of line, it means that the current lineIter is pointing at the closing >.
		} else {
			endOfElement2 = lineEnd++ - 1;
		}

		// Add check to ensure nothing after final angle bracket.

		// Add support for single-line comments optionally.

		// Optionally, add logic checks for start/end of positions (i.e., element1 doesn't end after element 2 starts).

		// Return string array.
		String []splitStrings = new String[3];

		// Element 1
		splitStrings[0] = line.substring(0, endOfElement1);

		// Token
		splitStrings[1] = line.substring(startOfToken, endOfToken);

		// Element 2
		splitStrings[2] = line.substring(startOfElement2, endOfElement2);

		return splitStrings;

	}
}