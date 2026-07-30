import java.io.File;
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
            File directory = new File(args[0]);
            File[] files = directory.listFiles();

            int numberOfJackFiles = 0;
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].getName().endsWith(".jack")) {
                    numberOfJackFiles++;
                }
            }

            filesToProcess = new String[numberOfJackFiles];

            numberOfJackFiles = 0;
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].getName().endsWith(".jack")) {
                    // String filePath = files[i].toString();
                    filesToProcess[numberOfJackFiles++] = files[i].toString();
                }
            }

        } else {
            System.err.println("Please ensure either the path to a directory or Jack file is entered.");
            return;
        }

        for (String currentFile : filesToProcess) {
        // for (int fileNumber = 0; fileNumber < filesToProcess.length; fileNumber++) {
            try {
                JackTokeniser jack = new JackTokeniser(currentFile);
                FileWriter writeFile = new FileWriter(currentFile.substring(0, currentFile.length() - 5) + ".Txml"); 

                // WARNING: Advance still needs to be processed/outputted one more time.
                writeFile.write("<tokens>\n");
                while (jack.advance()){
                    if (!jack.getCurrentTokenType().equals("NONE")) {
                        writeFile.write(String.format("<%s> %s </%s>\n", 
                            jack.getCurrentTokenType(), jack.getCurrentToken(), jack.getCurrentTokenType()));
                    }
                }
                if (!jack.getCurrentTokenType().equals("NONE")) {
                        writeFile.write(String.format("<%s> %s </%s>\n", 
                            jack.getCurrentTokenType(), jack.getCurrentToken(), jack.getCurrentTokenType()));
                }
                writeFile.write("</tokens>\n");

                writeFile.close();
            }
            catch (Exception e) {
                System.out.println(e);
                
                // Delete file if things go wrong. Disabled for better debugging.
                // File fileToDelete = new File(currentFile.substring(0, currentFile.length() - 5) + ".Txml");
                // fileToDelete.delete();

                // Stop compilation at this point.
                break;
            }

            // Now CompileEngine will read from .Txml and write to .xml, deleting .xml if things go wring and deleting .Txml no matter what.
            try {
                // Create CompilationEngine, which will run automatically.
                CompilationEngine engine = new CompilationEngine(new File(currentFile.substring(0, currentFile.length() - 5) + ".Txml"), 
                                                                 new File(currentFile.substring(0, currentFile.length() - 5) + ".xml"));
                
            } catch (Exception e) {
                System.err.println(e);
                // Things have gone wrong. Delete .xml outside of debugging.
                // File fileToDelete = new File(currentFile.substring(0, currentFile.length() - 5) + ".xml");
                // fileToDelete.delete();
            } finally {
                // Temporary .Txml file deleted no matter what.
                // File fileToDelete = new File(currentFile.substring(0, currentFile.length() - 5) + ".Txml");
                // fileToDelete.delete();
            }
        }

    }

}