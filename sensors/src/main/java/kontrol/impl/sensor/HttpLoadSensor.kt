package kontrol.impl.sensor

import kontrol.api.Machine
import kontrol.api.sensor.SensorValue
import java.net.URL
import kontrol.api.LoadSensor
import kontrol.HttpUtil

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HttpLoadSensor(val path: String) : LoadSensor {

    override fun name(): String {
        return "http-load"
    }

    override fun value(machine: Machine): SensorValue<Double?> {
        try {
            return SensorValue(when {
                machine.hostname().isNotEmpty() -> {

                    val URI = URL("http", machine.hostname(), 80, path).toURI()
                    val status = HttpUtil.getUrlAsString(URI, 60000)?.toDouble()
                    //                    println("$URI responded with $status")
                    status;

                }
                else -> {
                    null
                }
            });
        } catch (e: Exception) {
            println("HttpLoadSensor: ${e.javaClass} for ${machine.name()}")
            return SensorValue(null);
        }

    }
}