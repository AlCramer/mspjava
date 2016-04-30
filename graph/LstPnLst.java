package msp.graph;
import java.util.*;

/** (Python-style) extensible list of (list of Pn) */
public class LstPnLst {
    public PnLst[] a;
    public int N = 0;
    public LstPnLst() {
        super();
        a = new PnLst[16];
    }
    public void append(PnLst v) {
        if ((N+1) > a.length) {
            a = Arrays.copyOf(a, 2*a.length);
        }
        a[N++] = v;
    }
    public void extend(LstPnLst delta) {
        if ((N + delta.N) > a.length) {
            a = Arrays.copyOf(a, N+delta.N);
        }
        System.arraycopy(delta.a, 0, a, N, delta.N);
        N += delta.N;
    }
    public LstPnLst clone() {
        LstPnLst cl = new LstPnLst();
        cl.a = Arrays.copyOf(a, N);
        cl.N = N;
        return cl;
    }
    public PnLst combine() {
        PnLst pnLst = new PnLst();
        for (int i=0; i<N; i++) {
            PnLst px = a[i];
            if (px == null) {
                pnLst.append(null);
            } else {
                pnLst.extend(px);
            }
        }
        return pnLst;
    }
    public void printme() {
        for (int i=0; i<N; i++) {
            System.out.println(String.format("PnLst%d.", i));
            if (a[i] == null) {
                System.out.println("null");
            }
            a[i].printme();
        }
    }
}





