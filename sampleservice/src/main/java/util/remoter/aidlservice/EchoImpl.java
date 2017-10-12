package util.remoter.aidlservice;

import util.remoter.service.IEcho;

/**
 * To test @Remoter passing through RO
 */
public class EchoImpl implements IEcho {
    @Override
    public String echo(String string) {
        return string;
    }
}
