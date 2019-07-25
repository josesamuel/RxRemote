package sample.rxremote;

import io.reactivex.remote.RemoteObservable;


interface IEchoAidl {

    RemoteObservable getStringObservable();

    void echo(String string);
}