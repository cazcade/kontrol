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

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kontrol.sensor.SSHLoadSensor
import kontrol.mock.MockMachine
import kontrol.common.DefaultSensorArray
import kontrol.mock.MockDoubleSensor

public class TestSensors {

    test fun testLoad(): Unit {
        val sensor = SSHLoadSensor()
        sensor.start();
        println(sensor.value(MockMachine("teamcity.cazcade.com")));
        sensor.stop();
    }

    test fun testArray(): Unit {
        val sensors = listOf(MockDoubleSensor(50..100))
        val array = DefaultSensorArray(sensors)
        val machines = listOf(MockMachine("1.2.3.4"), MockMachine("1.2.3.5"))
        //
        //        println(array.avg(machines, "mock"));
        //        println(array.sum(machines, "mock"));
        //        println(array.max(machines, "mock"));
        //        println(array.min(machines, "mock"));
        //
        //        assert(array.avg(machines, "mock")!! > 50);
        //        assert(array.sum(machines, "mock")!! > 100);
        //        assert(array.max(machines, "mock")!! > 50);
        //        assert(array.min(machines, "mock")!! > 50);
        //
        //        assert(array.avg(machines, "mock")!! < 100);
        //        assert(array.sum(machines, "mock")!! < 200);
        //        assert(array.max(machines, "mock")!! < 100);
        //        assert(array.min(machines, "mock")!! < 100);
    }


}
