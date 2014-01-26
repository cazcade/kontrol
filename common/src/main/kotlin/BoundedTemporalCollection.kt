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

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
package kontrol.common

import kontrol.api.TemporalCollection
import java.util.LinkedList
import kontrol.api.ComparableTemporalStore
import kontrol.ext.collections.avgAsDouble
import kontrol.ext.collections.median


public open class BoundedTemporalCollection<T>(var limit: Int = 1000) : TemporalCollection<T>, MutableCollection<T>  {

    override fun withinSecs(range: Range<Int>, k: String): List<T?> {
        return within(LongRange(range.start * 1000L, range.end * 1000L), k)
    }
    override fun withinMinutes(range: Range<Int>, k: String): List<T?> {
        return within(LongRange(range.start * 1000 * 60L, range.end * 1000 * 60L), k)
    }
    override fun withinHours(range: Range<Int>, k: String): List<T?> {
        return within(LongRange(range.start * 1000 * 3600L, range.end * 1000 * 3600L), k)
    }

    override fun within(range: Range<Long>, k: String): List<T?> {
        val now = System.currentTimeMillis()
        val upperBound = now - range.start
        val lowerBound = now - range.end
        return list.filter { (it.time > lowerBound && it.time < upperBound ) && it.k == k }.map { it.v }.filterNotNull()
    }

    override fun countInWindow(targetValue: T?, window: Long, key: String): Int = inWindow(key, window).filter { it == targetValue }.size()

    override public  fun addNullable(e: T?): Boolean {
        addWithKey(e)
        return true;
    }
    override public fun add(e: T): Boolean {
        addWithKey(e)
        return true;
    }
    override public  fun remove(o: Any?): Boolean {
        return list.removeAll(list.filter { it.v == o })
    }
    override public  fun addAll(c: Collection<T>): Boolean {
        for (i in c) {
            add(i)
        }
        return true;
    }
    override public  fun removeAll(c: Collection<Any?>): Boolean {
        return list.removeAll(list.filter { it.v in c })
    }
    override public  fun retainAll(c: Collection<Any?>): Boolean {
        return list.retainAll(list.filter { it.v in c })
    }
    override public  fun clear() {
        list.clear();
    }
    override public  fun hashCode(): Int {
        return list.hashCode();
    }
    override  public fun equals(other: Any?): Boolean {
        return if (other is BoundedTemporalCollection<*>) list.equals(other.list) else false
    }
    override public  fun size(): Int {
        return list.size()
    }

    override public  fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    override  public fun contains(o: Any?): Boolean {
        return list.any { it.v == o }
    }

    override  public fun iterator(): MutableIterator<T> {
        return asList().filterNotNull().iterator() as MutableListIterator;
    }

    override  public fun containsAll(c: Collection<Any?>): Boolean {
        return c.all { contains(it) }
    }


    data class TemporalValue<T>(public var k: String, public var v: T?, public var  time: Long = System.currentTimeMillis()){
    }

    val list: MutableList<TemporalValue<T>> = LinkedList<TemporalValue<T>>()

    override fun inWindow(key: String, window: Long): List<T?> {
        return list.filter { it.time > System.currentTimeMillis() - window && it.k == key }.map { it.v }.filterNotNull()
    }
    override fun addWithKey(v: T?, k: String) {
        list.add(TemporalValue(k, v))
        if (list.size() > limit) {
            list.remove(list.first)
        }
    }
    override fun percentageInWindow(targetValue: T?, window: Long, key: String): Double {
        val windowList = inWindow(key, window)
        return (windowList.filter { it == targetValue }.size() * 100.0) / windowList.size().toDouble()
    }

    override fun avgForWindow(window: Long, key: String): Double? {
        return inWindow(key, window).map { if (it is Number) it as Number else null }.avgAsDouble()
    }

    public fun toString(): String {
        return list.toString();
    }

    override fun asList(): List<T?> {
        return list.map { it.v }
    }
    override fun lastEntry(k: String): T? {
        return list.filter { it.k == k }.last().v
    }


    override fun asMap(): Map<String, T?> {
        var map = hashMapOf<String, T?>()
        list.forEach { map.put(it.k, lastEntry(it.k)) }
        return map;
    }


}



public open class BoundedComparableTemporalCollection<T : Comparable<T>>(limit: Int = 1000) : ComparableTemporalStore<T>, BoundedTemporalCollection<T>(limit) {
    override fun   percentageInRangeInWindow(range: Range<T>, window: Long, key: String): Double {
        val windowList = inWindow(key, window)
        return (windowList.map { if (it is Comparable<*> && it as T in range) it else null }.filterNotNull().size() * 100.0) / windowList.size().toDouble()
    }
    override fun  medianForWindow(window: Long, key: String): T? {
        return inWindow(key, window).map { if (it is Comparable<*>) it as T else null }.median()
    }

}