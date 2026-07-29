import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class JackTokeniser {
	private FileReader file;
	private String currentToken;
	public String nextToken;
	private TokenType currentTokenType;

	// Opens the input .jack file and gets ready to tokenise it.
	// Constructor(input file/stream)
    public JackTokeniser(String filename) throws Exception {
		currentToken = "";
		nextToken = "";
		currentTokenType = TokenType.NONE;

		String[] file_split = filename.split("\\.");
		// System.err.println(Arrays.toString(file_split));

		if (!file_split[file_split.length - 1].equals("jack") || file_split.length < 2) {
			throw new IOException("File not ending in Jack.");
		}

		file = new FileReader(filename);

		// Loop until first non-whitespace character is found
		int nextChar;
		while (Character.isWhitespace(nextChar = file.read())) {}
		// Check that there is actually a non-whitespace character in the file
		if (nextChar == -1) {
				throw new IOException("Empty file.");
		} else {
			nextToken += (char)nextChar;
		}
    }
	
	// Are there more tokens in the input?
	// hasMoreTokens() // returns boolean
	
	// Gets the next token from the input, and makes it the current token.
	// This method should only be called if there are more tokens.
	// Initially there is no current token.
	public boolean advance() throws Exception {
		currentToken = nextToken;
		boolean validToken = false;
		int nextChar;
		String errorMsg = "";

		// Warning check for if current token is greater than one character.
		if (currentToken.length() > 1) {
			System.err.println("WARNING: Within advance() currentToken has started with more than one character: " + currentToken);
			return false;
		}

		// If the token is a symbol we increment once to get the next value and exit
		if (isSymbol(currentToken.charAt(0), true)) {
			validToken = true;
			currentTokenType = TokenType.symbol;
			nextChar = file.read();

		// If string then increment until " reached, if newline reached then invalid.
		// WARNING: Escape characters such as \" not implemented.
		} else if (currentToken.equals("\"")) {
			currentToken = ""; // to remove the leading "
			nextChar = file.read();
			// Loop until " or until the end of the file/a newline character.
			while (nextChar != 10 && nextChar != -1 && !validToken) {
				if (nextChar == 34) {
					validToken = true;
					currentTokenType = TokenType.stringConstant;
				} else if (nextChar != 10 && nextChar != -1) {
					currentToken += (char)nextChar;
				}
				nextChar = file.read();
			}

		// If number then increment until first non-number.
		} else if (48 <= currentToken.charAt(0) && currentToken.charAt(0) <= 57) { // Checks if character is a digit 0-9.
			nextChar = file.read();
			// Loop until digit is no longer 0-9.
			while(48 <= nextChar && nextChar <= 57) {
				currentToken += (char)nextChar;
				nextChar = file.read();
			}
			try {
				if (0 <= Integer.parseInt(currentToken) && Integer.parseInt(currentToken) <= 32767) {
					validToken = true;
					currentTokenType = TokenType.intConstant;
				} else {
					errorMsg = currentToken + " is out of bounds. Ensure integer constants are 0..=32767.";
				}
			}
			catch (NumberFormatException e) {
				throw new Exception("Invalid integer constant token: " + currentToken);
			}

		// Reaching this else indicates token is either a keyword, identifier or an invalid token.
		} else {
			nextChar = file.read();

			// Loop until end of token (a symbol, double-quote, or whitespace).
			while(!isSymbol((char)nextChar, false) && nextChar != '\"' && !Character.isWhitespace(nextChar)) {
				currentToken += nextChar;
				nextChar = file.read();
			}

			// Check if token is a keyword.
			if (isKeyword(currentToken)) {
				validToken = true;
				currentTokenType = TokenType.keyword;
			}

			// If not a keyword, check if token is eligible as an identifier.
			
		}

		// loops until token is acquired

		// ignore whitespace until first non-whitespace character found
		// then whitespace indicates end of token

		// if first character
		// 			number = integer constant then loop until no more digits
		// 			symbol = symbol then end of token
		// 			double quote = string constant loop until double quote
		// else loop until end of token (indicated by symbol, double-quote or whitespace)
		//	check if token is a keyword
		//	if not keyword then check if identifier with only letters, digits and underscore (not starting with a digit)
	
		// if not a valid token then raise error
		// if file ends without valid token raise error
		// if end of file then close and return False else return True

		// If next character is a whitespace, then loop until next non-whitespace character.
		if (Character.isWhitespace(nextChar)) {
			while (Character.isWhitespace(nextChar = file.read())) {}
		}
		// If next character isn't -1 then add it to next token.
		if (nextChar != -1) {
			nextToken = "" + (char)nextChar;
		}

		if (!validToken) { // Check if the token is valid or not, if not raise an exception.
			file.close();
			if (errorMsg.isBlank()){
				throw new Exception("Invalid token: " + currentToken);
			} else {
				throw new Exception(errorMsg);
			}
		} else if (nextChar == -1) { // Check if end of file reached, if it is then close file.
			file.close();
			return false;
		} else { // Return true to indicate there are more tokens to parse.
			return true;
		}
	}

	private boolean isSymbol(char token, boolean editEscapeCharacters) {
		// '{' | '}' | '(' | ')' | '['  | ']' |
		// '.' | ',' | '+' | '-' | '\*' | '/' |
		// '&' | '|' | '<' | '>' | '='  | '~'
		if (token == '{' || token == '}' || token == '(' || token == ')' || token == '[' || token == ']' || 
			token == '.' || token == ',' || token == '+' || token == '-' || token == '*' || token == '/' || 
			token == '&' || token == '|' || token == '<' || token == '>' || token == '=' || token == '~' ) {
				if (editEscapeCharacters) {
					// Some symbols must be replaced with escape characters.
					if (token == '<') {
						currentToken = "&lt;";
					} else if (token == '>') {
						currentToken = "&gt;";
					} else if (token == '\"') {
						currentToken = "&quot;";
					} else if (token == '&') {
						currentToken = "&amp;";
					}
				}
				return true;
		} else {
			return false;
		}
	}

	private boolean isKeyword(String token) {
		// 'class'  | 'constructor' | 'function' | 'method' | 'field'   |
		// 'static' | 'var'         | 'int'      | 'char'   | 'boolean' |
		// 'void'   | 'true'        | 'false'    | 'null'   | 'this'    |
		// 'let'    | 'do'          | 'if'       | 'else'   | 'while'   |
		// 'return'
		if (token.equals("class")  || token.equals("constructor") || token.equals("function") || token.equals("method") || token.equals("field")   ||
			token.equals("static") || token.equals("var")         || token.equals("int")      || token.equals("char")   || token.equals("boolean") ||
			token.equals("void")   || token.equals("true")        || token.equals("false")    || token.equals("null")   || token.equals("this")    ||
			token.equals("let")    || token.equals("do")          || token.equals("if")       || token.equals("else")   || token.equals("while")   ||
			token.equals("return")) {
			return true;
		} else {
			return false;
		}		
	}

	public String getCurrentToken() {
		return currentToken;
	}

	public String getCurrentTokenType() {
		return currentTokenType.name();
	}
	
	// Returns the type of the current token, as a constant.
	// tokenType() // returns KEYWORD, SYMBOL, INDENTIFIER, INT_CONST, STRING_CONST
	
	// Returns the keyword which is the current token, as a constant.
	// This method should be called only if tokenType is KEYWORD.
	// keyWord() // returns CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID, VAR, STATIC, FIELD, LET, DO IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS
	
	// Returns the character which is the current token.
	// Only called if tokenType is SYMBOL.
	// symbol() // returns char
	
	// Returns the identifier which is the current token.
	// Only called if tokenType is IDENTIFIER.
	// indentifier() // returns string
	
	// Returns the integer value which is the current token.
	// Only called if tokenType is INT_CONST.
	// intVal() // returns int
	
	// Returns the string value of the current token, without enclosing double quotes.
	// Only called if tokenType is STRING_CONST.
	// stringVal() // returns string
}