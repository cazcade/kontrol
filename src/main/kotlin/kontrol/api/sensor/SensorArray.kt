package kontrol.api.sensors

import kontrol.api.Machine
import kontrol.api.sensor.SensorValue

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait SensorArray<T> {

    fun values(machine: Machine): Map<String, SensorValue<T>>;

    fun avg(data: List<SensorValue<Any?>?>): Double?;

    fun min(data: List<SensorValue<Any?>?>): Double? ;

    fun max(data: List<SensorValue<Any?>?>): Double? ;

    fun sum(data: List<SensorValue<Any?>?>): Double? ;

    //    fun eachDouble(machines: List<Machine>, key: String, f: (List<Double?>) -> Double?): Double? ;


}