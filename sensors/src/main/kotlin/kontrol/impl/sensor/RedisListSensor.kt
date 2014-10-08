package kontrol.sensor
/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

import kontrol.api.sensor.SensorValue
import redis.clients.jedis.Jedis
import kontrol.api.GroupSensor
import kontrol.api.MachineGroup

public class RedisListSensor(val list: String, val host: String = "localhost", val port: Int = 6379, val password: String? = null, val autoTrimAt: Long = -1) : GroupSensor{
    val jedis: Jedis;

    {
        jedis = Jedis(host, port);
        if (password != null) {
            jedis.auth(password)
        }
    }

    override fun name(): String {
        return "redis-" + list;
    }

    override fun value(machineGroup: MachineGroup): SensorValue {
        val l = jedis.llen(list)?:-1
        println("Redis list $list length is $l")
        if (autoTrimAt > 0 && l > autoTrimAt) {
            jedis.ltrim(list, 0, autoTrimAt)
            println("Redis list $list trimmed to  $autoTrimAt")
        }
        return SensorValue(name(), l.toString());
    }
}