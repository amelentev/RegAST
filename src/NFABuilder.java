import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

public class NFABuilder implements RegAST.IntVisitor {
    /** @see NFA#chars */
    private final TIntList chars = new TIntArrayList();
    private final List<TIntList> epsilons = new ArrayList<>();
    private NFABuilder() { epsilons.add(null); }

    // invariants:
    // 1) chars.size()+1 == epsilons.size()
    // 2) for any RegAST (except eps), end state will be chars.size() == epsilons.size()-1
    // 3) no backward eps edges at end states.

    private void append(int c) {
        chars.add(c);
        epsilons.add(null);
    }
    private void epsEdge(int from, int to) {
        if (from==to) return;
        if (epsilons.get(from) == null)
            epsilons.set(from, new TIntArrayList());
        epsilons.get(from).add(to);
    }
    // st - start state, return end state
    @Override public int sym(int st, char c) {
        epsEdge(st, chars.size());
        append(c);
        return chars.size();
    }
    @Override public int any(int st) {
        epsEdge(st, chars.size());
        append(NFA.anyChar);
        return chars.size();
    }
    @Override public int alt(int st, RegAST... es) {
        int[] ends = new int[es.length];
        for (int i = 0; i < es.length; i++) {
            append(NFA.noChar);
            ends[i] = es[i].visit(st, this);
        }
        for (int e : ends)
            epsEdge(e, chars.size());
        return chars.size();
    }
    @Override public int seq(int st, RegAST... es) {
        for (RegAST e : es)
            st = e.visit(st, this);
        return st;
    }
    @Override public int rep1(int st, RegAST r) {
        int st1 = chars.size();
        epsEdge(st, st1);
        int end = r.visit(st1, this);
        epsEdge(end, st1);
        append(NFA.noChar);
        epsEdge(end, chars.size());
        return chars.size();
    }
    @Override public int eps(int d) { return d; }

    private static int[][] compress(List<TIntList> llst) {
        return llst.stream().map((lst) -> lst == null ? null : lst.toArray()).toArray(int[][]::new);
    }

    public static NFA buildNFA(RegAST re) {
        NFABuilder b = new NFABuilder();
        re.visit(0, b);
        int[][] e = compress(b.epsilons);
        return new NFA(b.chars.toArray(), e);
    }
}
