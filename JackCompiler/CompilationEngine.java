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
	private SymbolTable symbolTable;

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

		symbolTable = new SymbolTable();

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

		writer.close();
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
			writeLine(String.format("<className> %s </className>", previousToken));
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
			writeLine("<symbol> } </symbol>");
		}

		spaceCount--;
		writeLine("</class>");
	}
	
	// Compiles a static variable declaration, of a field declaration.
	// classVarDec: ('static'|'field') type varName (',' varName)* ';'
	private boolean compileClassVarDec(boolean allowedToFail) throws Exception {
		String kind;
		String type;

		// ('static'|'field')
		if (!expect(new String[]{"static", "field"}, new TokenType[]{TokenType.keyword, TokenType.keyword}, allowedToFail)) {
			usePreviousLine = true;
			return false;
		} else {
			writeLine("<classVarDec>");
			spaceCount++;
			writeLine(String.format("<keyword> %s </keyword>", previousToken));
		}
		kind = previousToken.toUpperCase(); // Store kind of variable so it can later be added to the SymbolTable.

		// type 
		String[] expectedTokens1 = {"int", "char", "boolean", "className"};
		TokenType[] expectedTokenTypes1 = {TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier};
		if (expect(expectedTokens1, expectedTokenTypes1, true)) {
			// Allows for 'className' to be written instead of 'identifier', for more accurate identifiers.
			String tokenTypeName = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				tokenTypeName = "className";
			}

			writeLine(String.format("<%s> %s </%s>", tokenTypeName, previousToken, tokenTypeName));
		}
		type = previousToken;
		

		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			symbolTable.define(previousToken, type, SymbolKind.valueOf(kind));

			writeLine(String.format("<%s> %s </%s> <!--Declaration. Index: %d -->", kind, previousToken, kind, symbolTable.indexOf(previousToken)));
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
				// varName will have the same type and kind as the first varName initialised in the loop.
				symbolTable.define(previousToken, type, SymbolKind.valueOf(kind));
				
				writeLine(String.format("<%s> %s </%s> <!--Declaration. Index: %d -->", kind, previousToken, kind, symbolTable.indexOf(previousToken)));
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
			symbolTable.startSubroutine(); // Clears subroutine symbol table, ready for the next subroutine.
			writeLine("<subroutineDec>");
			spaceCount++;
			writeLine(String.format("<keyword> %s </keyword>", previousToken));
		}
		
		// ('void'|type) 
		if (expect(new String[]{"void", "int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
			// Allows for 'className' to be written instead of 'identifier', for more accurate identifiers.
			String tokenTypeName = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				tokenTypeName = "className";
			}

			writeLine(String.format("<%s> %s </%s>", tokenTypeName, previousToken, tokenTypeName));
		}
		

		// subroutineName 
		if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<subroutineName> %s </subroutineName>", previousToken));
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
			String firstType = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				firstType = "className";
			}
			writeLine(String.format("<%s> %s </%s>", firstType, previousToken, firstType));
		
			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				symbolTable.define(previousToken, firstType, SymbolKind.ARG);

				writeLine(String.format("<%s> %s </%s> <!--Declaration+Initialisation. Index: %d -->", SymbolKind.ARG, previousToken, SymbolKind.ARG, symbolTable.indexOf(previousToken)));
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

				String secondTypes;
				// type
				if (expect(new String[]{"int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
					secondTypes = previousTokenType;
					if (previousTokenType == TokenType.identifier.name()) {
						secondTypes = "className";
					}

					writeLine(String.format("<%s> %s </%s>", secondTypes, previousToken, secondTypes));
				} 
				secondTypes = previousTokenType; // Redundant, but shuts the Java compiler up.

				// varName
				if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
					symbolTable.define(previousToken, secondTypes, SymbolKind.ARG);

					writeLine(String.format("<%s> %s </%s> <!--Declaration+Initialisation. Index: %d -->", SymbolKind.ARG, previousToken, SymbolKind.ARG, symbolTable.indexOf(previousToken)));
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
		// System.out.println("FAILED: " + previousLine + " " + usePreviousLine);

		usePreviousLine = true;
		
		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> } </symbol>");
		}

		spaceCount--;
		writeLine("</subroutineBody>");
	}
	
	// Compiles a var declaration.
	private boolean compileVarDec() throws Exception {
		String type;

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
			// Allows for 'className' to be written instead of 'identifier', for more accurate identifiers.
			String tokenTypeName = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				tokenTypeName = "className";
			}
			
			writeLine(String.format("<%s> %s </%s>", tokenTypeName, previousToken, tokenTypeName));
		}
		type = previousToken;

		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			symbolTable.define(previousToken, type, SymbolKind.VAR);

			writeLine(String.format("<%s> %s </%s> <!--Declaration. Index: %d -->", SymbolKind.VAR, previousToken, SymbolKind.VAR, symbolTable.indexOf(previousToken)));
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
				symbolTable.define(previousToken, type, SymbolKind.VAR);

				writeLine(String.format("<%s> %s </%s> <!--Declaration. Index: %d -->", SymbolKind.VAR, previousToken, SymbolKind.VAR, symbolTable.indexOf(previousToken)));
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
		boolean lastOneTrue = false;
		while (expect(new String[]{"let", "if", "while", "do", "return"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword}, false)) {
			lastOneTrue = true;

			switch(previousToken){
				case "let":
					writeLine("<letStatement>");
					spaceCount++;
					writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileLet();
					break;

				case "if":
					writeLine("<ifStatement>");
					spaceCount++;
					writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileIfStatement();
					break;

				case "while":
					writeLine("<whileStatement>");
					spaceCount++;
					writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileWhileStatement();
					break;

				case "do":
					writeLine("<doStatement>");
					spaceCount++;
					writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileDo();
					break;

				case "return":
					writeLine("<returnStatement>");
					spaceCount++;
					writeLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileReturn();
					break;

				case "_":
					throw new Exception("ERROR: Unusual compileStatements ending in the default statements switch case.");
			}
			 
		} 
		if (!lastOneTrue) {
			usePreviousLine = true;
		}
	}
	
	// Compiles a let statement.
	private void compileLet() throws Exception {
		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			writeLine(String.format("<%s> %s </%s> <!--Initialisation. Index: %d -->", 
					  symbolTable.kindOf(previousToken), previousToken, symbolTable.kindOf(previousToken), symbolTable.indexOf(previousToken)));
		}

		// ('['expression']')? 
		// '['
		if (expect(new String[]{"["}, new TokenType[]{TokenType.symbol}, false)) {
			writeLine("<symbol> [ </symbol>");

			// expression
			compileExpression(true);

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
		compileExpression(true);

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("</letStatement>");
	}
	
	// Compiles an if statment, possibly with a trailing else clause.
	private void compileIfStatement() throws Exception {
		// code for compiling an if statement
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ( </symbol>");
		}

		// expression 
		compileExpression(true);

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

		usePreviousLine = true;

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

			usePreviousLine = true;

			// '}'
			if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
				writeLine("<symbol> } </symbol>");
			}

		} else {
			usePreviousLine = true;
		}

		spaceCount--;
		writeLine("</ifStatement>");
	}
	
	// Compiles a while statement.
	private void compileWhileStatement() throws Exception {
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ( </symbol>");
		}

		// expression
		compileExpression(true);

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

		usePreviousLine = true;

		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> } </symbol>");
		}

		spaceCount--;
		writeLine("</whileStatement>");
	}
	
	// Compiles a do statement.
	private void compileDo() throws Exception {
		// subroutineCall: subroutineName '(' expressionList ')' | ( className | varName )' '.' subroutineName '(' expressionList ')'
		// identifier = subroutineName or ( className | varName )
		if (expect(new String[]{"varOrClassOrSubroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
			// Need to figure out if it's a variable.
			if (symbolTable.exists(previousToken)) {
				// Token is a variable.
				writeLine(String.format("<%s> %s </%s> <!--Usage. Type: %s, Index: %d -->", 
						  symbolTable.kindOf(previousToken), previousToken, symbolTable.kindOf(previousToken), symbolTable.typeOf(previousToken), symbolTable.indexOf(previousToken)));
			} else {
				writeLine(String.format("<classOrSubroutineName> %s </classOrSubroutineName>", previousToken));
			}
		}
		// then check if '.' or not
		if (expect(new String[]{"."}, new TokenType[]{TokenType.symbol}, false)) {
			// '.' subroutineName
			// '.'
			writeLine("<symbol> . </symbol>");

			// subroutineName
			if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
				writeLine(String.format("<subroutineName> %s </subroutineName>", previousToken));
			}
		} else {
			usePreviousLine = true;
		}
		// '(' expressionList ')'
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ( </symbol>");
		}

		// expressionList
		compileExpressionList();

		// ')'
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ) </symbol>");
		}
		
		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
			writeLine("<symbol> ; </symbol>");
		}

		spaceCount--;
		writeLine("</doStatement>");
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
		writeLine("</returnStatement>");
	}
	
	// Compiles an expression.
	private boolean compileExpression(boolean allowedToFail) throws Exception {
		// term
		boolean atLeastOneTerm = compileTerm(true, false);
		if ((!atLeastOneTerm) && allowedToFail) {
			throw new Exception(String.format("ERROR: Compilation engine expected expression on line %d.", lineNumber));
		} 
		
		// (op term)*
		while (true) {
			// 'op'
			if (expect(new String[]{"+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="}, new TokenType[]{TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol}, false)) {
				writeLine(String.format("<symbol> %s </symbol>", previousToken));
			} else {
				usePreviousLine = true;
				break;
			}

			// term
			compileTerm(false, true);
		} 

		if (atLeastOneTerm) {
			spaceCount--;
			writeLine("</expression>");	
			return true;
		} else {
			return false;
		}
	}
	
	// Compiles a term.
	// If the current token is an identifier, the routine must distinguish between a variable, an array entry, or a subroutine call.
	// As single look-ahead token, which may be one of '[', '(', or '.', suffices to distinguish between possibilities.
	// Any other token is not part of this term and should not be advanced over.
	private boolean compileTerm(boolean frontOfExpression, boolean allowedToFail) throws Exception {
		// code for compiling a term
		// When the current token is a varName(some identifier), it can either be a variable name, an array entry of a subroutine call.
	
		// if it's tokenType is symbol and that symbol is ')'|';' then it was an optional expression that needs to be returned from.
		if (expect(new String[]{")", ";"}, new TokenType[]{TokenType.symbol, TokenType.symbol}, false)) {
			usePreviousLine = true;
			return false;
		}
		usePreviousLine = true;

		if (frontOfExpression) {
			writeLine("<expression>");
			spaceCount++;
		}

		writeLine("<term>");
		spaceCount++;

		boolean validTerm = false;
		boolean doNotUsePreviousLine = false;
		// integerConstant 
		// tokenType is integerConstant
		if (previousTokenType.equals(TokenType.integerConstant.name())) {
			writeLine(String.format("<integerConstant> %s </integerConstant>", previousToken));
			validTerm = true;
			doNotUsePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		// stringConstant
		// tokenType is stringConstant
		if (!validTerm && previousTokenType.equals(TokenType.stringConstant.name())) {
			writeLine(String.format("<stringConstant> %s </stringConstant>", previousToken));
			validTerm = true;
			doNotUsePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		// keywordConstant 
		// tokenType is keyword of token 'true'|'false'|'null'|'this'
		if (!validTerm && expect(new String[]{"true", "false", "null", "this"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword}, false)) {
			writeLine(String.format("<keyword> %s </keyword>", previousToken)); // WARNING: Maybe supposed to be keywordConstant?
			validTerm = true;
			doNotUsePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		// '('expression')' 
		// tokenType is symbol of token '('
		if (!validTerm && expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, false)) {
			writeLine(String.format("<symbol> ( </symbol>")); 
			validTerm = true;

			// expression
			compileExpression(true);

			// ')' 
			if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
				writeLine("<symbol> ) </symbol>");
			}

			doNotUsePreviousLine = true;

		} else {
			usePreviousLine = true;
		}

		// (unaryOp term) 
		// tokenType is symbol of token '-'|'~'
		if (!validTerm && expect(new String[]{"-", "~"}, new TokenType[]{TokenType.symbol, TokenType.symbol}, false)) {
			writeLine(String.format("<symbol> %s </symbol>", previousToken));
			validTerm = true;

			// term
			compileTerm(false, true);
		} else {
			usePreviousLine = true;
		}

		if (!validTerm && expect(new String[]{"identifier"}, new TokenType[]{TokenType.identifier}, false)) {
			String varToken = previousToken;
			validTerm = true;

			// varName|subroutineName|className
			// Need to figure out if it's a variable.
			if (symbolTable.exists(varToken)) {
				// Token is a variable.
				writeLine(String.format("<%s> %s </%s> <!--Usage. Type: %s, Index: %d -->", 
						  symbolTable.kindOf(varToken), varToken, symbolTable.kindOf(varToken), symbolTable.typeOf(varToken), symbolTable.indexOf(varToken)));
			} else {
				writeLine(String.format("<classOrSubroutineName> %s </classOrSubroutineName>", varToken));
			}

			// varName'['expression']' 
			// tokenType of identifier followed by a symbol of token '['
			if (expect(new String[]{"["}, new TokenType[]{TokenType.symbol}, false)) { 
				writeLine("<symbol> [ </symbol>");

				// expression
				compileExpression(true);

				// ']' 
				if (expect(new String[]{"]"}, new TokenType[]{TokenType.symbol}, true)) {
					writeLine("<symbol> ] </symbol>");
				}

			
			// subroutineCall: subroutineName '(' expressionList ')' | ( className | varName)' '.' subroutineName '(' expressionList ')'
			// tokenType of identifier followed by symbol of token '(' or .
			} else if ((usePreviousLine = true) && expect(new String[]{"(", "."}, new TokenType[]{TokenType.symbol, TokenType.symbol}, false)) {
				// then check if '.' or not
				if (previousToken.equals(".")) {
					// '.' subroutineName
					// '.'
					writeLine("<symbol> . </symbol>");

					// subroutineName
					if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
						writeLine(String.format("<identifier> %s </identifier>", previousToken));
					}
				} else {
					usePreviousLine = true;
				}
				// '(' expressionList ')'
				// '('
				if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
					writeLine("<symbol> ( </symbol>");
				}
				
				// expressionList
				compileExpressionList();

				// ')'
				if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
					writeLine("<symbol> ) </symbol>");
				}

			// varName  (already written)
			// tokenType of identifier followed not by a symbol of token '['
			} else {
				usePreviousLine = true;
			}

		}

		if (validTerm) {
			spaceCount--;
			writeLine("</term>");
		}

		if (doNotUsePreviousLine) {
			usePreviousLine = false;
		}

		return validTerm;
	}
	
	// Compiles a (possible empty) comma-separated list of expressions.
	private void compileExpressionList() throws Exception {
		writeLine("<expressionList>");
		spaceCount++;
	
		// (expression (',' expression)* )?
		// expression 
		if (compileExpression(false)) {
			// (',' expression)*

			while (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {
				writeLine("<symbol> , </symbol>");

				compileExpression(true);
			}
			usePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		spaceCount--;
		writeLine("</expressionList>");
	}

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

		// System.out.println(Arrays.toString(splitStrings));
		
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

		// Check for next open bracket or end of line
		while (lineIter != lineEnd && line.charAt(lineIter) != '<') {lineIter++;}
		// Check if end of line
		if (lineIter == lineEnd) {
			throw new Exception("ERROR: Compilation Engine expected open angle bracket after token, in the line.");
		
		// If lineIter is currently pointing at the open angle bracket then the previous one should be a space.
		} else if (line.charAt(lineIter - 1) != ' ') {
			throw new Exception("ERROR: Compilation Engine expected space before open angle bracket after token, in the line.");
		} else {
			endOfToken = lineIter - 1;
		}

		// Check for slash and then slash after token.
		if (line.charAt(++lineIter) != '/') {
			throw new Exception("ERROR: Compilation Engine expected slash after open bracket, in the line.");
		} else {
			startOfElement2 = ++lineIter;
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
