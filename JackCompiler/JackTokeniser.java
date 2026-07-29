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
		int nextChar = 0;
		String errorMsg = "";

		// First arning check for if current token is greater than one character.
		if (currentToken.length() > 1) {
			System.err.println("WARNING: Within advance() pre-comment check, currentToken has started with more than one character: " + currentToken);
			return false;
		}
		
		// System.out.println("hi: " + currentToken);
		boolean endOfFile = false;
		boolean nonCommentSlash = false;
		boolean notComment = false;
		// Increment until non-comment is found.
		while (!notComment) {
			nonCommentSlash = false;
			if (currentToken.charAt(0) == '/') { // Potential for a comment.
				nextChar = file.read();
				if (nextChar == -1) {
					endOfFile = true;
				} else if ((currentToken + (char)nextChar).equals("//")) { // One-line comment, increment until newline.
					while ((nextChar = file.read()) != 10 && nextChar != -1) {}
					nextChar = file.read(); // Increment once more to get next character.
				} else if ((currentToken + (char)nextChar).equals("/*")) { // Multi-line comment, increment until */.
					nextChar = file.read();
					String twoLine = "" + (char)nextChar;
					nextChar = file.read();
					twoLine += (char)nextChar;

					while (!twoLine.equals("*/") && nextChar != -1) {
						nextChar = file.read();
						twoLine = "" + twoLine.charAt(1) + (char)nextChar;
					}
					nextChar = file.read(); // Increment once more to get next character.
				} else {
					nonCommentSlash = true;
					notComment = true;
				}

				if (!nonCommentSlash) {
					// If next character is a whitespace, then loop until next non-whitespace character.
					if (Character.isWhitespace(nextChar)) {
						while (Character.isWhitespace(nextChar = file.read())) {}
					}
					// If next character isn't -1 then put it in the current token.
					if (nextChar != -1) {
						currentToken = "" + (char)nextChar;
					} else {
						endOfFile = true;
						notComment = true;
					}
				} else {
					if (nextChar == -1) {
						endOfFile = true;
					}
				}
			} else {
				notComment = true;
			}
		}
		// If last token is actually a comment then JackCompiler will print undefined characters.

		// Second warning check for if current token is greater than one character.
		if (currentToken.length() > 1) {
			System.err.println("WARNING: Within advance() post-comment check, currentToken has started with more than one character: " + currentToken);
			return false;
		}

		// Skips processing of character if at end of file.
		if (endOfFile) {
			currentToken = "";
			currentTokenType = TokenType.NONE;

		// Special instance where there was a '/' that wasn't part of a comment.
		} else if (nonCommentSlash) {
			validToken = true;
			currentTokenType = TokenType.symbol;
			// nextChar = file.read();
			
		// If the token is a symbol we increment once to get the next value, then check if it's a comment.
		} else if (isSymbol(currentToken.charAt(0), true)) {
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
			// Loop until end of token (a symbol, double-quote, whitespace or end of file).
			while(!isSymbol((char)nextChar, false) && nextChar != '\"' && !Character.isWhitespace(nextChar) && nextChar != -1) {
				currentToken += (char)nextChar;
				nextChar = file.read();
			}

			// Check if token is a keyword.
			if (isKeyword(currentToken)) {
				validToken = true;
				currentTokenType = TokenType.keyword;
			}

			// If not a keyword, check if token is eligible as an identifier.
			// Eligible if only contains A-Z, a-z, 0-9 and _ characters but doesn't start with a digit.
			// WARNING: If identifier start with a number gets tokenised into integerConstant + identifier.
			if (currentToken.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
				validToken = true;
				currentTokenType = TokenType.identifier;
			}
			
		}

		// If next character is a whitespace, then loop until next non-whitespace character.
		if (Character.isWhitespace(nextChar)) {
			while (Character.isWhitespace(nextChar = file.read())) {}
		}
		// If next character isn't -1 then add it to next token.
		if (nextChar != -1) {
			nextToken = "" + (char)nextChar;
		}

		if (endOfFile) { // Special check for end of file reatled to comments.
			file.close();
			return false;
		} else if (!validToken) { // Check if the token is valid or not, if not raise an exception.
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
		// '&' | '|' | '<' | '>' | '='  | '~' | ';'
		if (token == '{' || token == '}' || token == '(' || token == ')' || token == '[' || token == ']' || 
			token == '.' || token == ',' || token == '+' || token == '-' || token == '*' || token == '/' || 
			token == '&' || token == '|' || token == '<' || token == '>' || token == '=' || token == '~' || token == ';') {
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