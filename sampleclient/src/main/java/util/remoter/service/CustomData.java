package util.remoter.service;

import android.os.Parcelable;

import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.List;

/**
 * For testing @Parcel (parceler)
 */
@Parcel
public class CustomData {
    int data;

    public int getData() {
        return data;
    }
}
