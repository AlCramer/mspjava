package msp.util;
import java.util.*;

/** (Python-style) extensible list of int's. */
public class ILst {
    public int[] a = new int[128];
    public int N = 0;
    public ILst() {
        super();
        a = new int[128];
    }
    public ILst(int size) {
        super();
        N = size;
        a = new int[size];
    }
    public ILst(int size, int vinit) {
        super();
        N = size;
        a = new int[size];
        for (int i=0; i<size; i++) {
            a[i] = vinit;
        }
    }
    public void append(int v) {
        if ((N+1) > a.length) {
            a = Arrays.copyOf(a, 2*a.length);
        }
        a[N++] = v;
    }
    // insert "v" immediately before element at "ix".
    public void insert(int ix, int v) {
        int[] dst = new int[(N+1) > a.length? 2*a.length : a.length];
        if (ix > 0) {
            System.arraycopy(a, 0, dst, 0, ix);
        }
        dst[ix] = v;
        int ntail = N-ix;
        if (ntail > 0) {
            System.arraycopy(a, ix, dst, ix+1, ntail);
        }
        a = dst;
        N++;
    }
    public void extend(ILst delta) {
        if ((N + delta.N) > a.length) {
            a = Arrays.copyOf(a, N+delta.N);
        }
        System.arraycopy(delta.a, 0, a, N, delta.N);
        N += delta.N;
    }
    public void reverse() {
        for(int i = 0; i < N / 2; i++) {
            int temp = a[i];
            a[i] = a[N - i - 1];
            a[N - i - 1] = temp;
        }
    }
    /**
    * Get sublist: S and E follow Python slice conventions.
    * "x.slice(0, 0)" returns a clone of "x".
    */
    public ILst slice(int S, int E) {
        ILst s = new ILst();
        int hi = E;
        if (E <= 0) {
            hi = N + E;
        }
        for (int i=S; i<hi; i++) {
            s.append(a[i]);
        }
        return s;
    }
    public ILst clone() {
        return slice(0, 0);
    }
    public boolean contains(int v) {
        for (int i=0; i<N; i++) {
            if (a[i] == v) {
                return true;
            }
        }
        return false;
    }
    public void sort() {
        for (int i=0; i<N-1; i++) {
            for (int j=i+1; j<N; j++) {
                if (a[j]< a[i]) {
                    int tmp = a[i];
                    a[i] = a[j];
                    a[j] = tmp;
                }
            }
        }
    }
    
    public boolean equals(ILst lx) {
        if ((lx == null) || (N != lx.N)) {
            return false;
        }
        for (int i=0; i<N; i++) {
            if (a[i] != lx.a[i]) {
                return false;
            }
        }
        return true;
    }
    public ILstIterator getIterator() {
        return new ILstIterator(this);
    }
    public String toString() {
        if (N == 0) {
            return "[]";
        }
        SLst l = new SLst();
        for (int i=0; i<N; i++) {
            l.append(Integer.toString(a[i]));
        }
        return "[" + l.join(", ") + "]";
    }
}




