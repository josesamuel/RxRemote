# RxRemote

**RxRemote** extends the power of Rx Observables across android process. 

**Example**
(using [Remoter](https://bit.ly/Remoter) interface)

```java
@Remoter
public interface ISampleService {
     //Define a remote method that returns a RemoteObservable<String> 
     RemoteObservable<String> getRemoteObservable();
}

```

**At server side**

* Return a RemoteObservable wrapping your RemoteEventController
* Use the above event controller to send data, or signal end of data stream

```java
//Controller to send events to cient
RemoteEventController<String> eventController = new RemoteEventController<>();
 			
@Override
public RemoteObservable<String> getRemoteObservable() {
	//wrap the controller and return  	
	return new RemoteObservable<>(eventController);
}	
   ...
   	...
        	
//send the events
eventController.sendEvent("Hello");
eventController.sendEvent("World");
...
//complete
eventController.sendCompleted();

```



**At the client side**

* Get the RxJava Observable from RemoteObervable that you get from remote service.


```java
ISampleService sampleService = new ISampleService_Proxy( binder ); //See remoter
Observable<String> observable = sampleService.getRemoteObservable().getObservable();

//This is an Rx Java observable that can be subsribed to get data from your remote service

```

That's it! 



Getting RxRemote
--------

Gradle dependency

```groovy
dependencies {
    implementation 'com.josesamuel:rxremote:1.0.1'
}
```


License
-------

    Copyright 2017 Joseph Samuel

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


