import java.util.List;
import java.util.function.BinaryOperator;

public class Util {
    /** construct balanced binary tree from lst using newBiNode*/
    public static <T> T balance(List<T> lst, BinaryOperator<T> newBiNode) {
        int n = lst.size();
        assert n>0;
        if (n==1) return lst.get(0);
        return newBiNode.apply(balance(lst.subList(0, n / 2), newBiNode), balance(lst.subList(n / 2, n), newBiNode));
    }
}
