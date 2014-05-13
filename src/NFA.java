import gnu.trove.list.array.TIntArrayList;

/** Immutable NFA representing RegExp */
public class NFA implements RegExp {
    /** char transitions: >=0 - char codePoint, -1 - any char, -2 - no char match */
    private final int[] chars;
    static final int anyChar = -1;
    static final int noChar = -2;

    private final int[][] epsilons; // epsilon transitions
    NFA(int[] chars, int[][] epsilons) {
        this.chars = chars;
        this.epsilons = epsilons;
    }

    public boolean match(String input) {
        int M = epsilons.length;
        int endState = M-1;
        boolean mark[] = new boolean[M];
        boolean nextMark[] = new boolean[M];
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
                if (s == endState) continue;
                if (!nextMark[s+1] && (chars[s]==anyChar || input.codePointAt(ind) == chars[s])) {
                    nextMark[s+1] = true;
                    nextStates.add(s+1);
                }
            }
            states.resetQuick();
            // swap states and marks
            TIntArrayList t = states; states = nextStates; nextStates = t;
            boolean[] bt = mark; mark = nextMark; nextMark = bt;
        }
        return !states.isEmpty() && mark[endState];
    }
}
