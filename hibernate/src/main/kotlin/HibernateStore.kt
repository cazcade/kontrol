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
package kontrol.hibernate

import java.io.Serializable
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import kontrol.api.TableStore
import org.hibernate.criterion.Order


public open class HibernateStore<K : Comparable<K>, V : Serializable>(val url: String, val clazz: java.lang.Class<V>, classes: List<java.lang.Class<*>>) : TableStore<K, V>{
    override fun addAll(list: List<V>) {
        list.forEach {
            var session = sessionFactory.getCurrentSession();
            session?.beginTransaction();
            try {
                session?.saveOrUpdate(it)
            } finally {
                session?.getTransaction()?.commit();
            }
        }
    }
    override fun last(n: Int): List<V> {
        var session = sessionFactory.getCurrentSession()!!;
        session.beginTransaction();
        try {
            return session.createCriteria(clazz)!!.addOrder(Order.desc("created"))!!.setMaxResults(n)!!.list() as MutableList<V>
        } finally {
            session.getTransaction()?.commit();
        }
    }


    var sessionFactory: SessionFactory;

    {
        var configuration = Configuration()
        configuration.addAnnotatedClass(clazz)
        classes.forEach { configuration = configuration.addAnnotatedClass(it)!! }

        sessionFactory = configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
        ?.setProperty("hibernate.connection.url", url)
        ?.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver")
        ?.setProperty("hibernate.connection.username", "root")
        ?.setProperty("hibernate.current_session_context_class", "thread")
        ?.setProperty("hibernate.show_sql", "true")
        ?.setProperty("hibernate.hbm2ddl.auto", "update")
        ?.buildSessionFactory()!!;
        sessionFactory.openSession()?.createCriteria(clazz)?.setMaxResults(1)?.list()
    }


    override fun get(key: K): V? {
        var session = sessionFactory.getCurrentSession();
        session?.beginTransaction();
        try {
            return session?.get(clazz, key as Serializable) as V?
        } finally {
            session?.getTransaction()?.commit();
        }
    }
    override fun remove(value: V): V? {
        var session = sessionFactory.getCurrentSession();
        session?.beginTransaction();
        try {
            session?.delete(value)
            return value;
        } finally {
            session?.getTransaction()?.commit();
        }
    }

    override  fun add(value: V) {
        var session = sessionFactory.getCurrentSession();
        session?.beginTransaction();
        try {
            session?.saveOrUpdate(value)
        } finally {
            session?.getTransaction()?.commit();
        }
    }
    override fun close() {
    }

    override fun contains(key: K): Boolean {
        var session = sessionFactory.getCurrentSession();
        session?.beginTransaction();
        try {
            return session?.get(clazz, key as Serializable) != null
        } finally {
            session?.getTransaction()?.commit();
        }
    }
}