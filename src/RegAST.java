import java.util.List;
import java.util.stream.Collectors;

/**
 * Regular expression AST.
 * Can be constructed via RegParser#parse.
 * string matching via #match method.
 * public methods are thread-safe.
 * <p> use example: RegParser.parse(regexp).match(input);
 */
public abstract class RegAST implements Cloneable {
/* Implementation details:
   It must be a Tree, not DAG! node reuse prohibited(except Eps). use clone().
   Problem: stack overflow on regexps of big depth (~10K on default stack size). can be partially fixed by flattering & ast optimizations. or tail call optimization.

   AST has mutable state (canFinal and active fields). It can be removed:
     1) Make canFinal and active be ThreadLocal so every thread could have separate state. But performance will be poor.
     2) Make AST immutable: Extract all state fields to separate state class (-2 boolean).
        Add id/key field to AST nodes for index the state (+1 int). Performance unclear.
*/
    /** can the expression match empty string */
    final protected boolean canEmpty;

    protected RegAST(boolean canEmpty) { this.canEmpty = canEmpty; }

    // Mutable state fields.
    /** accept current string? */
    protected boolean canFinal = false,
    /** have the expression canFinal==true somewhere inside? */
                      active = false;

    /** @return is string s matches the regular expression. <p>
     *  running time = O(n*m) where n = s.length(), m = regexp size.
     *  O(m) additional memory used. thread-safe. */
    public boolean match(String s) {
        if ("".equals(s)) return canEmpty;
        RegAST r = this.clone(); // clone entire ast. O(m). no big deal, we need O(m) for state anyway.
        r.shift(true, s.charAt(0)); // transition from starting state
        for (int i = 1; i < s.length(); i++)
            r.shift(false, s.charAt(i));
        return r.canFinal;
    }

    /** Make transition in AST-NFA on char c.
     *  @param st is starting state */
    protected abstract void shift(boolean st, char c);
    protected abstract RegAST clone();

    /** RegAST with avoiding unnecessary shifts */
    public static abstract class ARegAST extends RegAST {
        protected ARegAST(boolean canEmpty) { super(canEmpty); }
        @Override protected void shift(boolean st, char c) {
            if (active || st) // avoid unnecessary steps
                step(st, c);
        }
        /** Make actual transition. for use only inside #shift */
        protected abstract void step(boolean st, char c);
    }

    /** match empty string */
    static class Eps extends ARegAST {
        private Eps() { super(true); }
        @Override protected void step(boolean st, char c) {}
        @Override protected Eps clone() { return this; }
        @Override public String toString() { return "@"; }
    }
    static final Eps eps = new Eps(); // singleton Eps

    /** match one symbol */
    static class Sym extends ARegAST {
        private final char c;
        Sym(char c) {
            super(false);
            this.c = c;
        }
        @Override protected void step(boolean st, char c) {
            active = canFinal = st && c==this.c;
        }
        @Override protected Sym clone() { return new Sym(c); }
        final static String escapeSymbols = "*.+@";
        @Override public String toString() { return (escapeSymbols.indexOf(c)>=0 ? "\\"+c : c) + (canFinal?"`":""); }
    }

    /** match any symbol */
    static class AnySym extends ARegAST {
        AnySym() { super(false); }
        @Override protected void step(boolean st, char c) {
            active = canFinal = st;
        }
        @Override protected AnySym clone() { return new AnySym(); }
        @Override public String toString() { return "."; }
    }
    // TODO: match symbol group eg: [a-z]. class SymGroup { Predicate<Character> f }

    /** Either p or q */
    static class Alt extends ARegAST {
        private final RegAST p, q;
        Alt(RegAST p, RegAST q) {
            super(p.canEmpty || q.canEmpty);
            this.p = p;
            this.q = q;
        }
        @Override protected void step(boolean st, char c) {
            p.shift(st, c); q.shift(st, c);
            canFinal = p.canFinal || q.canFinal;
            active = p.active || q.active;
        }
        @Override protected Alt clone() {
            return new Alt(p.clone(), q.clone());
        }
        @Override public String toString() {
            return "("+p.toString() + "|" + q.toString() + ")";
        }
    }
    /** Either one of list */
    static class AltList extends ARegAST {
        private final List<RegAST> lst;
        AltList(List<RegAST> lst) {
            super(lst.stream().anyMatch(r -> r.canEmpty));
            this.lst = lst;
        }
        @Override protected void step(boolean st, char c) {
            active = canFinal = false;
            for (RegAST a : lst) {
                a.shift(st, c);
                canFinal |= a.canFinal;
                active |= a.active;
            }
        }
        @Override protected AltList clone() {
            return new AltList(lst.stream().map(RegAST::clone).collect(Collectors.toList()));
        }
        @Override public String toString() {
            return "("+ lst.stream().map(Object::toString).collect(Collectors.joining("|"))+")";
        }
    }
    /** Sequence p then q */
    static class Seq extends ARegAST {
        private final RegAST p, q;
        Seq(RegAST p, RegAST q) {
            super(p.canEmpty && q.canEmpty);
            this.p = p;
            this.q = q;
        }
        @Override protected void step(boolean st, char c) {
            boolean m2 = st && p.canEmpty || p.canFinal;
            p.shift(st, c);
            q.shift(m2, c);
            canFinal = p.canFinal && q.canEmpty || q.canFinal;
            active = p.active || q.active;
        }
        @Override protected RegAST clone() {
            return new Seq(p.clone(), q.clone());
        }
        @Override public String toString() {
            return p.toString() + q.toString();
        }
    }
    /** Sequence of >1 regexps */
    static class SeqList extends ARegAST {
        private final List<RegAST> lst;
        SeqList(List<RegAST> lst) {
            super(lst.stream().allMatch(r -> r.canEmpty));
            this.lst = lst;
        }
        @Override protected void step(boolean st, char c) {
            active = canFinal = false;
            for (RegAST a : lst) {
                boolean nextst = st && a.canEmpty || a.canFinal;
                a.shift(st, c);
                st = nextst;
                canFinal = canFinal && a.canEmpty || a.canFinal;
                active |= a.active;
            }
        }
        @Override protected SeqList clone() {
            return new SeqList(lst.stream().map(RegAST::clone).collect(Collectors.toList()));
        }
        @Override public String toString() {
            return lst.stream().map(Object::toString).collect(Collectors.joining());
        }
    }
    /** Repetition of r any times (including 0).
     *  Can be replaced by Alt(eps, Rep1(r)) */
    static class Rep extends ARegAST {
        final RegAST r;
        public Rep(RegAST r) {
            super(true);
            this.r = r;
        }
        @Override protected void step(boolean st, char c) {
            r.shift(st || r.canFinal, c);
            canFinal = r.canFinal;
            active = canFinal || r.active;
        }
        @Override protected Rep clone() {
            return new Rep(r.clone());
        }
        @Override public String toString() {
            String s = r.toString();
            return r instanceof Alt || r instanceof AltList || s.length()==1 ? s+"*" : "("+s+")*";
        }
    }
    /** Repetition of r  >=1 times. */
    static class Rep1 extends ARegAST {
        final RegAST r;
        public Rep1(RegAST r) {
            super(r.canEmpty);
            this.r = r;
        }
        @Override protected void step(boolean st, char c) {
            r.shift(st || r.canFinal, c);
            canFinal = r.canFinal;
            active = canFinal || r.active;
        }
        @Override protected Rep1 clone() {
            return new Rep1(r.clone());
        }
        @Override public String toString() {
            String s = r.toString();
            return r instanceof Alt || r instanceof AltList || s.length()==1 ? s+"+" : "("+s+")+";
        }
    }
}
