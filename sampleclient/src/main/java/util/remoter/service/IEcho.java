package util.remoter.service;

import remoter.annotations.Remoter;

/**
 * To test @Remoter can be passed
 */
@Remoter
public interface IEcho {
    String echo(String string);
}
