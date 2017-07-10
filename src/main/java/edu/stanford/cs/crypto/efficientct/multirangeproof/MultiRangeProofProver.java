package edu.stanford.cs.crypto.efficientct.multirangeproof;

import cyclops.collections.immutable.VectorX;
import edu.stanford.cs.crypto.efficientct.*;
import edu.stanford.cs.crypto.efficientct.commitments.PeddersenCommitment;
import edu.stanford.cs.crypto.efficientct.commitments.PolyCommittment;
import edu.stanford.cs.crypto.efficientct.innerproduct.InnerProductProof;
import edu.stanford.cs.crypto.efficientct.innerproduct.InnerProductProver;
import edu.stanford.cs.crypto.efficientct.innerproduct.InnerProductWitness;
import edu.stanford.cs.crypto.efficientct.linearalgebra.FieldVector;
import edu.stanford.cs.crypto.efficientct.linearalgebra.GeneratorVector;
import edu.stanford.cs.crypto.efficientct.linearalgebra.PeddersenBase;
import edu.stanford.cs.crypto.efficientct.linearalgebra.VectorBase;
import edu.stanford.cs.crypto.efficientct.rangeproof.RangeProof;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.field.Polynomial;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by buenz on 7/2/17.
 */
public class MultiRangeProofProver implements Prover<GeneratorParams, GeneratorVector, MultiRangeProofWitness, RangeProof> {


    @Override
    public RangeProof generateProof(GeneratorParams parameter, GeneratorVector commitments, MultiRangeProofWitness witness) {
        VectorX<BigInteger> numbers = witness.getNumber();
        int m = commitments.size();
        VectorBase vectorBase = parameter.getVectorBase();
        PeddersenBase base = parameter.getBase();
        int n = vectorBase.getGs().size();
        int bitsPerNumber = n / m;

        FieldVector aL = FieldVector.from(VectorX.range(0, n).map(i -> numbers.get(i / bitsPerNumber).testBit(i % bitsPerNumber) ? BigInteger.ONE : BigInteger.ZERO));
        FieldVector aR = aL.subtract(VectorX.fill(n, BigInteger.ONE));
        BigInteger alpha = ProofUtils.randomNumber();
        ECPoint a = vectorBase.commit(aL, aR, alpha);
        FieldVector sL = FieldVector.random(n);
        FieldVector sR = FieldVector.random(n);
        BigInteger rho = ProofUtils.randomNumber();
        ECPoint s = vectorBase.commit(sL, sR, rho);

        ECPoint[] challengeArr = Stream.concat(commitments.stream(), Stream.of(a, s)).toArray(ECPoint[]::new);
        BigInteger y = ProofUtils.computeChallenge(challengeArr);
        FieldVector ys = FieldVector.from(VectorX.iterate(n, BigInteger.ONE, y::multiply));

        BigInteger z = ProofUtils.challengeFromInts(y);

        BigInteger p = ECConstants.P;
        FieldVector zs = FieldVector.from(VectorX.iterate(m, z.pow(2), z::multiply).map(bi -> bi.mod(p)));

        VectorX<BigInteger> twoVector = VectorX.iterate(bitsPerNumber, BigInteger.ONE, bi -> bi.shiftLeft(1));
        FieldVector twos = FieldVector.from(twoVector);
        FieldVector twoTimesZSquared = FieldVector.from(zs.getVector().flatMap(twos::times));
        FieldVector l0 = aL.add(z.negate());

        FieldVector l1 = sL;
        FieldVectorPolynomial lPoly = new FieldVectorPolynomial(l0, l1);
        FieldVector r0 = ys.haddamard(aR.add(z)).add(twoTimesZSquared);
        FieldVector r1 = sR.haddamard(ys);
        FieldVectorPolynomial rPoly = new FieldVectorPolynomial(r0, r1);


        FieldPolynomial tPoly = lPoly.innerProduct(rPoly);

        PolyCommittment polyCommittment = PolyCommittment.from(base, VectorX.of(tPoly.getCoefficients()));
        BigInteger x = ProofUtils.computeChallenge(polyCommittment.getCommitments());
        PeddersenCommitment mainCommitment = polyCommittment.evaluate(x);

        BigInteger mu = alpha.add(rho.multiply(x)).mod(p);

        BigInteger t = mainCommitment.getX();
        BigInteger tauX = mainCommitment.getR().add(zs.innerPoduct(witness.getRandomness()));
        ECPoint u = ProofUtils.fromSeed(ProofUtils.challengeFromInts(tauX, mu, t).mod(p));
        GeneratorVector hs = vectorBase.getHs();
        GeneratorVector gs = vectorBase.getGs();
        GeneratorVector hPrimes = hs.haddamard(ys.invert());
        FieldVector l = lPoly.evaluate(x);
        FieldVector r = rPoly.evaluate(x);
        FieldVector hExp = ys.times(z).add(twoTimesZSquared);
        ECPoint P = a.add(s.multiply(x)).add(gs.sum().multiply(z.negate())).add(hPrimes.commit(hExp)).add(u.multiply(t)).subtract(base.h.multiply(mu));
        VectorBase primeBase = new VectorBase(gs, hPrimes, u);
        ECPoint PAlt = primeBase.commit(l, r, t);

        InnerProductProver prover = new InnerProductProver();
        InnerProductWitness innerProductWitness = new InnerProductWitness(l, r);
        InnerProductProof proof = prover.generateProof(primeBase, P, innerProductWitness);
        //System.out.println("PProofAlt " + PAlt.normalize());
//
        //System.out.println("PProof " + P.normalize());
        //System.out.println("XProof " + x);
        //System.out.println("YProof " + y);
        //System.out.println("ZProof " + z);
        //System.out.println("uProof " + u);
        return new RangeProof(a, s, GeneratorVector.from(polyCommittment.getCommitments()), tauX, mu, t, proof);
    }
}
