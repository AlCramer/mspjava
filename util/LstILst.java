package msp.util;
import java.util.*;

/** List of list-of-int */
public class LstILst {
    public ILst[] a = new ILst[128];
    public int N = 0;
    public LstILst() {
        super();
        a = new ILst[128];
    }
    public LstILst(int size) {
        super();
        a = new ILst[size];
    }
    public void append(ILst v) {
        if ((N+1) > a.length) {
            a = Arrays.copyOf(a, 2*a.length);
        }
        a[N++] = v;
    }
    public boolean equals(LstILst lx) {
        if ((lx == null) || (N != lx.N)) {
            return false;
        }
        for (int i=0; i<N; i++) {
            if (a[i] == null) {
                if (lx.a[i] != null) {
                    return false;
                }
            } else if (!a[i].equals(lx.a[i])) {
                return false;
            }
        }
        return true;
    }
    public void print() {
        for (int i=0; i<N; i++) {
            System.out.println(String.format(
            "%d.%s", i, a[i].a.toString() ));
        }
    }
    
}




