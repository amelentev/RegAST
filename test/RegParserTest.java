import org.junit.Test;

import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.*;

public class RegParserTest {
    private void check(String exp, String re) {
        assertEquals(exp, RegParser.parse(re).toString());
    }

    @Test
    public void parseTest() {
        check("ab", "ab");
        check("ab*", "ab*");
        check("ab*c", "ab*c");
        check("(ab*|c)", "ab*|c");
        check("a(b*|c)", "a(b*|c)");
        check("a((b*|c)|)", "a(b*|c)?");
        check("((a|b)*c(a|b)*c)*(a|b)*", "((a|b)*c(a|b)*c)*(a|b)*");
        check("(qw)*e.zx\\.\\*cas\nd", "(qw)*e.zx\\.\\*cas\\nd");
        check("(a|b|c)", "a|b|c");
        check("(((a|)*)*|b)", "a?**|b");
        check("a(b+c)+", "a(b+c)+");
    }

    @Test(expected = PatternSyntaxException.class)
    public void exceptionTest1() {
        check("", "ab|(*)c");
    }
    @Test(expected = PatternSyntaxException.class)
    public void exceptionTest3() {
        check("", "c\\");
    }
    @Test(expected = PatternSyntaxException.class)
    public void exceptionTest4() {
        check("", "(asdasd");
    }
    @Test(expected = PatternSyntaxException.class)
    public void exceptionTest5() {
        check("", "a(sd)a)sd");
    }
}