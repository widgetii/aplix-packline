package ru.aplix.packline.hardware.scales;

public interface MeasurementListener {

    void onMeasure(Float value);

    void onWeightStabled(Float value);
}
