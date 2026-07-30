import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;

class CompilationEngine {
	private BufferedReader reader;
	private BufferedWriter writer;
	int line_number = 0;

	// Creates a new compilation engine with the given input and output.
	// The next routine called must be compileClass.
	public CompilationEngine(File read, File write) throws Exception {
		try {
			reader = new BufferedReader(new FileReader(read));
		} catch (Exception e) {
			throw new Exception("ERROR: " + e + "\nERROR: CompilationEngine cannot find intermediate file: " + read);
		}
		

		try {
			writer = new BufferedWriter(new FileWriter(write));
		} catch (Exception e) {
			throw new Exception("ERROR: " + e + "\nERROR: CompilationEngine cannot write to file: " + read);
		}

		// Check if file starts correctly.
		line_number++;
		if (!reader.readLine().equals("<tokens>")) {
			throw new Exception("ERROR: CompilationEngine file input expected to start with '<tokens>'.");
		}

		compileClass();

		// Check if file ends correctly.
		line_number++;
		if (!reader.readLine().equals("<tokens>")) {
			throw new Exception("ERROR: CompilationEngine  file input expected to start with '<tokens>'.");
		}
	}
	
	// Compiles a complete class.
	void compileClass() {
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

	private String expect(String expectedToken, TokenType expectedTokenType) {

		return "";
	}
}