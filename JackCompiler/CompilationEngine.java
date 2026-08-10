import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import javax.swing.text.Segment;

import java.io.BufferedReader;

class CompilationEngine {
	private BufferedReader reader;
	private VMWriter writer;
	private int lineNumber = 0;
	private SymbolTable symbolTable;

	private boolean usePreviousLine = false;
	private String previousToken;
	private String previousTokenType;
	private String previousLine;
	private String errorMsg = "";
	private String currentFunctionReturn = "";
	
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
			writer = new VMWriter(write);
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
	
	// Compiles a complete class.
	// class: 'class' className '{' classVarDec* subroutineDec* '}'
	void compileClass() throws Exception {
		writer.spaceCount++;

		// 'class'
		if (expect(new String[]{"class"}, new TokenType[]{TokenType.keyword}, true)) {}
		
		// className 
		if (expect(new String[]{"className"}, new TokenType[]{TokenType.identifier}, true)) {
			writer.setClassName(previousToken);
		} 

		// '{' 
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {}

		// classVarDec* 
		while (compileClassVarDec(false)) {};

		// subroutineDec* 
		while (compileSubroutineDec(false)) {};

		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {}

		writer.spaceCount--;
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
			writer.spaceCount++;
		}
		kind = previousToken.toUpperCase(); // Store kind of variable so it can later be added to the SymbolTable.

		// type 
		String[] expectedTokens1 = {"int", "char", "boolean", "className"};
		TokenType[] expectedTokenTypes1 = {TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier};
		if (expect(expectedTokens1, expectedTokenTypes1, true)) {}
		type = previousToken;
		

		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			symbolTable.define(previousToken, type, SymbolKind.valueOf(kind));

			writer.writeXMLLine(String.format("// Define of variable %s type %s, index: %d", previousToken, kind, symbolTable.indexOf(previousToken)));
		} 

		// (',' varName)* 
		while (true) {
			// ','
			if (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {} else {
				usePreviousLine = true;
				break;
			}

			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				// varName will have the same type and kind as the first varName initialised in the loop.
				symbolTable.define(previousToken, type, SymbolKind.valueOf(kind));
				
				writer.writeXMLLine(String.format("// Define of variable %s type %s, index: %d", previousToken, kind, symbolTable.indexOf(previousToken)));
			}
		} 

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {}

		writer.spaceCount--;
		return true;
	}
	
	// Compiles a complete method, function, or a constructor.
	private boolean compileSubroutineDec(boolean allowedToFail) throws Exception {

		// ('constructor'|'function'|'method')
		String functionType = "";
		if (!expect(new String[]{"constructor", "function", "method"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword}, allowedToFail)) {
			usePreviousLine = true;
			return false;
		} else {
			symbolTable.startSubroutine(); // Clears subroutine symbol table, ready for the next subroutine.
			writer.spaceCount++;
			functionType = previousToken;
			writer.writeXMLLine(String.format("// Start of subroutine."));
		}
		
		// ('void'|type) 
		String tokenTypeName = "";
		if (expect(new String[]{"void", "int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
			// Allows for 'className' to be written instead of 'identifier', for more accurate identifiers.
			tokenTypeName = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				tokenTypeName = "className";
			}
			currentFunctionReturn = previousToken;
		}
		

		// subroutineName 
		String subroutineName = "";
		if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
			subroutineName = previousToken;
		} 

		// '(' 
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {}

		// parameterList 
		int numOfParameters = compileParameterList(); 
		// Count number of parameters.

		// ')' 
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {}

		/*
		Here we need to write a function: className.functionName nLocals
		If constructor:
			Works on one argument.
			Must allocate memory for new object and set base of this to new object's base address.
			Memory.alloc(size).
		If method:
			Due to operating on object, k+1 arguments.	
		
			push argument 0
			pop pointer 0 // THIS = argument 0
		If function:
		*/
		writer.writeFunction(subroutineName, numOfParameters);
		if (functionType.equals("constructor")) {
			// Must allocate memory for new object and set base of this to new object's base address.
			writer.writePush(SegmentType.CONSTANT, symbolTable.getNumOfFields()); // push number of fields to stack
			writer.writeCall("Memory.alloc", 1);
			writer.writePop(SegmentType.POINTER, 0); // THIS = MEMORY ADDRESS
		} else if (functionType.equals("method")) {
			writer.writePush(SegmentType.ARG, 0);
			writer.writePop(SegmentType.POINTER, 0); // THIS = argument 0
		} else { // Function
		}

		compileSubroutineBody();

		// If method is void, then we need to insert a push constant 0 before return.
		// If constructor, return this. Ensure this is correct and inserted correctly.

		writer.spaceCount--;

		return true;
	}
	
	// Compiles a (possible empty) parameter list.
	// Does not handle the enclosing '()'.
	private int compileParameterList() throws Exception {
		writer.spaceCount++;
		int numOfParameters = 0;

		// ( (type varName) (',' type varName)*)?
		// type
		if (expect(new String[]{"int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, false)) {
			String firstType = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				firstType = "className";
			}
		
			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				symbolTable.define(previousToken, firstType, SymbolKind.ARG);
				numOfParameters++;

				writer.writeXMLLine(String.format("<%s> %s </%s> <!--Declaration+Initialisation. Index: %d -->", SymbolKind.ARG, previousToken, SymbolKind.ARG, symbolTable.indexOf(previousToken)));
			} 

			// (',' type varName)*	
			while (true) {
				// ','
				if (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {
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

				} 
				secondTypes = previousTokenType; // Redundant, but shuts the Java compiler up.

				// varName
				if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
					symbolTable.define(previousToken, secondTypes, SymbolKind.ARG);
					numOfParameters++;

					writer.writeXMLLine(String.format("<%s> %s </%s> <!--Declaration+Initialisation. Index: %d -->", SymbolKind.ARG, previousToken, SymbolKind.ARG, symbolTable.indexOf(previousToken)));
				} 
			} 
		} else {
			usePreviousLine = true;
		}

		writer.spaceCount--;
		return numOfParameters;
	}
	
	// Compiles a subroutine's body.
	private void compileSubroutineBody() throws Exception {
		writer.spaceCount++;

		// '{' 
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
		}

		// varDec* 
		while(compileVarDec()) {}

		// statements
		writer.spaceCount++;
		compileStatements();
		writer.spaceCount--;

		usePreviousLine = true;
		
		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
		}

		writer.spaceCount--;
	}
	
	// Compiles a var declaration.
	private boolean compileVarDec() throws Exception {
		String type;

		// 'var' 
		if (!expect(new String[]{"var"}, new TokenType[]{TokenType.keyword}, false)) {
			usePreviousLine = true;
			return false;
		} else {
			writer.spaceCount++;
		}

		// type 
		if (expect(new String[]{"int", "char", "boolean", "className"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.identifier}, true)) {
			// Allows for 'className' to be written instead of 'identifier', for more accurate identifiers.
			String tokenTypeName = previousTokenType;
			if (previousTokenType == TokenType.identifier.name()) {
				tokenTypeName = "className";
			}

		}
		type = previousToken;

		// varName 
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
			symbolTable.define(previousToken, type, SymbolKind.VAR);

			writer.writeXMLLine(String.format("// Define of variable %s kind %s, index: %d", previousToken, type, symbolTable.indexOf(previousToken)));
		}

		// (',' varName)* 
		while (true) {
			// ','
			if (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {} 
			else {
				usePreviousLine = true;
				break;
			}

			// varName
			if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {
				symbolTable.define(previousToken, type, SymbolKind.VAR);

				writer.writeXMLLine(String.format("// Define of variable %s kind %s, index: %d", previousToken, type, symbolTable.indexOf(previousToken)));
			}
		} 

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {}

		writer.spaceCount--;
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

			switch (previousToken){
				case "let":
					writer.writeXMLLine("<letStatement>");
					writer.spaceCount++;
					writer.writeXMLLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileLet();
					break;

				case "if":
					writer.writeXMLLine("<ifStatement>");
					writer.spaceCount++;
					writer.writeXMLLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileIfStatement();
					break;

				case "while":
					writer.writeXMLLine("<whileStatement>");
					writer.spaceCount++;
					writer.writeXMLLine(String.format("<%s> %s </%s>", previousTokenType, previousToken, previousTokenType));
					compileWhileStatement();
					break;

				case "do":
					writer.spaceCount++;
					compileDo();
					break;

				case "return":
					writer.spaceCount++;
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
		if (expect(new String[]{"varName"}, new TokenType[]{TokenType.identifier}, true)) {}

		SymbolKind varKind;
		SegmentType varType;
		String varName = previousToken;
		varKind = symbolTable.kindOf(varName);
		if (varKind.equals(SymbolKind.FIELD)) {
			varType = SegmentType.THIS;
		} else if (varKind.equals(SymbolKind.VAR)) {
			varType = SegmentType.LOCAL;
		} else {
			varType = SegmentType.valueOf(varKind.name());
		}

		// ('['expression']')? 
		// '['
		boolean isExpr = false;
		if (expect(new String[]{"["}, new TokenType[]{TokenType.symbol}, false)) {
			isExpr = true;

			// expression
			compileExpression(true);

			// ']'
			if (expect(new String[]{"]"}, new TokenType[]{TokenType.symbol}, true)) {}

			// Push varName
			writer.writePush(varType, symbolTable.indexOf(varName)); 

			// Pop pointer 1
			writer.writePop(SegmentType.POINTER, 1);
			
		} else {
			usePreviousLine = true;
		}

		// '=' 
		if (expect(new String[]{"="}, new TokenType[]{TokenType.symbol}, true)) {
		}

		// expression
		compileExpression(true);

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {}

		// Pop into value
		if (isExpr) { // Pop into that 0
			writer.writePop(SegmentType.THAT, 0);
		} else { // Pop into VarName
			writer.writePop(varType, symbolTable.indexOf(varName)); 
		}

		writer.spaceCount--;
	}
	
	// Compiles an if statment, possibly with a trailing else clause.
	private void compileIfStatement() throws Exception {
		// code for compiling an if statement
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {}

		// expression 
		compileExpression(true);

		// Skips to else statements if expression is failed
		writer.writeArithmetic("NOT");
		String L1 = writer.claimLabel();
		writer.writeIf(L1);

		// ')' 
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {}

		// '{'
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {}

		// statements
		writer.spaceCount++;
		compileStatements();
		writer.spaceCount--;

		// Skip over else statements if processed if statements
		String L2 = writer.claimLabel();
		writer.writeGoto(L2);

		usePreviousLine = true;

		// '}' 
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {}

		// ('else' '{' statements '}')?
		// 'else'
		writer.writeLabel(L1); // Label to jump to the else statements
		if (expect(new String[]{"else"}, new TokenType[]{TokenType.keyword}, false)) {
			// '{' 
			if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {
				writer.writeXMLLine("<symbol> { </symbol>");
			}

			// statements
			writer.spaceCount++;
			compileStatements();
			writer.spaceCount--;

			usePreviousLine = true;

			// '}'
			if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {
			}

		} else {
			usePreviousLine = true;
		}
		writer.writeLabel(L2); // Label to jump after the else statements

		writer.spaceCount--;
		writer.writeXMLLine("</ifStatement>");
	}
	
	// Compiles a while statement.
	private void compileWhileStatement() throws Exception {
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {}

		String L1 = writer.claimLabel();
		writer.writeLabel(L1); // Point to jump to at start of each while loop.

		// expression
		compileExpression(true);

		// If expression is not, then end of while loop and jump to after while loop (L2).
		writer.writeArithmetic("NOT");
		String L2 = writer.claimLabel();
		writer.writeIf(L2);

		// ')' 
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {}

		// '{'
		if (expect(new String[]{"{"}, new TokenType[]{TokenType.symbol}, true)) {}

		// statements
		writer.spaceCount++;
		compileStatements();
		writer.spaceCount--;

		usePreviousLine = true;

		writer.writeGoto(L1);

		// '}'
		if (expect(new String[]{"}"}, new TokenType[]{TokenType.symbol}, true)) {}

		writer.writeLabel(L2);

		writer.spaceCount--;
	}
	
	// Compiles a do statement.
	private void compileDo() throws Exception {
		// subroutineCall: subroutineName '(' expressionList ')' | ( className | varName )' '.' subroutineName '(' expressionList ')'
		// identifier = subroutineName or ( className | varName )
		String fullSubroutineName = "";
		Boolean isVarName = false;
		if (expect(new String[]{"varOrClassOrSubroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
			// Need to figure out if it's a variable.
			if (symbolTable.exists(previousToken)) {
				// Token is a variable.
				isVarName = true;
			} else {
				// Not a variable.
			}
			fullSubroutineName += previousToken;
		}
		// then check if '.' or not
		if (expect(new String[]{"."}, new TokenType[]{TokenType.symbol}, false)) {
			// Called method function if valid variable, must push reference to object being called
			if (isVarName) {
				SymbolKind kind = symbolTable.kindOf(fullSubroutineName);
				SegmentType type;
				if (kind.equals(SymbolKind.FIELD)) {
					type = SegmentType.THIS;
				} else if (kind.equals(SymbolKind.VAR)) {
					type = SegmentType.LOCAL;
				} else {
					type = SegmentType.valueOf(kind.name());
				}
				writer.writePush(type, symbolTable.indexOf(fullSubroutineName)); // Reference to object being called
			}

			// '.' subroutineName
			// '.'
			fullSubroutineName += ".";

			// subroutineName
			if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
				fullSubroutineName += previousToken;
			}
		} else {
			usePreviousLine = true;
			// Means subroutineName() which means calling Method of current object.
			writer.writePush(SegmentType.THIS, 0);
		}
		// '(' expressionList ')'
		// '('
		if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
		}

		// expressionList
		int nArgs = compileExpressionList();

		// ')'
		if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
		}
		
		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
		}

		writer.writeCall(fullSubroutineName, nArgs);
		writer.writePop(SegmentType.TEMP, 0);

		writer.spaceCount--;
	}
	
	// Compiles a return statement.
	private void compileReturn() throws Exception {
		// expression?
		compileExpression(false);

		System.err.println(currentFunctionReturn);
		if (currentFunctionReturn.equals("void")) {
			writer.writePush(SegmentType.CONSTANT, 0);
		}

		writer.writeReturn();

		// ';'
		if (expect(new String[]{";"}, new TokenType[]{TokenType.symbol}, true)) {
		}

		writer.spaceCount--;
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
			String opInstruction = "";
			if (expect(new String[]{"+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="}, new TokenType[]{TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol, TokenType.symbol}, false)) {
				opInstruction = previousToken;
			} else {
				usePreviousLine = true;
				break;
			}

			// term
			compileTerm(false, true);

			// perform op
			switch (opInstruction) {
				case "*":
					writer.writeCall("Math.multiply", 2);
					break;
				case "/":
					writer.writeCall("Math.divide", 2);
					break;
				case "+":
					writer.writeArithmetic("ADD");
					break;
				case "-":
					writer.writeArithmetic("SUB");
					break;
				case "&amp;":
					writer.writeArithmetic("AND");
					break;
				case "|":
					writer.writeArithmetic("OR");
					break;
				case "&lt;":
					writer.writeArithmetic("LT");
					break;
				case "&gt;":
					writer.writeArithmetic("GT");
					break;
				case "=":
					writer.writeArithmetic("EQ");
					break;
				case "_":
					throw new Exception("ERROR: Unusual, invalid operand leading to default switch case in compileExpression: " + opInstruction);
			}
			
		} 

		if (atLeastOneTerm) {
			writer.spaceCount--;
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
			writer.spaceCount++;
		}

		writer.spaceCount++;

		boolean validTerm = false;
		boolean doNotUsePreviousLine = false;
		// integerConstant 
		// tokenType is integerConstant
		if (previousTokenType.equals(TokenType.integerConstant.name())) {
			writer.writePush(SegmentType.CONSTANT, Integer.parseInt(previousToken));
			validTerm = true;
			doNotUsePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		// stringConstant
		// tokenType is stringConstant
		if (!validTerm && previousTokenType.equals(TokenType.stringConstant.name())) {
			writer.writeXMLLine(String.format("<stringConstant> %s </stringConstant>", previousToken));
			validTerm = true;
			doNotUsePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		// keywordConstant 
		// tokenType is keyword of token 'true'|'false'|'null'|'this'
		if (!validTerm && expect(new String[]{"true", "false", "null", "this"}, new TokenType[]{TokenType.keyword, TokenType.keyword, TokenType.keyword, TokenType.keyword}, false)) {
			writer.writeXMLLine(String.format("<keyword> %s </keyword>", previousToken)); // WARNING: Maybe supposed to be keywordConstant?
			switch (previousToken) {
				case "true":
					writer.writePush(SegmentType.CONSTANT, 1);
					writer.writeArithmetic("NEG");
					break;
			
				case "false":
					writer.writePush(SegmentType.CONSTANT, 0);
					break;
				
				case "null":
					writer.writePush(SegmentType.CONSTANT, 0);
					break;
				
				case "this": // WARNING: Assuming this 0 = current object
					writer.writePush(SegmentType.THIS, 0);
					break;
				
				default:
					break;
			}

			validTerm = true;
			doNotUsePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		// '('expression')' 
		// tokenType is symbol of token '('
		if (!validTerm && expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, false)) {
			validTerm = true;

			// expression
			compileExpression(true);

			// ')' 
			if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
			}

			doNotUsePreviousLine = true;

		} else {
			usePreviousLine = true;
		}

		// (unaryOp term) 
		// tokenType is symbol of token '-'|'~'
		if (!validTerm && expect(new String[]{"-", "~"}, new TokenType[]{TokenType.symbol, TokenType.symbol}, false)) {
			validTerm = true;
			String unaryOp = previousToken;

			// term
			compileTerm(false, true);

			// unaryOp written after term. e.g., -1 -> push 1; NEG;
			if (unaryOp.equals("-")) {
				writer.writeArithmetic("NEG");
			} else {
				writer.writeArithmetic("NOT");
			}
		} else {
			usePreviousLine = true;
		}

		if (!validTerm && expect(new String[]{"identifier"}, new TokenType[]{TokenType.identifier}, false)) {
			String varToken = previousToken;
			validTerm = true;

			// varName|subroutineName|className
			// Need to figure out if it's a variable.
			boolean isVar = false;
			SymbolKind varKind = SymbolKind.NONE;
			SegmentType varType = SegmentType.NONE;
			if (symbolTable.exists(varToken)) {
				// Token is a variable.
				isVar = true;
				varKind = symbolTable.kindOf(varToken);
				if (varKind.equals(SymbolKind.FIELD)) {
					varType = SegmentType.THIS;
				} else if (varKind.equals(SymbolKind.VAR)) {
					varType = SegmentType.LOCAL;
				} else {
					varType = SegmentType.valueOf(varKind.name());
				}
			} else {
			}

			// varName'['expression']' 
			// tokenType of identifier followed by a symbol of token '['
			if (expect(new String[]{"["}, new TokenType[]{TokenType.symbol}, false)) { 

				// expression
				compileExpression(true);

				// ']' 
				if (expect(new String[]{"]"}, new TokenType[]{TokenType.symbol}, true)) {
				}

				// Push varName
				writer.writePush(varType, symbolTable.indexOf(varToken)); 

				// Pop pointer 1
				writer.writePop(SegmentType.POINTER, 1);

				// Push that 0
				writer.writePush(SegmentType.THAT, 0);
			
			// subroutineCall: subroutineName '(' expressionList ')' | ( className | varName)' '.' subroutineName '(' expressionList ')'
			// tokenType of identifier followed by symbol of token '(' or .
			} else if ((usePreviousLine = true) && expect(new String[]{"(", "."}, new TokenType[]{TokenType.symbol, TokenType.symbol}, false)) {
				// then check if '.' or not
				if (previousToken.equals(".")) {
					// Need to push varName
					writer.writePush(varType, symbolTable.indexOf(varToken)); 

					// '.' subroutineName
					// '.'
					varToken += ".";
					

					// subroutineName
					if (expect(new String[]{"subroutineName"}, new TokenType[]{TokenType.identifier}, true)) {
						varToken += previousToken;
					}
				} else {
					writer.writePush(SegmentType.THIS, 0);
					usePreviousLine = true;
				}
				// '(' expressionList ')'
				// '('
				if (expect(new String[]{"("}, new TokenType[]{TokenType.symbol}, true)) {
				}
				
				// expressionList
				int nArgs = compileExpressionList();

				// ')'
				if (expect(new String[]{")"}, new TokenType[]{TokenType.symbol}, true)) {
				}

				writer.writeCall(varToken, nArgs);
				// Temp 0 NOT popped because function shouldn't be void if a term.

			// varName
			// tokenType of identifier followed not by a symbol of token '['
			} else {
				// Push varName
				writer.writePush(varType, symbolTable.indexOf(varToken)); 

				usePreviousLine = true;
			}

		}

		if (validTerm) {
			writer.spaceCount--;
		}

		if (doNotUsePreviousLine) {
			usePreviousLine = false;
		}

		return validTerm;
	}
	
	// Compiles a (possible empty) comma-separated list of expressions.
	private int compileExpressionList() throws Exception {
		writer.spaceCount++;
		int numOfExpressions = 0;
	
		// (expression (',' expression)* )?
		// expression 
		if (compileExpression(false)) {
			// (',' expression)*
			numOfExpressions++;

			while (expect(new String[]{","}, new TokenType[]{TokenType.symbol}, false)) {

				compileExpression(true);
				numOfExpressions++;
			}
			usePreviousLine = true;
		} else {
			usePreviousLine = true;
		}

		writer.spaceCount--;
		return numOfExpressions;
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
