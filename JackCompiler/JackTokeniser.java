import java.io.FileReader;
import java.util.Arrays;

public class JackTokeniser {
	// Opens the input .jack file and gets ready to tokenise it.
	// Constructor(input file/stream)
    public JackTokeniser(String filename) throws Exception {
		final FileReader file;

		String[] file_split = filename.split("\\.");
		// System.err.println(Arrays.toString(file_split));

		if (!file_split[file_split.length - 1].equals("jack") || file_split.length < 2) {
			throw new Exception("File not ending in Jack.");
		}

		try {
			file = new FileReader(filename);
			file.close();
		}
		catch (Exception e) {
			System.out.println(e);
			// throw new FileNotFoundException("File not found.");
		}
    }
	
	// Are there more tokens in the input?
	// hasMoreTokens() // returns boolean
	
	// Gets the next token from the input, and makes it the current token.
	// This method should only be called if there are more tokens.
	// Initially there is no current token.
	public boolean advance() {
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