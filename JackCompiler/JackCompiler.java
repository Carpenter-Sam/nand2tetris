public class JackCompiler {

    public static void main(String []args) {
        try {
            JackTokeniser jack = new JackTokeniser("test-files/ArrayTest/Main.jack");

            // WARNING: Advance still needs to be processed/outputted one more time.
            while (jack.advance()){
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