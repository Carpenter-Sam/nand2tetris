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
	private String previousTokenType;
	private String previousLine;
	private String errorMsg = "";
	
	// private String[] keywords = {"class", "constructor", "function", "method",
	// 		 					 "field", "static", "var", "int", "char", "boolean",
	// 		 					 "void", "true", "false", "null", "this", "let", "do",
	// 							 "if", "else", "while", "return"};
	

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
		if (expect(new String[]{"class"}, new TokenType[]{TokenType.keyword}, true)) {
			writeLine("<keyword> class </keyword>");
		}
		
		// className 
		if (expect(new String[]{"className"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<identifier> %s </identifier>", previousToken));
		} 

		// '{' 
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> { </symbol>");
		}

		// classVarDec* 
		while (compileClassVarDec(false)) {};

		// subroutineDec* 
		while (compileSubroutineDec(false)) {};

		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> { </symbol>");
		}

		spaceCount--;
		writeLine("</class>");
	}
	
	// Compiles a static variable declaration, of a field declaration.
	// classVarDec: ('static'|'field') type varName (',' varName)* ';'
	private boolean compileClassVarDec(boolean allowedToFail) throws Exception {
		// ('static'|'field')
		if (!expect(new String[]{"static", "field"}, new TokenType[]{TokenType.keyword, TokenType.keyword}, allowedToFail)) {
			usePreviousLine = true;
			return false;
		} else {
			writeLine("<classVarDec>");
			spaceCount++;
			writeLine(String.format("<keyword> %s </keyword>", previousToken));
		}

		// type 
		String[] expectedTokens1 = {"int", "char", "boolean", "className"};
		TokenType[] expectedTokenTypes1 = {TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier};
		if (expect(expectedTokens1, expectedTokenTypes1, true)) {
			writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
		}
		

		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<identifier> %s </identifier>", previousToken));
		} 

		// (',' varName)* 
		while (true) {
			// ','
			if (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {
				writeLine("<symbol> , </symbol>");
			} else {
				usePreviousLine = true;
				break;
			}

			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				writeLine(String.format("<identifier> %s </identifier>", previousToken));
			}
		} 

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("</classVarDec>");
		return true;
	}
	
	// Compiles a complete method, function, or a constructor.
	private boolean compileSubroutineDec(boolean allowedToFail) throws Exception {

		// ('constructor'|'function'|'method')
		if (!expect(new String[]{"constructor", "function", "method"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword}, allowedToFail)) {
			usePreviousLine = true;
			return false;
		} else {
			writeLine("<subroutineDec>");
			spaceCount++;
			writeLine(String.format("<keyword> %s </keyword>", previousToken));
		}
		
		// ('void'|type) 
		if (expect(new String[]{"void", "int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
			writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
		}
		

		// subroutineName 
		if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<identifier> %s </identifier>", previousToken));
		} 

		// '(' 
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ( </symbol>");
		}

		// parameterList 
		compileParameterList();

		// ')' 
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ) </symbol>");
		}

		compileSubroutineBody();

		spaceCount--;
		writeLine("</subroutineDec>");

		return true;
	}
	
	// Compiles a (possible empty) parameter list.
	// Does not handle the enclosing '()'.
	private void compileParameterList() throws Exception {
		writeLine("<parameterList>");
		spaceCount++;

		// ( (type varName) (',' type varName)*)?
		// type
		if (expect(new String[]{"int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, false)) {
			writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
		
			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				writeLine(String.format("<identifier> %s </identifier>", previousToken));
			} 

			// (',' type varName)*	
			while (true) {
				// ','
				if (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {
					writeLine("<symbol> , </symbol>");
				} else {
					usePreviousLine = true;
					break;
				}

				// type
				if (expect(new String[]{"int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
					writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
				}

				// varName
				if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
					writeLine(String.format("<identifier> %s </identifier>", previousToken));
				}
			} 
		} else {
			usePreviousLine = true;
		}

		spaceCount--;
		writeLine("</parameterList>");
	}
	
	// Compiles a subroutine's body.
	private void compileSubroutineBody() throws Exception {
		writeLine("<subroutineBody>");
		spaceCount++;

		// '{' 
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> { </symbol>");
		}

		// varDec* 
		while(compileVarDec()) {}

		// statements
		writeLine("<statements>");
		spaceCount++;
		compileStatements();
		spaceCount--;
		writeLine("</statements>");
		
		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> } </symbol>");
		}

		spaceCount--;
		writeLine("</subroutineBody>");
	}
	
	// Compiles a var declaration.
	private boolean compileVarDec() throws Exception {
		// 'var' 
		if (!expect(new String[]{"var"}, new TokenType[]{TokenType.keyword}, false)) {
			usePreviousLine = true;
			return false;
		} else {
			writeLine("<varDec>");
			spaceCount++;
			writeLine(String.format("<keyword> %s </keyword>", previousToken));
		}

		// type 
		if (expect(new String[]{"int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
			writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
		}

		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<identifier> %s </identifier>", previousToken));
		}

		// (',' varName)* 
		while (true) {
			// ','
			if (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {
				writeLine("<symbol> , </symbol>");
			} else {
				usePreviousLine = true;
				break;
			}

			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				writeLine(String.format("<identifier> %s </identifier>", previousToken));
			}
		} 

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("</varDec>");
		return true;
	}
	
	// Compiles a sequence of statements.
	// Does not handle the enclosing '{}'.
	private void compileStatements() throws Exception {
		// code for compiling statements
		// Uses a loop to handle 0 or more statment instances, according to the left-most token.
		// If left-most token is 'if' then 'compileIfStatement' is called.
		
		// statement*
		// statement: letStatement | ifStatement | whileStatement | doStatement | returnStatement
		if (expect(new String[]{"let", "if", "while", "do", "return"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword}, false)) {
			writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
			
			switch(previousToken){
				case "let":
					writeLine("<letStatement>");
					spaceCount++;
					compileLet();;
					break;

				case "if":
					writeLine("<ifStatement>");
					spaceCount++;
					compileIfStatement();
					break;

				case "while":
					writeLine("<whileStatement>");
					spaceCount++;
					compileWhileStatement();
					break;

				case "do":
					writeLine("<doStatement>");
					spaceCount++;
					compileDo();
					break;

				case "return":
					writeLine("<returnStatement>");
					spaceCount++;
					compileReturn();
					break;

				case "_":
					throw new Exception("ERROR: Unusual compileStatements ending in the default statements switch case.");
			}
			 
		} else {
			usePreviousLine = true;
		}
	}
	
	// Compiles a let statement.
	private void compileLet() throws Exception {
		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<identifier> %s </identifier>", previousToken));
		}

		// ('['expression']')? 
		// '('
		if (expect(new String[]{"["}, new TokenType[]{TokenType.symbol}, false)) {
			writeLine("<symbol> [ </symbol>");

			// expression
			compileExpression(false);

			// ']'
			if (expect(new String[]{"]"}, new TokenType[]{TokenType.symbol}, true)) {
				writeLine("<symbol> ] </symbol>");
			}
		} else {
			usePreviousLine = true;
		}

		// '=' 
		if (expect(new String[]{"="}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> = </symbol>");
		}

		// expression
		compileExpression(false);

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("<letStatement>");
	}
	
	// Compiles an if statment, possibly with a trailing else clause.
	private void compileIfStatement() throws Exception {
		// code for compiling an if statement
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ( </symbol>");
		}

		// expression 
		compileExpression(false);

		// ')' 
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ) </symbol>");
		}

		// '{'
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> { </symbol>");
		}

		// statements
		writeLine("<statements>");
		spaceCount++;
		compileStatements();
		spaceCount--;
		writeLine("</statements>");

		// '}' 
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> } </symbol>");
		}

		// ('else' '{' statements '}')?
		// 'else'
		if (expect(new String[]{"else"}, new TokenType[]{TokenType.keyword}, false)) {
			writeLine("<keyword> else </keyword>");

			// '{' 
			if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
				writeLine("<symbol> { </symbol>");
			}

			// statements
			writeLine("<statements>");
			spaceCount++;
			compileStatements();
			spaceCount--;
			writeLine("</statements>");

			// '}'
			if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
				writeLine("<symbol> } </symbol>");
			}

		} else {
			usePreviousLine = true;
		}

		spaceCount--;
		writeLine("<ifStatement>");
	}
	
	// Compiles a while statement.
	private void compileWhileStatement() throws Exception {
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ( </symbol>");
		}

		// expression
		compileExpression(false);

		// ')' 
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ) </symbol>");
		}

		// '{'
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> { </symbol>");
		}

		// statements
		writeLine("<statements>");
		spaceCount++;
		compileStatements();
		spaceCount--;
		writeLine("</statements>");

		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> } </symbol>");
		}

		spaceCount--;
		writeLine("<whileStatement>");
	}
	
	// Compiles a do statement.
	private void compileDo() throws Exception {
		// subroutineCall 
		// TODO 

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("<doStatement>");
	}
	
	// Compiles a return statement.
	private void compileReturn() throws Exception {
		// expression?
		compileExpression(false);

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("<returnStatement>");
	}
	
	// Compiles an expression.
	private boolean compileExpression(boolean allowedToFail) throws Exception {
		writeLine("<expression>");
		spaceCount++;

		

		spaceCount--;
		writeLine("</expression>");
		return true;
	}
	
	// Compiles a term.
	// If the current token is an identifier, the routine must distinguish between a variable, an array entry, or a subroutine call.
	// As single look-ahead token, which may be one of '[', '(', or '.', suffices to distinguish between possibilities.
	// Any other token is not part of this term and should not be advanced over.
	// compileTerm() throws Exception {
			// code for compiling a term
			// When the current token is a varName(some identifier), it can either be a variable name, an array entry of a rubroutine call.
	// }
	
	// Compiles a (possible empty) comma-separated list of expressions.
	// compileExpressionList() throws Exception 

	// If not told to use the previous line, then expect reads a new line of the .Txml and stores it in case of later use.
	// Expect checks for errors then checks to see if the token is valid, returning an error message if it isn't.
	// If it is valid then it returns a boolean and a potential identifier.
	private boolean expect(String []expectedTokens, TokenType []expectedTokenType, boolean fails) throws Exception {
		String line;
		errorMsg = "";

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
		
		// Check that the two elements are of the same type
		if (!splitStrings[0].equals(splitStrings[2])) {
			throw new Exception(String.format("ERROR: Compilation Engine expected elements of equal types on line %d but instead got types '%s' and '%s'.",
											  lineNumber, splitStrings[0], splitStrings[2]));
		}
		
		// Check if any of the valid tokens match the string
		boolean validToken = false;
		int iter = 0;
		while (iter < expectedTokens.length) {
			validToken = check(expectedTokens[iter], expectedTokenType[iter], splitStrings);
			if (validToken) {break;}
			iter++;
		} 
		
		
		if (!validToken) {
			errorMsg = String.format("ERROR: Compilation Engine expected a token of '%s' on line %d but instead got '%s'.",
												Arrays.toString(expectedTokens), lineNumber, splitStrings[1]);
		}
		

		previousLine = line;
		previousTokenType = splitStrings[0];
		previousToken = splitStrings[1];
		if (fails && !validToken) {
			throw new Exception(errorMsg);
		} else {
			return validToken;
		}
	}

	private boolean check(String potentialToken, TokenType tokenType, String[] splitStrings) {
		// If types aren't equal then return false.
		if (!splitStrings[0].equals(tokenType.name())) {
			return false;
		// If tokens aren't equal and the type is not an identifier then return false.
		} else if (!splitStrings[1].equals(potentialToken) && tokenType != TokenType.identifier) {
			return false;
		} else {
			// Returns true if the types are equal and the tokens are equal.
			// Also returns true if the types are identifiers without checking token equality, since we won't know the identifier token in advance.
			return true;
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
}