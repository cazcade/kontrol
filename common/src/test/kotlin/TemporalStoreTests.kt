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

import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class TemporalStoreTests {

    test fun testWindow() {
        val store = BoundedComparableTemporalCollection<Int>();

        (1..10).forEach { store.add(it);Thread.sleep(1000) }
        val avg1Sec = store.avgForWindow(1100)?.toInt()
        val avg2Sec = store.avgForWindow(2100)?.toInt()
        val avg60Sec = store.avgForWindow(60 * 1000)?.toInt()
        val median60Sec = store.medianForWindow(60 * 1000)
        assertEquals(0.0, store.percentageInWindow(1, 100L..1100L))
        val piw1 = store.percentageInWindow(10, 0L..1100L)
        val piw2 = store.percentageInWindow(10, 0L..2100L)
        assertTrue(piw1 > 90)
        assertTrue(piw2 in 49.0..51.0)
        assertEquals(10, avg1Sec)
        assertEquals(9, avg2Sec)
        assertEquals(5, avg60Sec)
        assertEquals(6, median60Sec)
        println(avg1Sec)
        println(avg2Sec)
        println(avg60Sec)
        println(median60Sec)
        println(piw1)
        println(piw2)

    }

    after before fun clean() {
    }
}