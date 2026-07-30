import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.BufferedReader;

class CompilationEngine {
	private BufferedReader reader;
	private BufferedWriter writer;
	private int lineNumber = 0;
	private int spaceCount = 0;

	private boolean usePreviousLine = false;
	private String previousToken;
	private String previousLine;
	
	private String[] keywords = {"class", "constructor", "function", "method",
			 					 "field", "static", "var", "int", "char", "boolean",
			 					 "void", "true", "false", "null", "this", "let", "do",
								 "if", "else", "while", "return"};
	

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

		// splitLineTest();

		compileClass();

		// Check if file ends correctly.
		lineNumber++;
		if (!reader.readLine().equals("</tokens>")) {
			throw new Exception("ERROR: CompilationEngine  file input expected to end with '</tokens>'.");
		}
	}

	void writeLine(String line) throws Exception {
		for (int i = 0; i < spaceCount; i++) {
			writer.write(" ");
		}
		writer.write("line"); writer.newLine();
	}
	
	// Compiles a complete class.
	// class: 'class' className '{' classVarDec* subroutineDec* '}'
	void compileClass() throws Exception {
		writeLine("<class>");
		spaceCount++;

		// 'class'
		if (expect(new String[]{"class"}, TokenType.keyword, false, true)) {
			writeLine("<keyword> class </keyword>");
		}
		
		// className 
		if (expect(new String[]{"className"}, TokenType.identifier, false, true)) {
			writeLine(String.format("<identifier> %s </identifier>", previousToken));
		} 

		// '{' 
		if (expect(new String[]{"{"}, TokenType.symbol, false, true)) {
			writeLine("<keyword> { </keyword>");
		}

		// classVarDec* 
		while(compileClassVarDec(false));

		// subroutineDec* 

		// '}'
		if (expect(new String[]{"}"}, TokenType.symbol, false, true)) {
			writeLine("<keyword> { </keyword>");
		}

		spaceCount--;
		writeLine("</class>");
	}
	
	// Compiles a static variable declaration, of a field declaration.
	// classVarDec: ('static'|'field') type varName (',' varName)* ';'
	private boolean compileClassVarDec(boolean allowedToFail) throws Exception {
		// ('static'|'field')
		if (!expect(new String[]{"static", "field"}, TokenType.identifier, false, allowedToFail)) {
			usePreviousLine = true;
			return false;
		} else {
			writeLine("<classVarDec>");
			spaceCount++;
			writeLine(String.format("<keyword> %s </keyword>", previousToken));
		}

		// type 
		expect(new String[]{"int", "char", "boolean"}, TokenType.identifier, true, true);
		

		// varName 
		// (',' varName)* 
		// ';'

		spaceCount--;
		writeLine("</classVarDec>");
		return true;
	}
	
	// Compiles a complete method, function, or a constructor.
	// compileSubroutineDec() throws Exception 
	
	// Compiles a (possible empty) parameter list.
	// Does not handle the enclosing '()'.
	// compileParameterList() throws Exception 
	
	// Compiles a subroutine's body.
	// compileSubroutineBody() throws Exception 
	
	// Compiles a var declaration.
	// compileVarDec() throws Exception 
	
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

	// Reads the next line of the .Txml and checks expected token is there with expected token type.
	// private String expect(String []expectedTokens, TokenType expectedTokenType, char expectType) throws Exception {
	// 	String line;
	// 	potentiallyMore = false;

	// 	// Only read next line if there isn't a previous line that needs to be read.
	// 	if (stringStored) {
	// 		line = previousLine;
	// 		stringStored = false;
	// 	} else {
	// 		lineNumber++;
	// 		try {
	// 			line = reader.readLine(); 
	// 		} catch (Exception e) {
	// 			System.err.println("ERROR: " + e);
	// 			throw new Exception("ERROR: Compilation Engine unable to read next line of file.");
	// 		}

	// 		if (line.isEmpty()) {
	// 			throw new Exception(String.format("ERROR: Compilation Engine got back empty line when expecting '%s' at line %d.",
	// 								Arrays.toString(expectedTokens), lineNumber));
	// 		} else if (line.equals("-1")) {
	// 			throw new Exception(String.format("ERROR: Compilation Engine file input ended early when expecting '%s' at line %d.",
	// 								Arrays.toString(expectedTokens), lineNumber));
	// 		}
	// 	}
		
	// 	String[] splitStrings;
	// 	try {
	// 		splitStrings = splitLine(line);
	// 	} catch (Exception e) {
	// 		System.err.println(e);
	// 		throw new Exception(String.format("ERROR: Compilation Engine error occured when splitting line expecting '%s' at line %d.",
	// 							Arrays.toString(expectedTokens), lineNumber));
	// 	}

	// 	System.out.println(Arrays.toString(splitStrings));
		
		
	// 	// Check that the two elements are of the same type
	// 	if (!splitStrings[0].equals(splitStrings[2])) {
	// 		throw new Exception(String.format("ERROR: Compilation Engine expected elements of equal types on line %d but instead got types '%s' and '%s'.",
	// 										  lineNumber, splitStrings[0], splitStrings[2]));
	// 	}

	// 	// If one of these types, then a failed check doesn't mean a fail.
	// 	boolean returnPotentially = false;
	// 	boolean doNotFail = false;
	// 	if (expectType == '*' || expectType == '+') {
	// 		returnPotentially = true;
	// 		if (expectType == '?') {
	// 			doNotFail = true;
	// 		}
	// 	}
		
	// 	// Check if the two elements are equal to the expected type
	// 	if (!splitStrings[0].equals(expectedTokenType.name())) {
	// 		if (doNotFail) {
	// 			previousLine = line;
	// 			stringStored = true;
	// 			return "";
	// 		} else {
	// 			throw new Exception(String.format("ERROR: Compilation Engine expected element of type '%s' on line %d but instead got type '%s'.",
	// 										  expectedTokenType.name(), lineNumber, splitStrings[0]));
	// 		}
		
	// 	// If tokenType is an identifier then unknown identifier token, so no need to check if it equals expectedToken.
	// 	} else if (expectedTokenType == TokenType.identifier) {
	// 		if (returnPotentially) {
	// 			potentiallyMore = true;
	// 		}
	// 		return splitStrings[1];
	// 	}

	// 	// Check if the token is same as any expectedTokens
	// 	for (String token : expectedTokens) {
	// 		if (token.equals(splitStrings[1])) {
	// 			if (returnPotentially) {
	// 				potentiallyMore = true;
	// 			}
	// 			return splitStrings[1];
	// 		}
	// 	}
		
	// 	if (doNotFail) {
	// 			previousLine = line;
	// 			stringStored = true;
	// 			return "";
	// 	} else {
	// 	throw new Exception(String.format("ERROR: Compilation Engine expected token '%s' on line %d but instead got '%s'.",
	// 										Arrays.toString(expectedTokens), lineNumber, splitStrings[1]));
	// 	}
	// }

	// If not told to use the previous line, then expect reads a new line of the .Txml and stores it in case of later use.
	// Expect checks for errors then checks to see if the token is valid, returning an error message if it isn't.
	// If it is valid then it returns a boolean and a potential identifier.
	private boolean expect(String []expectedTokens, TokenType expectedTokenType, boolean bypassKeywordCheck, boolean fails) throws Exception {
		String line;

		// Only read next line if there isn't a previous line that needs to be read.
		if (usePreviousLine) {
			line = previousLine;
			usePreviousLine = false;
		} else {
			lineNumber++;
			try {
				line = reader.readLine(); 
			} catch (Exception e) {
				System.err.println("ERROR: " + e);
				throw new Exception("ERROR: Compilation Engine unable to read next line of file.");
			}

			if (line.isEmpty()) {
				throw new Exception(String.format("ERROR: Compilation Engine got back empty line when expecting '%s' at line %d.",
									Arrays.toString(expectedTokens), lineNumber));
			} else if (line.equals("-1")) {
				throw new Exception(String.format("ERROR: Compilation Engine file input ended early when expecting '%s' at line %d.",
									Arrays.toString(expectedTokens), lineNumber));
			}
		}
		
		String[] splitStrings;
		try {
			splitStrings = splitLine(line);
		} catch (Exception e) {
			System.err.println(e);
			throw new Exception(String.format("ERROR: Compilation Engine error occured when splitting line expecting '%s' at line %d.",
								Arrays.toString(expectedTokens), lineNumber));
		}

		System.out.println(Arrays.toString(splitStrings));
		
		String errorMsg = new String();
		boolean validToken = true;
		// Check that the two elements are of the same type
		if (!splitStrings[0].equals(splitStrings[2])) {
			validToken = false;
			errorMsg = String.format("ERROR: Compilation Engine expected elements of equal types on line %d but instead got types '%s' and '%s'.",
											  lineNumber, splitStrings[0], splitStrings[2]);
		}
		
		// Check if the two elements are equal to the expected type
		if (bypassKeywordCheck) {
			splitStrings[0] = "identifier";
			splitStrings[2] = "identifier";
		}
		if (!splitStrings[0].equals(expectedTokenType.name())) {
			validToken = false;
			errorMsg = String.format("ERROR: Compilation Engine expected element of type '%s' on line %d but instead got type '%s'.",
											  expectedTokenType.name(), lineNumber, splitStrings[0]);
		}
		
		// If identifier, then checks need to be made to see if it's a valid identifier or not.
		if (expectedTokenType == TokenType.identifier) {
			// We check if it's equal to a keyword and if it is then we check to see if it's a valid keyword or not.
			for (String keyword : keywords) {
				if (splitStrings[1].equals(keyword)) {
					validToken = isKeywordAllowed(keyword, expectedTokens);
					break;
				}
			}
		} else {
			validToken = false;
			// Check if the token is same as any expectedTokens
			for (String token : expectedTokens) {
				if (token.equals(splitStrings[1])) {
					validToken = true;
				}
			}
		}
		
		if (!validToken) {
			errorMsg = String.format("ERROR: Compilation Engine expected token '%s' on line %d but instead got '%s'.",
												Arrays.toString(expectedTokens), lineNumber, splitStrings[1]);
		}
		

		previousLine = line;
		previousToken = splitStrings[1];
		if (fails && !validToken) {
			throw new Exception(errorMsg);
		} else {
			return validToken;
		}
	}

	// WARNING: Inproper input validation! Assumes no errors currently.
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
			endOfElement1 = lineIter++;
		}

		// System.out.println("Reached: " + lineIter);
		// System.out.println(endOfElement1);

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
			endOfToken = lineIter++;
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
			endOfElement2 = lineIter++;
		}

		// Add check to ensure nothing after final angle bracket.

		// Add support for single-line comments optionally.

		// Optionally, add logic checks for start/end of positions (i.e., element1 doesn't end after element 2 starts).

		// Return string array.
		String []splitStrings = new String[3];

		// Element 1
		splitStrings[0] = line.substring(1, endOfElement1);

		// Token
		splitStrings[1] = line.substring(startOfToken, endOfToken);

		// Element 2
		splitStrings[2] = line.substring(startOfElement2, endOfElement2);

		return splitStrings;

	}

	
	private boolean isKeywordAllowed(String keywordToCheck, String[] allowedKeywords) {
		for (String keyword : allowedKeywords) {
			if (keyword.equals(keywordToCheck)) {
				return true;
			}
		}
		return false;
	}

	// private void splitLineTest() throws Exception {
	// 	System.out.println("Testing.");
	// 	System.out.println(Arrays.toString(splitLine("<h i d </h i>")));
	// }
}