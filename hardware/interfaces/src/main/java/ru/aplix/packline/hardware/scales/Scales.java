package ru.aplix.packline.hardware.scales;

import ru.aplix.packline.hardware.Connectable;

public interface Scales<C extends ScalesConfiguration> extends Connectable {

    String getName();

    void setConfiguration(String config) throws IllegalArgumentException;

    void setConfiguration(C config) throws IllegalArgumentException;

    C getConfiguration();

    void setConnectOnDemand(boolean value);

    boolean getConnectOnDemand();

    void addMeasurementListener(MeasurementListener listener);

    void removeMeasurementListener(MeasurementListener listener);

    void addConnectionListener(ScalesConnectionListener connectionListener);

    void removeConnectionListener(ScalesConnectionListener connectionListener);
}
