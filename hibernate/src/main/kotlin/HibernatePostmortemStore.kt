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

package kontrol.hibernate

import kontrol.api.PostmortemStore
import kontrol.api.PostmortemResult
import kontrol.postmortem.LogPart
import kontrol.postmortem.TextPart
import kontrol.api.PostmortemPart
import kontrol.api.sensor.SensorValue
import org.hibernate.criterion.Restrictions
import org.hibernate.FetchMode

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HibernatePostmortemStore(url: String = "jdbc:h2:file:store;DB_CLOSE_DELAY=-1") : PostmortemStore, HibernateStore<Int, PostmortemResult>(url, javaClass<PostmortemResult>(), listOf<Class<*>>(
        javaClass<LogPart>(),
        javaClass<TextPart>(),
        javaClass<PostmortemPart>(),
        javaClass<SensorValue>(),
        javaClass<PostmortemResult>()
)) {

    override fun addAll(list: List<PostmortemResult>) {
        try {
            return super<HibernateStore>.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun getWithParts(key: Int): PostmortemResult? {
        var session = sessionFactory.getCurrentSession();
        session?.beginTransaction();
        try {
            return session?.createCriteria(clazz)?.add(Restrictions.idEq(key))?.setFetchMode("parts", FetchMode.JOIN)?.uniqueResult() as PostmortemResult?
        } finally {
            session?.getTransaction()?.commit();
        }
    }
}
