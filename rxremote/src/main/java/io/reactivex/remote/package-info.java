/**
 * <p>
 * <b>RxRemote</b> extends the power of Rx Observables across android process. 
 * </p>
 * <br/>
 * 
 * <br/>
 * Example (using <a href=\"https://bit.ly/Remoter\">Remoter</a> interface)
 * <br/>
 * <pre><code>
 * 
 * {@literal @}Remoter
 *  public interface ISampleService {
 *	    //Define a remote method that returns a RemoteObservable<String>
 *		RemoteObservable<String> getObservable();
 * }
 * </code></pre>
 *
 * <p>
 *     At the <b>service</b> side :
 *<pre><code>
 *          
 * //Controller to send events to cient
 * RemoteEventController<String> eventController = new RemoteEventController<>();
 * 
 *  public RemoteObservable<String> getObservable() {
 *		//wrap the controller and return
 *		return new RemoteObservable<>(eventController);
 *	}	
 *      	
 *      	...
 *      	...
 *      	
 *      	//send the events
 *      	eventController.sendEvent("Hello");
 *      	eventController.sendEvent("World");
 *      	...
 *      	//complete
 *      	eventController.sendCompleted();
 *</code></pre>
 * </p>
 * <br/>
 *
 * <p>
 *     At the <b>client</b> side :
 *     <br/>
 *     <br/>
 *     Get the RxJava Observable from RemoteObervable that you get from remote service.
 *<pre><code>
 * ISampleService sampleService = new ISampleService_Proxy( binder ); //See remoter
 * Observable<String> observable = sampleService.getObservable.getObservable();
 *</code></pre>
 * </p>
 * <br/>
 *
 * <p>
 * To add Remoter to your project add these to its gradle <b>dependencies</b>:
 * <br/>
 * <br/>
 *  <b>implementation 'com.josesamuel:rxremote:1.0.2’</b>
 * <br/><br/>
 * </p>
 * <br/>
 *
 */
package io.reactivex.remote;
