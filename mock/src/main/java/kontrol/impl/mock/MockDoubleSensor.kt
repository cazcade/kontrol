package kontrol.impl.mock

import kontrol.api.Sensor
import kontrol.api.Machine
import kontrol.api.sensor.SensorValue

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockDoubleSensor(val intRange: IntRange) : Sensor<Double>{
    override fun name(): String {
        return "mock"
    }
    override fun value(machine: Machine): SensorValue<Double> {
        return SensorValue(Math.random() * (intRange.end - intRange.start) + intRange.start);
    }
    override fun start() {

    }
    override fun stop() {

    }
}