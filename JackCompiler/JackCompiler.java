public class JackCompiler {

    public static void main(String []args) {
        try {
            JackTokeniser jack = new JackTokeniser("test-files/test.jack");
            jack.advance();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}