package msp.util;
import java.util.*;

/** (Python-style) extensible list of Strings's. */
public class SLst {
    public String[] a = new String[128];
    public int N = 0;
    public SLst() {
        super();
        a = new String[128];
    }
    public SLst(int size) {
        super();
        N = size;
        a = new String[size];
    }
    public SLst(String[] ary) {
        super();
        N = ary.length;
        a = Arrays.copyOf(ary, N);
    }
    public void append(String v) {
        if ((N+1) > a.length) {
            a = Arrays.copyOf(a, 2*a.length);
        }
        a[N++] = v;
    }
    public void extend(SLst delta) {
        if ((N + delta.N) > a.length) {
            a = Arrays.copyOf(a, N+delta.N);
        }
        System.arraycopy(delta.a, 0, a, N, delta.N);
        N += delta.N;
    }
    public boolean contains(String s) {
        for (int i=0; i<N; i++) {
            if (a[i].equals(s)) {
                return true;
            }
        }
        return false;
    }
    public String join(String delim) {
        if (N == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(a[0]);
        for (int i=1; i<N; i++) {
            sb.append(delim);
            sb.append(a[i]);
        }
        return sb.toString();
    }
    public String[] toArray() {
        return Arrays.copyOf(a, N);
    }
    public boolean equals(SLst lx) {
        if ((lx == null) || (N != lx.N)) {
            return false;
        }
        if (N != lx.N) {
            return false;
        }
        for (int i=0; i<N; i++) {
            if (!a[i].equals(lx.a[i])) {
                return false;
            }
        }
        return true;
    }
    public SLstIterator getIterator() {
        return new SLstIterator(this);
    }
    public void print() {
        System.out.print("[");
        for (int i=0; i<N; i++) {
            System.out.print(a[i] + " ");
        }
        System.out.println("]");
    }
}




