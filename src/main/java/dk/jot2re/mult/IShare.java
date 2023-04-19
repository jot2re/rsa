package dk.jot2re.mult;

import java.io.Serializable;

public interface IShare<T extends Serializable> extends Serializable {

    T getRawShare();
}
