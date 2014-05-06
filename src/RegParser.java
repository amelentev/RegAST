import java.util.*;
import java.util.function.IntConsumer;
import java.util.regex.PatternSyntaxException;

public class RegParser {
    /** Parse regular expression from string to AST. O(re.length()). thread-safe
     * Syntax is standard:
     * . - any symbol
     * ? - optional
     * * - 0 or more repetition
     * + - one or more repetition
     * | - alternation
     * () - grouping
     */
    public static RegAST parse(String re) {
        int nalt, natom; // number of alternations and number of atoms (needs to be Seq).
        final Deque<State> paren = new ArrayDeque<>(); // stacks
        final Deque<RegAST> ast = new ArrayDeque<>();
        nalt = natom = 0;
        final IntConsumer doseq = (na) -> { // Seq of na atoms
            if (na > 1) {
                Deque<RegAST> lst = new ArrayDeque<>();
                for (int i = 0; i < na; i++)
                    lst.addFirst(ast.pop()); // reverse stack
                List<RegAST> lst2 = extractStrings(lst);
                //List<RegAST> lst2 = new ArrayList<>(lst);
                //ast.push(RegAST.balanceSeq(lst2));
                ast.push(new RegAST.SeqSmartList(lst2));
            }
        };
        final IntConsumer doalt = (n) -> { // Alt n times
            if (n>0) {
                LinkedList<RegAST> lst = new LinkedList<>();
                for (int i = 0; i < n+1; i++)
                    lst.addFirst(ast.pop());
                ast.push(new RegAST.AltList(lst));
            }
        };
        for (int i = 0; i < re.length(); i++) {
            switch (re.charAt(i)) {
                case '(':
                    paren.push(new State(nalt, natom));
                    nalt = natom = 0;
                    break;
                case ')':
                    if (paren.isEmpty())
                        throw new PatternSyntaxException("unmatched )", re, i);
                    if (natom == 0) // () or (a|)
                        ast.push(RegAST.eps);
                    doseq.accept(natom);
                    doalt.accept(nalt);
                    State s = paren.pop();
                    nalt = s.nalt;
                    natom = s.natom;
                    natom++;
                    break;
                case '|':
                    if (natom == 0) // (|a)
                        ast.push(RegAST.eps);
                    doseq.accept(natom);
                    natom = 0;
                    nalt++;
                    break;
                case '*':
                    if(natom == 0)
                        throw new PatternSyntaxException("nothing to *", re, i);
                    ast.push(new RegAST.Rep(ast.pop()));
                    break;
                case '+':
                    if(natom == 0)
                        throw new PatternSyntaxException("nothing to +", re, i);
                    ast.push(new RegAST.Rep1(ast.pop()));
                    break;
                case '?':
                    if(natom == 0)
                        throw new PatternSyntaxException("nothing to ?", re, i);
                    ast.push(new RegAST.Alt(ast.pop(), RegAST.eps));
                    break;
                case '.': // any symbol
                    ast.push(new RegAST.AnySym());
                    natom++;
                    break;
                case '\\': // escaping
                    if (i+1 >= re.length())
                        throw new PatternSyntaxException("escape at end", re, i);
                    i++;
                    final char r;
                    switch (re.charAt(i)) {
                        case 'n': r = '\n'; break;
                        case 'r': r = '\r'; break;
                        case 't': r = '\t'; break;
                        default: r = re.charAt(i);
                    }
                    ast.push(new RegAST.Sym(r));
                    natom++;
                    break;
                default: // specific symbol
                    ast.push(new RegAST.Sym(re.charAt(i)));
                    natom++;
                    break;
            }
        }
        if (!paren.isEmpty())
            throw new PatternSyntaxException("unmatched (", re, 0);
        doseq.accept(natom);
        doalt.accept(nalt);
        assert (ast.size()==1);
        return ast.pop();
    }
    private static class State {
        final int nalt, natom;
        State(int nalt, int natom) {
            this.nalt = nalt;
            this.natom = natom;
        }
    }

    /** concat consecutive Sym to Str */
    private static List<RegAST> extractStrings(Collection<RegAST> lst) {
        StringBuilder sb = new StringBuilder();
        List<RegAST> res = new ArrayList<>();
        for (RegAST a : lst) {
            if (a instanceof RegAST.Sym)
                sb.append(((RegAST.Sym)a).c);
            else {
                if (sb.length()>0) {
                    res.add(RegAST.newStr(sb));
                    sb.setLength(0);
                }
                res.add(a);
            }
        }
        if (sb.length() > 0)
            res.add(RegAST.newStr(sb));
        return res;
    }
}
