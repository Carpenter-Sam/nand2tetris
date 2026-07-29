import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class JackTokeniser {
	private FileReader file;
	private String currentToken;
	private String nextToken;
	private TokenType currentTokenType;

	// Opens the input .jack file and gets ready to tokenise it.
	// Constructor(input file/stream)
    public JackTokeniser(String filename) throws Exception {
		currentToken = "";
		nextToken = "";

		String[] file_split = filename.split("\\.");
		// System.err.println(Arrays.toString(file_split));

		if (!file_split[file_split.length - 1].equals("jack") || file_split.length < 2) {
			throw new Exception("File not ending in Jack.");
		}

		try {
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
		catch (IOException e) {
			System.err.println(e);
			file.close();
		}
		catch (Exception e) {
			System.err.println(e);
			file.close();
			// throw new FileNotFoundException("File not found.");
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

		// If the token is a symbol we increment once to get the next value and exit
		if (isSymbol(currentToken)) {
			validToken = true;
			nextChar = file.read();
		} else {
			nextChar = file.read();
		}

		// loops until token is acquired

		// ignore whitespace until first non-whitespace character found
		// then whitespace indicates end of token

		// if first character
		// 			number = integer constant then loop until no more digits
		// 			symbol = symbol then end of token
		// 			double quote = string constant loop until double quote
		// else loop until end of token (indicated by start of intConstant, symbol, stringConstant or whitespace)
		//	check if token is a keyword
		//	if not keyword then check if identifier with only letters, digits and underscore (not starting with a digit)
	
		// if not a valid token then raise error
		// if file ends without valid token raise error
		// if end of file then close and return False else return True

		// If next character is a whitespace, then loop until next non-whitespace character
		if (Character.isWhitespace(nextChar)) {
			while (Character.isWhitespace(nextChar = file.read())) {}
		}
		if (nextChar != -1) {
			nextToken = "" + (char)nextChar;
		}

		if (!validToken) { // Check if the token if valid or not.
			file.close();
			throw new Exception("Invalid token.");
		} else if (nextChar == -1) { // Check if end of file reached.
			file.close();
			return false;
		} else { // Return true to indicate there are more tokens to parse.
			return true;
		}
	}

	private boolean isSymbol(String token) {
		if (token.length() > 1) {
			System.err.println("WARNING: isSymbol given String with more that one character: " + token);
			return false;
		}
		// '{'|'}'|'('|')'|'['|']'|'.'|','|'+'|''-'|'\*'|'/'|'&'|'|'|'<'|'>'|'='|'~'
		if (token.equals("{") || token.equals("}") || token.equals("(") || token.equals(")") || token.equals("[") || token.equals("]") || 
			token.equals(".") || token.equals(",") || token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") || 
			token.equals("&") || token.equals("|") || token.equals("<") || token.equals(">") || token.equals("=") || token.equals("~")) {
				currentTokenType = TokenType.SYMBOL;

				// Some symbols must be replaced with escape characters.
				if (token.equals("<")) {
					currentToken = "&lt;";
				} else if (token.equals(">")) {
					currentToken = "&gt;";
				} else if (token.equals("\"")) {
					currentToken = "&quot;";
				} else if (token.equals("&")) {
					currentToken = "&amp;";
				}
				return true;
			}
		return false;
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