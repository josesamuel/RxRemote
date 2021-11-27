# RxRemote

**RxRemote** extends the power of Rx Observables across android process. 

**Example**


```java
@Remoter
public interface ISampleService {
     //Returns a RemoteObservable<String> 
     RemoteObservable<String> getRemoteObservable();
}

```
(Above example uses [Remoter](https://bit.ly/Remoter) interface instead of aidl, but it could be used with aidl too)

**At server side**

* Use Factory class RemoteObservables to create/notify

```java

@Override
public RemoteObservable<Integer> getDownloadObservable() {
	//Use the factory tied to "Download"	
	return RemoteObservables.<Integer>of("Download").newObservable();
}	
   ...
   	...
        	
//send the events
RemoteObservables.<Integer>of("Download").onNext(50);
RemoteObservables.<Integer>of("Download").onNext(100);

...
//complete
RemoteObservables.<Integer>of("Download").onCompleted();

```

OR

simply return a RemoteObservable wrapping your source Observable. Any events generated by the source Observable are delivered remotely

```java
 			
@Override
public RemoteObservable<String> getRemoteObservable() {
	//wrap your observable with a RemoteObservable to return across Binder
	return new RemoteObservable<>(Observable.from(new String[]{"Hello", "World"}););
}	

```


**At the client side**

* Get the RxJava Observable from RemoteObervable that you get from remote service.


```java
ISampleService sampleService = new ISampleService_Proxy( binder ); //See remoter
Observable<String> observable = sampleService.getRemoteObservable().getObservable();

//This is an Rx Java observable that can be subsribed to get data from your remote service

```

OR

directly access the data from RemoteObservable, and listen for updates by registering a listener

```java
ISampleService sampleService = new ISampleService_Proxy( binder ); //See remoter
RemoteObservable<String> remoteData = sampleService.getRemoteObservable();

//get last data
String data = remoteData.getData();

//listen for updates
remoteData.setDataListener(data -> {});


```



That's it! 



Getting RxRemote
--------

Gradle dependency

```groovy
dependencies {
    implementation 'com.josesamuel:rxremote:2.0.7'
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


