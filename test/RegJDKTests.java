import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Run our RegAST matcher on j.u.regex tests from OpenJDK8 */
public class RegJDKTests {

    @Test
    public void test() throws Exception {
        processFile("test/TestCases.txt");
    }

    // modified code from OpenJDK8 j.u.regex tests:
    /**
     * Goes through the file "TestCases.txt" and creates many patterns
     * described in the file, matching the patterns against input lines in
     * the file, and comparing the results against the correct results
     * also found in the file. The file format is described in comments
     * at the head of the file.
     */
    void processFile(String fileName) throws Exception {
        File testCases = new File(System.getProperty("test.src", "."),
                fileName);
        FileInputStream in = new FileInputStream(testCases);
        BufferedReader r = new BufferedReader(new InputStreamReader(in));

        final List<String> excludesPat = Arrays.asList("[", "(?", "^", "{", "\\", /*?*/"?+", "+?", "++", "*+");
        final List<String> excludesData = Arrays.asList("\n");
        // Process next test case.
        int testCount = 0;
        wloop:
        while(r.readLine() != null) {
            // Read a line for pattern
            String patternString = grabLine(r);
            String dataString = grabLine(r);
            String expectedResult = grabLine(r);
            if (expectedResult.startsWith("error"))
                continue;

            if (patternString.startsWith("^")) patternString = patternString.substring(1);
            if (patternString.endsWith("$")) patternString = patternString.substring(0, patternString.length()-1);

            for (String s : excludesPat)
                if (patternString.contains(s))
                    continue wloop;
            for (String s : excludesData)
                if (dataString.contains(s))
                    continue wloop;

            boolean exp = dataString.matches(patternString);
            RegASTTest.check(patternString + "\n" + dataString, exp, patternString, dataString);

            patternString = ".*"+patternString+".*";
            exp = dataString.matches(patternString);
            RegASTTest.check(patternString + "\n" + dataString, exp, patternString, dataString);
            testCount++;
        }
        System.out.println(testCount);
    }

    /**
     * Reads a line from the input file. Keeps reading lines until a non
     * empty non comment line is read. If the line contains a \n then
     * these two characters are replaced by a newline char. If a \\uxxxx
     * sequence is read then the sequence is replaced by the unicode char.
     */
    String grabLine(BufferedReader r) throws Exception {
        int index = 0;
        String line = r.readLine();
        while (line.startsWith("//") || line.length() < 1)
            line = r.readLine();
        while ((index = line.indexOf("\\n")) != -1) {
            StringBuilder temp = new StringBuilder(line);
            temp.replace(index, index+2, "\n");
            line = temp.toString();
        }
        while ((index = line.indexOf("\\u")) != -1) {
            StringBuilder temp = new StringBuilder(line);
            String value = temp.substring(index+2, index+6);
            char aChar = (char)Integer.parseInt(value, 16);
            String unicodeChar = "" + aChar;
            temp.replace(index, index+6, unicodeChar);
            line = temp.toString();
        }

        return line;
    }
}
