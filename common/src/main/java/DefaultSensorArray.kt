package kontrol.common


import kontrol.api.sensors.SensorArray
import kontrol.api.Machine
import kontrol.api.Sensor
import java.util.HashMap
import kontrol.api.sensor.SensorValue


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */

public class DefaultSensorArray<T>(val sensors: List<Sensor<T>>) : SensorArray<T> {

    //    override fun eachDouble(machines: List<Machine>, key: String, f: (List<Double?>) -> Double?): Double? {
    //        return f(machines.map {
    //            val sensorValue = values(it)[key];
    //            println("Sensor Value ${sensorValue}")
    //            sensorValue?.value?.toString()?.toDouble()
    //        });
    //    }



    override fun values(machine: Machine): Map<String, SensorValue<T>> {
        var result = HashMap<String, SensorValue<T>>();
        sensors.forEach { result.put(it.name(), it.value(machine)) };
        return result;
    }
}

