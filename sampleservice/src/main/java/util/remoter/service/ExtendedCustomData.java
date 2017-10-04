package util.remoter.service;

/**
 * For testing @Parcel (parceler) extension
 */
public class ExtendedCustomData extends CustomData {

    public ExtendedCustomData(int data) {
        super(data);
    }

    public int getData() {
        return data;
    }
}
