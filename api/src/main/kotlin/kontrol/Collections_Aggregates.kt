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

package kontrol.ext.collections

fun List<Double?>.sum(): Double? = if (this.filterNotNull().size() > 0) this.filterNotNull().reduce { x, y -> x + y } else null
fun List<Number?>.sumAsDouble(): Double? = if (this.filterNotNull().size() > 0) this.filterNotNull().map { it.toDouble() }.reduce { x, y -> x + y } else null
fun List<Int?>.sum(): Int? = if (this.filterNotNull().size() > 0) this.filterNotNull().reduce { x, y -> x + y } else null
fun List<Long?>.sum(): Long? = if (this.filterNotNull().size() > 0) this.filterNotNull().reduce { x, y -> x + y } else null

fun List<Number?>.avgAsDouble(): Double? {
    val sum = this.sumAsDouble()
    val size = this.filterNotNull().size
    return when {
        sum == null -> null
        size == 0 -> 0.0
        else -> sum / size
    };
}


fun <T : Comparable<T>> List<T?>.median(): T? {
    val sorted = this.filterNotNull().sortBy { it }
    if (sorted.size() > 0) {
        return sorted.get(sorted.size() / 2)
    } else {
        return null;
    }
}

fun <T : Comparable<T>> List<T>.max(): T? = if (this.filterNotNull().size() > 0) this.filterNotNull().reduce { x, y -> when { x > y -> x else -> y } } else null
fun <T : Comparable<T>> List<T>.min(): T? = if (this.filterNotNull().size() > 0) this.filterNotNull().reduce { x, y -> when { x < y -> x else -> y } } else null
