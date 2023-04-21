package dk.jot2re.compiler;

import dk.jot2re.IProtocol;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public interface ICompilableProtocol extends IProtocol {

    default BigInteger execute(BigInteger privateInput, BigInteger publicInput) {
        return executeList(Collections.singletonList(privateInput), Collections.singletonList(publicInput)).get(0);
    }
    default List<BigInteger> executeOutputList(BigInteger privateInput, BigInteger publicInput) {
        return executeList(Collections.singletonList(privateInput), Collections.singletonList(publicInput));
    }
    default BigInteger executeInputList(List<BigInteger> privateInput, BigInteger publicInput) {
        return executeList(privateInput, Collections.singletonList(publicInput)).get(0);
    }
    default BigInteger executeInputList(BigInteger privateInput, List<BigInteger> publicInput) {
        return executeList(Collections.singletonList(privateInput), publicInput).get(0);
    }

    public List<BigInteger> executeList(List<BigInteger> privateInput, List<BigInteger> publicInput);

}
