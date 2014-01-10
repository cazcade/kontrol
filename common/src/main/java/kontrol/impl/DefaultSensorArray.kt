package kontrol.impl

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

    override  fun avg(data: List<SensorValue<Any?>?>): Double? {
        val sum = sum(data)
        val size = data.filterNotNull().size
        return when {
            sum == null -> null
            size == 0 -> 0.0
            else -> sum / size
        };
    }


    override fun max(data: List<SensorValue<Any?>?>): Double? {
        return if (data.size() > 0) data.map { if (it?.value != null) it?.value.toString().toDouble() else null } .reduce { x, y ->
            when {
                x == null -> y
                y == null -> x
                x!! > y!! -> x
                else -> y
            }
        } else null
    }
    override fun min(data: List<SensorValue<Any?>?>): Double? {
        return if (data.size() > 0) data.map { if (it?.value != null) it?.value.toString().toDouble() else null } .reduce { x, y ->
            when {
                x == null -> y
                y == null -> x
                x!! < y!! -> x
                else -> y
            }
        } else null
    }
    override fun sum(data: List<SensorValue<Any?>?>): Double? {
        return if (data.size() > 0) data.map { if (it?.value != null) it?.value.toString().toDouble() else null } .reduce { x, y ->
            when {
                x == null -> y
                y == null -> x
                else -> x + y
            }
        } else null
    }


    override fun values(machine: Machine): Map<String, SensorValue<T>> {
        var result = HashMap<String, SensorValue<T>>();
        sensors.forEach { result.put(it.name(), it.value(machine)) };
        return result;
    }
}
