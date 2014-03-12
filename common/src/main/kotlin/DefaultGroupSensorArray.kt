/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kontrol.common


import java.util.HashMap
import kontrol.api.sensor.SensorValue
import kontrol.api.GroupSensor
import kontrol.api.sensors.GroupSensorArray
import kontrol.api.MachineGroup


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */

public class DefaultGroupSensorArray(val sensors: List<GroupSensor> = listOf()) : GroupSensorArray{
    val sensorMap: Map<String, GroupSensor>;

    {
        val sensorMap: MutableMap<String, GroupSensor> = hashMapOf();
        sensors.forEach { sensorMap[it.name()] = it }
        this.sensorMap = sensorMap;

    }


    //    override fun eachDouble(machines: List<Machine>, key: String, f: (List<Double?>) -> Double?): Double? {
    //        return f(machines.map {
    //            val sensorValue = values(it)[key];
    //            println("Sensor Value ${sensorValue}")
    //            sensorValue?.value?.toString()?.toDouble()
    //        });
    //    }


    override fun get(group: MachineGroup, sensor: String): SensorValue? {
        return sensorMap[sensor]?.value(group);
    }
}

