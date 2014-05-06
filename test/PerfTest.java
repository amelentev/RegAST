import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PerfTest {
    @Test
    public void performance1() {
        System.out.println("Matching a{n} with (a?){n}a{n}. n = 5000");
        int n = 5000;
        String inp = RegASTTest.genA(n).toString();
        { // mutable AST, no str
            List<RegAST> lst = new ArrayList<>();
            for (int i = 0; i < n; i++)
                lst.add(new RegAST.Alt(new RegAST.Sym('a'), RegAST.eps));
            for (int i = 0; i < n; i++)
                lst.add(new RegAST.Sym('a'));
            run("mutable AST, no Str, SeqList", true, new RegAST.SeqList(lst), inp);
            run("mutable AST, no Str, balanced Seq", true, RegAST.balanceSeq(lst), inp);
            run("mutable AST, no Str, SeqSmartList", true, new RegAST.SeqSmartList(lst), inp);
        }
        { // mutable AST, str
            List<RegAST> lst = new ArrayList<>();
            for (int i = 0; i < n; i++)
                lst.add(new RegAST.Alt(new RegAST.Sym('a'), RegAST.eps));
            lst.add(new RegAST.Str(RegASTTest.genA(n)));
            run("mutable AST, Str, SeqList", true, new RegAST.SeqList(lst), inp);
            run("mutable AST, Str, balanced Seq", true, RegAST.balanceSeq(lst), inp);
            run("mutable AST, Str, SeqSmartList", true, new RegAST.SeqSmartList(lst), inp);
        }
        { // immutable AST, no str
            RegAST2.Builder b = new RegAST2.Builder();
            List<RegAST2> lst = new ArrayList<>();
            for (int i = 0; i < n; i++)
                lst.add(b.newAlt(b.newSym('a'), b.eps));
            for (int i = 0; i < n; i++)
                lst.add(b.newSym('a'));
            run("immutable AST, no Str, balanced Seq", true, b.balanceSeq(lst), inp);
        }
    }/*
mutable AST, no Str, SeqList:	356.8
mutable AST, no Str, balanced Seq:	628.7
mutable AST, no Str, SeqSmartList:	612.1
mutable AST, Str, SeqList:	479.5
mutable AST, Str, balanced Seq:	497.4
mutable AST, Str, SeqSmartList:	504.6
immutable AST, no Str, balanced Seq:	1329.4
grep -E '(a?){5000}a{5000}' - hang
google re2 - 4919 */

    // a{n}
    @Test public void performance2() {
        System.out.println("Matching a{n} with a{n}. n = 10^6");
        int n = 1000000;
        String s = RegASTTest.genA(n);
        List<RegAST> lst = new ArrayList<>();
        for (int i = 0; i < n; i++)
            lst.add(new RegAST.Sym('a'));
        //run("SeqList, no Str", true, new RegAST.SeqList(lst), s); hang, 10*n^2 = 10^13 ops
        run("Balanced, no Str", true, RegAST.balanceSeq(lst), s);
        run("SeqSmartList, no Str", true, new RegAST.SeqSmartList(lst), s);
        run("Str", true, new RegAST.Str(s), s);
    }/*
Balanced, no Str:	256.4
SeqSmartList, no Str:	134.3
Str:	30.3*/

    void run(String msg, boolean expected, RegExp re, String inp) {
        final int m = 10;
        System.gc();
        long time = System.currentTimeMillis();
        for (int i = 0; i < m; i++)
            assertEquals(expected, re.match(inp));
        time = System.currentTimeMillis() - time;
        System.out.printf("%s:\t%.1f\n", msg, ((double)time)/m);
    }
}
