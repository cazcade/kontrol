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

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait TemporalCollection<E> {

    fun inWindow(key: String = "<default>", window: Long): List<E?>
    fun lastEntry(k: String = "<default>"): E?
    fun percentageInWindow(targetValue: E?, window: Long, key: String = "<default>"): Double
    fun countInWindow(targetValue: E?, window: Long, key: String = "<default>"): Int
    fun avgForWindow(window: Long, key: String = "<default>"): Double?
    fun asList(): List<E?>
    fun asMap(): Map<String, E?>
    fun addWithKey(v: E?, k: String = "<default>")
    fun addNullable(e: E?): Boolean

    fun within(range: Range<Long>, k: String = "<default>"): List<E?>
    fun withinSecs(range: Range<Int>, k: String = "<default>"): List<E?>
    fun withinMinutes(range: Range<Int>, k: String = "<default>"): List<E?>
    fun withinHours(range: Range<Int>, k: String = "<default>"): List<E?>

    fun iterator(): MutableIterator<E>

    // Modification Operations
    public fun add(e: E): Boolean
    public fun remove(o: Any?): Boolean

    // Bulk Modification Operations
    public fun addAll(c: Collection<E>): Boolean
    public fun removeAll(c: Collection<Any?>): Boolean
    public fun retainAll(c: Collection<Any?>): Boolean
    public fun clear()


}
