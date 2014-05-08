import java.util.BitSet;
import java.util.List;

/** Immutable version of RegAST. State extracted to separate class State. */
public abstract class RegAST2 implements RegExp {
    final protected boolean canEmpty;
    final protected int id;
    protected RegAST2(int id, boolean canEmpty) { this.id = id; this.canEmpty = canEmpty; }

    private static class State {
        final BitSet bs = new BitSet();
        boolean active(RegAST2 u) { if (u.id<0) return false; else return bs.get(u.id); }
        boolean canFinal(RegAST2 u) { if (u.id<0) return false; else return bs.get(u.id+1); }
        void setActive(int id, boolean v) { bs.set(id, v); }
        void setCanFinal(int id, boolean v) { bs.set(id+1, v); }
    }

    public boolean match(String s) {
        if ("".equals(s)) return canEmpty;
        State state = new State();
        shift(state, true, s.charAt(0)); // transition from starting state
        for (int i = 1; i < s.length() && state.active(this); i++)
            shift(state, false, s.charAt(i));
        return state.canFinal(this);
    }

    protected abstract void shift(State state, boolean st, char c);

    private static abstract class ARegAST extends RegAST2 {
        protected ARegAST(int id, boolean canEmpty) { super(id, canEmpty); }
        @Override protected void shift(State state, boolean st, char c) {
            if (st || state.active(this)) // avoid unnecessary steps
                step(state, st, c);
        }
        /** Make actual transition. for use only inside #shift */
        protected abstract void step(State state, boolean st, char c);
    }

    /** match empty string */
    static class Eps extends ARegAST {
        private Eps() { super(-1, true); }
        @Override protected void step(State state, boolean st, char c) {}
        @Override public String toString() { return "@"; }
    }

    /** match one symbol */
    static class Sym extends ARegAST {
        final char c;
        Sym(int id, char c) {
            super(id, false);
            this.c = c;
        }
        @Override protected void step(State state, boolean st, char c) {
            boolean v = st && c==this.c;
            state.setCanFinal(id, v);
            state.setActive(id, v);
        }
        final static String escapeSymbols = "*.+@";
        @Override public String toString() { return (escapeSymbols.indexOf(c)>=0 ? "\\"+c : ""+c); }
    }

    /** match any symbol */
    static class AnySym extends ARegAST {
        AnySym(int id) { super(id, false); }
        @Override protected void step(State state, boolean st, char c) {
            state.setCanFinal(id, st);
            state.setActive(id ,st);
        }
        @Override public String toString() { return "."; }
    }

    /** Either p or q */
    static class Alt extends ARegAST {
        private final RegAST2 p, q;
        Alt(int id, RegAST2 p, RegAST2 q) {
            super(id, p.canEmpty || q.canEmpty);
            this.p = p;
            this.q = q;
        }
        @Override protected void step(State state, boolean st, char c) {
            p.shift(state, st, c); q.shift(state, st, c);
            state.setCanFinal(id, state.canFinal(p) || state.canFinal(q));
            state.setActive(id, state.active(p) || state.active(q));
        }
        @Override public String toString() {
            return "("+p.toString() + "|" + q.toString() + ")";
        }
    }
    /** Sequence p then q */
    static class Seq extends ARegAST {
        private final RegAST2 p, q;
        Seq(int id, RegAST2 p, RegAST2 q) {
            super(id, p.canEmpty && q.canEmpty);
            this.p = p;
            this.q = q;
        }
        @Override protected void step(State state, boolean st, char c) {
            boolean m2 = st && p.canEmpty || state.canFinal(p);
            p.shift(state, st, c);
            q.shift(state, m2, c);
            state.setCanFinal(id, state.canFinal(p) && q.canEmpty || state.canFinal(q));
            state.setActive(id, state.active(p) || state.active(q));
        }
        @Override public String toString() {
            return p.toString() + q.toString();
        }
    }
    /** Repetition of r any times (including 0).
     *  Can be replaced by Alt(eps, Rep1(r)) */
    static class Rep extends ARegAST {
        final RegAST2 r;
        Rep(int id, RegAST2 r) {
            super(id, true);
            this.r = r;
        }
        @Override protected void step(State state, boolean st, char c) {
            r.shift(state, st || state.canFinal(r), c);
            state.setCanFinal(id, state.canFinal(r));
            state.setActive(id, state.active(r));
        }
        @Override public String toString() {
            String s = r.toString();
            return r instanceof Alt || s.length()==1 ? s+"*" : "("+s+")*";
        }
    }
    /** Repetition of r  >=1 times. */
    static class Rep1 extends ARegAST {
        final RegAST2 r;
        Rep1(int id, RegAST2 r) {
            super(id, r.canEmpty);
            this.r = r;
        }
        @Override protected void step(State state, boolean st, char c) {
            r.shift(state, st || state.canFinal(r), c);
            state.setCanFinal(id, state.canFinal(r));
            state.setActive(id, state.active(r));
        }
        @Override public String toString() {
            String s = r.toString();
            return r instanceof Alt || s.length()==1 ? s+"+" : "("+s+")+";
        }
    }

    /** not thread safe */
    static class Builder {
        int curId = -2;
        private void incid() { curId += 2; }
        Sym newSym(char c) { incid(); return new Sym(curId, c); }
        AnySym newAnySym() { incid(); return new AnySym(curId); }
        Alt newAlt(RegAST2 p, RegAST2 q) { incid(); return new Alt(curId, p, q); }
        Seq newSeq(RegAST2 p, RegAST2 q) { incid(); return new Seq(curId, p, q); }
        Rep newRep(RegAST2 r) { incid(); return new Rep(curId, r); }
        Rep1 newRep1(RegAST2 r) { incid(); return new Rep1(curId, r); }
        RegAST2 balanceSeq(List<RegAST2> lst) { return Util.balance(lst, this::newSeq); }
        static final Eps eps = new Eps(); // singleton Eps
    }
}
