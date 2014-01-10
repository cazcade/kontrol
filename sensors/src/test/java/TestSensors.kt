/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kontrol.impl.sensor.SSHLoadSensor
import kontrol.impl.mock.MockMachine
import kontrol.impl.DefaultSensorArray
import kontrol.impl.mock.MockDoubleSensor

public class TestSensors {

    test fun testLoad(): Unit {
        val sensor = SSHLoadSensor()
        sensor.start();
        println(sensor.value(MockMachine("teamcity.cazcade.com")));
        sensor.stop();
    }

    test fun testArray(): Unit {
        val sensors = listOf(MockDoubleSensor(50..100))
        val array = DefaultSensorArray<Double>(sensors)
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
