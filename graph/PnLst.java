package msp.graph;
import msp.lex.*;
import java.util.*;

/** (Python-style) extensible list of Pn's. */
public class PnLst {
    public Pn[] a;
    public int N = 0;
    public PnLst() {
        super();
        a = new Pn[16];
    }
    public PnLst(int capacity) {
        super();
        a = new Pn[capacity];
    }
    public PnLst(Pn pn) {
        super();
        a = new Pn[16];
        a[0] = pn;
        N = 1;
    }
    public void append(Pn v) {
        if ((N+1) > a.length) {
            a = Arrays.copyOf(a, 2*a.length);
        }
        a[N++] = v;
    }
    // insert "v" immediately before element at "ix".
    public void insert(int ix, Pn v) {
        Pn[] dst = new Pn[(N+1) > a.length? 2*a.length : a.length];
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
    public void extend(PnLst delta) {
        if ((N + delta.N) > a.length) {
            a = Arrays.copyOf(a, N+delta.N);
        }
        System.arraycopy(delta.a, 0, a, N, delta.N);
        N += delta.N;
    }
    public void remove(Pn v) {
        Pn[] dst = new Pn[N];
        int j = 0;
        for (int i=0; i<N; i++) {
            if (a[i] != v) {
                dst[j++] = a[i];
            }
        }
        a = dst;
        N--;
    }
    public int find(Pn v) {
        for (int i=0; i<N; i++) {
            if (a[i] == v) {
                return i;
            }
        }
        return -1;
    }
    public boolean contains(Pn v) {
        return find(v) != -1;
    }
    // Copy from S(inclusive) thru E(exclusive)
    public PnLst copy(int S, int E) {
        PnLst l = new PnLst();
        for (int i=S; i<E; i++) {
            l.append(a[i]);
        }
        return l;
    }
    public PnLst clone() {
        return copy(0, N);
    }
    public PnLstIterator getIterator() {
        return new PnLstIterator(this);
    }
    
    public void printme() {
        for (int i=0; i<N; i++) {
            System.out.println(String.format("PnLst item%d.", i));
            a[i].printme();
        }
    }
}




