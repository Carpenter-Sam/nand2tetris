public class JackCompiler {

    public static void main(String []args) {
        try {
            JackTokeniser jack = new JackTokeniser("test-files/test.jack");

            // WARNING: Advance still needs to be processed/outputted one more time.
            while (jack.advance()){
                System.out.println(jack.getCurrentToken());
            }
            System.out.println(jack.getCurrentToken());
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}