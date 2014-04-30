import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegASTTest {
    private void check(boolean e, RegAST re, String inp) {
        assertEquals(e, re.match(inp));
    }
    private void check(boolean e, String re, String inp) {
        assertEquals(e, RegParser.parse(re).match(inp));
    }

    @Test
    public void testEvenCs() {
        RegAST evencs = getEvenCs();
        check(true, evencs, "");
        check(true, evencs, "a");
        check(true, evencs, "ba");
        check(false, evencs, "c");
        check(false, evencs, "acb");
        check(true, evencs, "cc");
        check(true, evencs, "acc");
        check(false, evencs, "accc");
    }

    private RegAST getEvenCs() {
        RegAST nocs = new RegAST.Rep(new RegAST.Alt(new RegAST.Sym('a'), new RegAST.Sym('b')));
        RegAST onec = new RegAST.Seq(nocs.clone(), new RegAST.Sym('c'));
        return new RegAST.Seq(new RegAST.Rep(new RegAST.Seq(onec.clone(), onec.clone())), nocs.clone());
    }

    @Test public void testPlus() {
        RegAST re = RegParser.parse("a(b+c)+");
        check(false, re, "a");
        check(false, re, "ab");
        check(true, re, "abc");
        check(false, re, "ac");
        check(false, re, "abcb");
        check(true, re, "abcbc");
        check(true, re, "abbcbc");
        check(false, re, "abccbc");
        check(true, re, "abbcbbc");
    }

    @Test public void testDot() {
        check(false, ".", "");
        check(true, ".", "a");
        check(false, ".", "aa");
        check(true, ".*", "");
        check(true, ".*", "qwe");
        check(false, "a*.", "aaaqe");
        check(true, "a*.", "e");
        check(true, "a*.", "a");
        check(true, "a*.", "aaaq");
        check(false, "a*.", "");
    }

    @Test public void testAlt() {
        check(true, "a(|b)a", "aba");
        check(true, "a(|b)a", "aa");
        check(true, "a(b|)a", "aba");
        check(true, "a(b|)a", "aa");
        check(false, "a(b|)a", "aaa");
        check(false, "a(b|)a", "a");
    }

    @Test public void testParen() {
        check(true, "a()a", "aa");
        check(false, "a()a", "aba");
    }

    // (a?){n}a{n}
    @Test public void testAonAn() {
        int n = 100;
        StringBuilder re = genAQ(n);
        StringBuilder inp = genA(n);
        re.append(inp.toString());
        check(true, re.toString(), inp.toString());
        //assertTrue(inp.toString().matches(re.toString())); // hang. exponential time in j.u.regex
        for (int i = 0; i < n; i++) {
            inp.append('a');
            check(true, re.toString(), inp.toString());
        }
        inp.append('a');
        check(false, re.toString(), inp.toString());
    }
    @Test public void performance1() {
        int n = 5000;
        StringBuilder re = genAQ(n);
        StringBuilder inp = genA(n);
        re.append(inp.toString());
        long time = System.currentTimeMillis();
        check(true, re.toString(), inp.toString());
        System.out.println("our:" + (System.currentTimeMillis() - time)); // ~0.7s in 2GHz i7.
        // grep -E '(a?){5000}a{5000}' - hang
        // google re2 - 4.919s
    }
    StringBuilder genAQ(int n) {
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < n; i++) re.append("a?");
        return re;
    }
    StringBuilder genA(int n) {
        StringBuilder inp = new StringBuilder();
        for (int i = 0; i < n; i++) inp.append('a');
        return inp;
    }

    @Test public void parallelTest() throws ExecutionException, InterruptedException {
        String sre = "((a|b)*c(a|b)*c)*(a|b)*";
        Pattern p = Pattern.compile(sre);
        RegAST re = RegParser.parse(sre);
        Callable<Boolean> task = () -> {
            String s = genrnd(1000);
            boolean our = re.match(s);
            boolean exp = p.matcher(s).matches(); // >=10000 - stack overflow in j.u.regexp
            return exp == our;
        };
        List<Future<Boolean>> lst = new ArrayList<>();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int i=0; i<1000; i++)
            lst.add(pool.submit(task));
        for (Future<Boolean> f : lst)
            assertTrue(f.get());
    }

    /** generete random string in alphabet {a,b,c} */
    String genrnd(int n) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < n; i++)
            sb.append((char)('a' + r.nextInt(3)));
        return sb.toString();
    }
}
