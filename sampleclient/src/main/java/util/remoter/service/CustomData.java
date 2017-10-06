package util.remoter.service;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * For testing @Parcel (parceler)
 */
@Parcel
public class CustomData {
    int data;

    @ParcelConstructor
    public CustomData(int data) {
        this.data = data;
    }

    public int getData() {
        return data;
    }
}
