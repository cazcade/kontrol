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

package kontrol.api.sensor

import java.io.Serializable

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public data class SensorValue<T>(public val value: T?) : Serializable {

    override fun toString(): String? {
        return if (value == null) null else value.toString();
    }

    fun D(): Double? {
        val v = value;
        return when (v) {
            null -> null
            is Int -> v.toDouble()
            is Double -> v as Double
            else -> v.toString().toDouble()
        }
    }


    fun I(): Int? {
        val v = value;
        return when (v) {
            null -> null
            is Int -> v
            is Double -> v.toInt()
            else -> (v.toString().toDouble()).toInt()
        }
    }

    fun I(multiplier: Double): Int? {
        val v = value;
        return when (v) {
            null -> null
            is Int -> v * multiplier.toInt()
            is Double -> (v * multiplier).toInt()
            else -> (v.toString().toDouble() * multiplier).toInt()
        }
    }
}