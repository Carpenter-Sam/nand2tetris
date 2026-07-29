public class JackCompiler {

    public static void main(String []args) {
        try {
            JackTokeniser jack = new JackTokeniser("test-files/test.jack");

            // WARNING: Advance still needs to be processed/outputted one more time.
            while (jack.advance()){
                // print "<" + tokenClassification + ">"
                // print the current token value
                // print "</" + tokenClassification + ">"
                // print newLine
                System.out.println(String.format("<%s> %s <%s>", 
                    jack.getCurrentTokenType(), jack.getCurrentToken(), jack.getCurrentTokenType()));
            }
            System.out.println(String.format("<%s> %s <%s>", 
                    jack.getCurrentTokenType(), jack.getCurrentToken(), jack.getCurrentTokenType()));
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}