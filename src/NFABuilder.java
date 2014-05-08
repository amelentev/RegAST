import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

public class NFABuilder implements RegAST.IntVisitor {
    private final StringBuilder chars = new StringBuilder();
    private final List<TIntList> epsilons = new ArrayList<>();
    private NFABuilder() { epsilons.add(null); }

    private void append(char c) {
        chars.append(c);
        epsilons.add(null);
    }
    private void epsEdge(int from, int to) {
        if (from==to) return;
        if (epsilons.get(from) == null)
            epsilons.set(from, new TIntArrayList());
        epsilons.get(from).add(to);
    }
    @Override public int sym(int st, char c) {
        int end = chars.length();
        append(c);
        epsEdge(st, end);
        return end+1;
    }
    @Override public int any(int st) {
        int end = chars.length();
        append(NFA.anyChar);
        epsEdge(st, end);
        return end+1;
    }
    @Override public int alt(int st, RegAST... es) {
        TIntList lst = new TIntArrayList();
        for (int i = 0; i < es.length; i++) {
            lst.add(es[i].visit(st, this));
            if (i+1<es.length)
                append('|');
        }
        int end = lst.max();
        for (int i = 0; i < lst.size(); i++) {
            epsEdge(lst.get(i), end);
        }
        return end;
    }
    @Override public int seq(int st, RegAST... es) {
        for (RegAST e : es)
            st = e.visit(st, this);
        return st;
    }
    @Override public int rep1(int st, RegAST r) {
        int end = r.visit(st, this);
        epsEdge(end, st);
        return end;
    }
    @Override public int eps(int d) { return d; }

    private static int[][] compress(List<TIntList> llst) {
        return llst.stream().map((lst) -> lst == null ? null : lst.toArray()).toArray(int[][]::new);
    }

    public static NFA buildNFA(RegAST re) {
        NFABuilder b = new NFABuilder();
        int endstate = re.visit(0, b);
        int[][] e = compress(b.epsilons);
        return new NFA(b.chars.toString(), e, endstate);
    }
}
