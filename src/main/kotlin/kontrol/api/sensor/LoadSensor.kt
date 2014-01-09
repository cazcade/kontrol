package kontrol.api

import kontrol.api.sensor.SensorValue

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait LoadSensor : Sensor<Double?>{
    override fun value(machine: Machine): SensorValue<Double?>
}