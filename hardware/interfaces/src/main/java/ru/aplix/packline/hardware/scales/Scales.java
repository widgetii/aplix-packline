package ru.aplix.packline.hardware.scales;

public interface Scales<C extends ScalesConfiguration> {

    String getName();

    void setConfiguration(String config) throws IllegalArgumentException;

    void setConfiguration(C config) throws IllegalArgumentException;

    C getConfiguration();

    void connect();

    void disconnect();

    boolean isConnected();

    void setConnectOnDemand(boolean value);

    boolean getConnectOnDemand();

    void addMeasurementListener(MeasurementListener listener);

    void removeMeasurementListener(MeasurementListener listener);

    void addConnectionListener(ScalesConnectionListener connectionListener);

    void removeConnectionListener(ScalesConnectionListener connectionListener);
}
