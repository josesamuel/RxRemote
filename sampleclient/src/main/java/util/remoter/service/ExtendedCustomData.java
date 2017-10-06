package util.remoter.service;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * For testing @Parcel (parceler) extension
 */
@Parcel
public class ExtendedCustomData extends CustomData {

    @ParcelConstructor
    public ExtendedCustomData(int data) {
        super(data);
    }

    public int getData() {
        return data;
    }
}
