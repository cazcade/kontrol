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

package kontrol.api

import java.io.Serializable
import java.io.Closeable

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait TableStore<K : Comparable<K>, V : Serializable> : Closeable {

    fun add(value: V)

    fun addAll(list: List<V>)

    fun get(key: K): V?

    fun remove(value: V): V?

    fun contains(key: K): Boolean

    fun minus(value: V) {
        remove(value)
    }
    fun plusEquals(value: V): TableStore<K, V> {
        add(value)
        return this
    }
    fun minusEquals(value: V): TableStore<K, V> {
        remove(value)
        return this
    }

    fun last(n: Int): List<V>

}