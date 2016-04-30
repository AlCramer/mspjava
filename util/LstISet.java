package msp.util;
import java.util.*;

/** a list of [set of ints] */
public class LstISet {
    public Object[] a;
    public int N;
    public LstISet(int N) {
        super();
        this.N = N;
        a = new Object[N];
        for (int i=0; i<N; i++) {
            a[i] = new HashSet<Integer>();
        }
    }
    public void addElement(int ixlst, int v) {
        HashSet<Integer> s = (HashSet<Integer>)a[ixlst];
        s.add(v);
    }
    public boolean contains(int ixlst, int v) {
        HashSet<Integer> s = (HashSet<Integer>)a[ixlst];
        return s.contains(v);
    }
}




