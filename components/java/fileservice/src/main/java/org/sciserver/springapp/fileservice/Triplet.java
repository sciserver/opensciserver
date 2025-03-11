package org.sciserver.springapp.fileservice;

public class Triplet<X1, X2, X3> {

    public final X1 x1;
    public final X2 x2;
    public final X3 x3;

    public Triplet(X1 x1, X2 x2, X3 x3) {
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
	}
}