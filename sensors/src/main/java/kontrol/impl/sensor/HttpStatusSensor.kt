package kontrol.sensor

import kontrol.api.Sensor
import kontrol.api.Machine
import kontrol.api.sensor.SensorValue
import java.net.URL
import java.util.Locale
import kontrol.HttpUtil

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HttpStatusSensor(val path: String, val port:Int=80) : Sensor<Int> {

    override fun name(): String {
        return "http-status"
    }

    override fun value(machine: Machine): SensorValue<Int> {
        try {
            return SensorValue(when {
                machine.hostname().isNotEmpty() -> {

                    val URI = URL("http", machine.hostname(), port, path).toURI()
                    val status = HttpUtil.getStatus(URI, Locale.getDefault(), 5000)
                    //                    println("$URI responded with $status")
                    status;

                }
                else -> {
                    null
                }
            });
        } catch (e: Exception) {
            println("HttpStatusSensor: ${e.javaClass} for ${machine.name()}")
            return SensorValue(999);
        }

    }
}