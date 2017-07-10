package edu.stanford.cs.crypto.efficientct.linearalgebra;

import cyclops.collections.immutable.VectorX;
import cyclops.companion.Monoids;
import edu.stanford.cs.crypto.efficientct.ECConstants;
import edu.stanford.cs.crypto.efficientct.ProofUtils;

import java.math.BigInteger;
import java.util.Iterator;

/**
 * Created by buenz on 7/2/17.
 */
public class FieldVector implements Iterable<BigInteger> {

    private final VectorX<BigInteger> a;
    private final BigInteger q;

    public FieldVector(VectorX<BigInteger> a, BigInteger q) {
        this.a = a;
        this.q = q;
    }

    public static FieldVector from(VectorX<BigInteger> vectorX) {
        return new FieldVector(vectorX, ECConstants.P);
    }

    public static FieldVector from(Iterable<BigInteger> vectorX) {
        return new FieldVector(VectorX.fromIterable(vectorX), ECConstants.P);
    }

    public static FieldVector random(int n) {
        return from(VectorX.generate(n, ProofUtils::randomNumber));
    }


    public BigInteger innerPoduct(Iterable<BigInteger> b) {
        return a.zip(b, BigInteger::multiply).reduce(Monoids.bigIntSum).mod(q);
    }

    public FieldVector haddamard(Iterable<BigInteger> b) {

        return from(a.zip(b, BigInteger::multiply).map(bi -> bi.mod(q)));
    }

    public FieldVector haddamard(VectorX<FieldVector> b) {
        return b.zip(a, FieldVector::times).reduce(FieldVector::add).get();
    }

    public FieldVector times(BigInteger b) {

        return from(a.map(b::multiply).map(bi -> bi.mod(q)));
    }

    public FieldVector add(Iterable<BigInteger> b) {

        return from(a.zip(b, BigInteger::add).map(bi -> bi.mod(q)));
    }

    public FieldVector add(BigInteger constant) {

        return from(a.map(constant::add).map(bi -> bi.mod(q)));
    }

    public FieldVector subtract(Iterable<BigInteger> b) {
        return from(a.zip(b, BigInteger::subtract).map(bi -> bi.mod(q)));
    }

    public BigInteger sum() {
        return a.reduce(Monoids.bigIntSum);
    }

    public FieldVector invert() {
        return from(a.map(bi -> bi.modInverse(q)));
    }

    public BigInteger firstValue() {
        return a.firstValue();
    }

    public BigInteger get(int i) {
        return a.get(i);
    }

    public int size() {
        return a.size();
    }

    public FieldVector subVector(int start, int end) {
        return from(a.subList(start, end));
    }

    public VectorX<BigInteger> getVector() {
        return a;
    }

    @Override
    public Iterator<BigInteger> iterator() {
        return a.iterator();
    }

    @Override
    public String toString() {
        return a.toString();
    }
}
