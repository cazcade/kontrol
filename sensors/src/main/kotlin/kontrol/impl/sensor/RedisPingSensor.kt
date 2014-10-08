package kontrol.sensor

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

import kontrol.api.sensor.SensorValue
import redis.clients.jedis.Jedis
import kontrol.api.Sensor
import kontrol.api.Machine

public class RedisPingSensor(val password: String? = null, val port: Int = 6379) : Sensor {

    override fun name(): String {
        return "redis-ping";
    }

    override fun value(machine: Machine): SensorValue {
        val jedis: Jedis;
        jedis = Jedis(machine.ip(), port);
        if (password != null) {
            jedis.auth(password)
        }
        return SensorValue(name(), if (jedis.ping() == "PONG") "OK" else "FAIL");
    }
}