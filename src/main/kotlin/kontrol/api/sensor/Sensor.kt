package kontrol.api

import kontrol.api.sensor.SensorValue

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait Sensor<out T> {

    fun name(): String
    fun value(machine: Machine): SensorValue<T>
    fun start() {
    }
    fun stop() {
    }
}