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

package kontrol.test.mock

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kontrol.api.PostmortemResult
import kontrol.postmortem.LogPart
import kontrol.api.PostmortemPart
import kontrol.hibernate.HibernatePostmortemStore

public class TestHibernate {
    test fun testStore(): Unit {
        val store = HibernatePostmortemStore()
        var list = arrayListOf<PostmortemPart>(LogPart("test-part", "hello world"))
        val pmr = PostmortemResult("test", null, list)
        store.add(pmr);

    }


}
