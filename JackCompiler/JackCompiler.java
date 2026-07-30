import java.io.FileWriter;

public class JackCompiler {

    public static void main(String []args) {
        // Ensure argument has been entered.
        if (args.length < 1) {
            System.out.println("Usage: JackCompiler fileName.jack|directoryName");
            return;
        }

        // Check whether directory or filename.
        String []argsSplit = args[0].split("/"); // [path, to, name.jack]
        if (argsSplit.length == 0) {
            System.out.println("Invalid file/directory path entered.");
            return;
        }

        argsSplit = argsSplit[argsSplit.length - 1].split("\\."); // [fileName, jack] or [directoryName]
        if (argsSplit.length == 0) {
            System.out.println("Invalid file/directory path entered.");
            return;
        }

        boolean isDirectory = false;
        boolean isJackFile = false;
        if (argsSplit.length == 1) { // Potential dictionary.
            if (argsSplit[0].charAt(argsSplit[0].length() - 1) != '.') {
                System.out.println("Directory.");
                isDirectory = true;
            } else {
                System.out.println("Empty file extension.");
                return;
            }
        } else if (argsSplit[1].equals("jack")) { // Jack file.
            System.out.println("Jack file.");
            isJackFile = true;
        } else {
            System.out.println("Non Jack file entered.");
            return;
        }

        String[] filesToProcess;
        if (isJackFile) {
            filesToProcess = new String[1];
            filesToProcess[0] = args[0];
        } else if (isDirectory) {

            filesToProcess = new String[5];
        } else {
            return;
        }

        for (int fileNumber = 0; fileNumber < filesToProcess.length; fileNumber++) {
            try {
                JackTokeniser jack = new JackTokeniser(filesToProcess[fileNumber]);
                FileWriter file = new FileWriter(filesToProcess[fileNumber].substring(0, args[0].length() - 5) + ".xml");
                System.out.println(filesToProcess[fileNumber]);
                System.out.println(filesToProcess[fileNumber].substring(0, args[0].length() - 5) + ".xml");

                // WARNING: Advance still needs to be processed/outputted one more time.
                file.write("<tokens>\n");
                while (jack.advance()){
                    if (!jack.getCurrentTokenType().equals("NONE")) {
                        file.write(String.format("<%s> %s </%s>\n", 
                            jack.getCurrentTokenType(), jack.getCurrentToken(), jack.getCurrentTokenType()));
                    }
                }
                if (!jack.getCurrentTokenType().equals("NONE")) {
                        file.write(String.format("<%s> %s </%s>\n", 
                            jack.getCurrentTokenType(), jack.getCurrentToken(), jack.getCurrentTokenType()));
                }
                file.write("</tokens>\n");

                file.close();
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }

    }
}