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
import javax.persistence.Entity as entity
import javax.persistence.Id as id
import javax.persistence.GeneratedValue as generated

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

enum class Type {
    INTEGER
    FLOAT
    BOOLEAN
    STRING
    NULL
}

public entity data class SensorValue(public var k: String? = null, value: Any? = null, public id  var id: String? = null, public var t: Type? = when(value) {
    null -> Type.NULL
    is Float, is Double -> Type.FLOAT
    is Long, is Int -> Type.INTEGER
    is Boolean -> Type.BOOLEAN
    else -> Type.STRING
}) : Serializable, Comparable<SensorValue> {


    override fun compareTo(other: SensorValue): Int {
        if (t in listOf(Type.FLOAT, Type.INTEGER) && other.t in listOf(Type.FLOAT, Type.INTEGER)) {
            return this.D()!!.compareTo(other.D()!!)
        } else {
            return  (v?:"").compareTo(other.v?:"")
        }

    }

    public var v: String? = value.toString()


    override fun toString(): String? {
        return if (v == null) null else v.toString();
    }

    fun D(): Double? {
        val v = v;
        return when(t) {
            Type. NULL -> null
            Type.INTEGER -> v?.toDouble()
            else -> v!!.toDouble()
        }
    }


    fun I(): Int? {
        val v = v;
        return when(t) {
            Type. NULL -> null
            Type.INTEGER -> v?.toInt()
            else -> (v.toString().toDouble()).toInt()
        }
    }


    fun L(): Long? {
        val v = v;
        return when(t) {
            Type. NULL -> null
            Type.INTEGER -> v?.toLong()
            else -> (v.toString().toDouble()).toLong()
        }
    }


    fun I(multiplier: Double): Int? {
        val v = v;
        return when (v) {
            null -> null
            "null" -> null
            else -> (v.toString().toDouble() * multiplier).toInt()
        }
    }

    fun withNewId(newId: String): SensorValue {
        return SensorValue(k, v, newId, t)

    }
}