package util.remoter.aidlservice;

import util.remoter.service.IGen;

/**
 * To test @Remoter passing through RO
 */
public class GenImpl<T> implements IGen<T> {
    @Override
    public T echo(T data) {
        return data;
    }
}
