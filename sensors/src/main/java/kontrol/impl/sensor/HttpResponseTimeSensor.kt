package kontrol.sensor

import kontrol.api.Machine
import kontrol.api.sensor.SensorValue
import java.net.URL
import kontrol.HttpUtil
import kontrol.api.Sensor
import java.util.Locale

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HttpResponseTimeSensor(val path: String) : Sensor<Long?> {

    override fun name(): String {
        return "http-response-time"
    }

    override fun value(machine: Machine): SensorValue<Long?> {
        try {
            return SensorValue(when {
                machine.hostname().isNotEmpty() -> {

                    val start = System.currentTimeMillis();
                    val URI = URL("http", machine.hostname(), 80, path).toURI()
                    HttpUtil.getStatus(URI,Locale.getDefault(), 60 * 60 * 1000)
                    System.currentTimeMillis() - start

                }
                else -> {
                    null
                }
            });
        } catch (e: Exception) {
            println("HttpResponseTimeSensor: ${e.javaClass} for ${machine.name()}")
            return SensorValue(null);
        }

    }
}