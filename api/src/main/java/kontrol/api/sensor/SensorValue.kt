package kontrol.api.sensor

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public data class SensorValue<T>(public val value: T?) {

    fun toString(): String? {
        return if (value == null) null else value.toString();
    }

    fun D(): Double? {
        return when (value) {
            null -> null
            is Double -> value as Double
            else -> value.toString().toDouble()
        }
    }

    fun I(): Int? {
        return when (value) {
            null -> null
            is Int -> value as Int
            else -> value.toString().toInt()
        }
    }
}