import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class GenTest {
    static final String chars = "ab"; //|*+.?()";
    /** QuickCheck-like RegAST generator */
    static class RegASTGenerator {
        final Random r = new Random();
        private List<RegAST> genList(int n, int m) {
            List<RegAST> lst = new ArrayList<>();
            for (int i = 0; i < n; i++)
                lst.add(next(m));
            return lst;
        }
        RegAST next(int m) {
            if (m==0)
                switch (r.nextInt(3)) {
                    case 0: return RegAST.eps;
                    case 1: return new RegAST.Sym(chars.charAt(r.nextInt(chars.length())));
                    default: return new RegAST.AnySym();
                }
            else
                switch (r.nextInt(5)) {
                    case 0: return new RegAST.Rep(next(m-1));
                    case 1: return new RegAST.Rep1(next(m-1));
                    case 2: return new RegAST.Alt(next(m/2), next(m/2));
                    case 3: return new RegAST.AltList(genList(3, m/3));
                    case 4: return new RegAST.Seq(next(m/2), next(m/2));
                    default: return new RegAST.SeqList(genList(3, m/3));
                }
        }
    }
    static class InputGenerator {
        final Random r = new Random();
        String next(int len) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++)
                sb.append(chars.charAt(r.nextInt(chars.length())));
            return sb.toString();
        }
    }

    void check(RegAST re, NFA nfa, String inp) {
        boolean r1 = re.match(inp);
        boolean r2 = nfa.match(inp);
        if (r1 != r2) {
            System.out.println(re.toString());
            System.out.println(inp);
            assertEquals(r1, r2);
        }
    }

    @Test public void genTests() {
        RegASTGenerator reg = new RegASTGenerator();
        InputGenerator ing = new InputGenerator();
        for (int m = 0; m < 40; m++) {
            for (int _i = 0; _i < 10; _i++) {
                RegAST re = reg.next(m);
                NFA nfa = NFABuilder.buildNFA(re);
                check(re, nfa, "");
                for (int len = 1; len <= (m+1)*3; len++)
                    for (int _j = 0; _j < 10; _j++)
                        check(re, nfa, ing.next(len));
            }
        }
    }

    @Test public void tests() {
        RegASTTest.check(false, "(((.|b|)|a+)|(a|a*)|b+a+)", "bbaba");
        RegASTTest.check(true, "(.|(.|(|b))*)", "baabababbbabaabbaabbabbbbbabaaaabbaabbbbbababa");
        RegASTTest.check(false, "((\\||)*|.)", "|b");
        RegASTTest.check(false, "((((\\?\\()*)*|(|((((.|(\\?)*)+|\\).(|.*))(|))*..)+))a.+)*", "*+");
        RegASTTest.check(false, "(\\)*(((.|(b)+)b*\\)\\.(.*)+b)+)+)*", "+?b(+|*?+?b)|a..+)..*+b)ba|?(b+*?+++ba.+(+a+(b*)??)?).+)||+|b+()*(b?.?|b)a?.aa(+a|*+++(*)|.?*a*|b)a*+?*+(()))(((aaa)a?)+a)a|+?(ba*)*?*.+a..+(.|ab)*||b(a)(b*))b+a+aa(b?a)|(||?b*?(*..?|*b)**.(a.a+)b?*(*(++((ab+|a)|a?)b(.+()a|.+a+b|?a(((?.+)*()a((.)+b(??*(+a.+.a.*?aa?*)(a|*?.*|.?|+..**b)(*)b*)|*)b(.)()?+|.?|+*)?||((+.**)))+++a|.?(++(?b|.b?b.**a)?(.b|.bb.bbb))|aaa.*.ab.|?|+)|*|?||a)*)+).?)ba|.*+?a?()ab+*)?.??).).b+(?a?(+)+a??ab(b*.|a?*");
    }
}
