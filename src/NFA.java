import gnu.trove.list.array.TIntArrayList;

/** Immutable NFA representing RegExp */
public class NFA implements RegExp {
    private final String chars;
    private final int[][] epsilons; // epsilon transitions
    private final int M; // end state
    NFA(String chars, int[][] epsilons, int M) {
        this.chars = chars;
        this.epsilons = epsilons;
        this.M = M;
    }

    public final static char anyChar = '\0';

    public boolean match(String input) {
        boolean mark[] = new boolean[M+1];
        boolean nextMark[] = new boolean[M+1];
        TIntArrayList states = new TIntArrayList();
        TIntArrayList nextStates = new TIntArrayList();
        mark[0] = true;
        states.add(0);
        for (int ind = 0; !states.isEmpty(); ind++) {
            // invariant: nextMark is clear
            // epsilon transitions
            for (int i = 0; i < states.size(); i++) {
                int s = states.get(i);
                if (epsilons[s]==null) continue;
                for (int e : epsilons[s]) {
                    if (!mark[e]) {
                        mark[e] = true;
                        states.add(e);
                    }
                }
            }
            if (ind >= input.length()) break;
            // transitions on chars
            for (int i = 0; i < states.size(); i++) {
                int s = states.get(i);
                mark[s] = false; // will be nextMark
                if (s==M) continue;
                if (!nextMark[s+1] && (chars.charAt(s)==anyChar || input.charAt(ind) == chars.charAt(s))) {
                    nextMark[s+1] = true;
                    nextStates.add(s+1);
                }
            }
            states.resetQuick();
            // swap states and marks
            TIntArrayList t = states; states = nextStates; nextStates = t;
            boolean[] bt = mark; mark = nextMark; nextMark = bt;
        }
        return !states.isEmpty() && mark[M];
    }
}
