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

package kontrol.server

import kontrol.common.DefaultController
import kontrol.api.Infrastructure
import kontrol.status.StatusServer
import kontrol.hibernate.HibernatePostmortemStore
import kontrol.api.Controller
import kontrol.api.Bus
import kontrol.api.PostmortemStore

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public class Server(var infraFactory: (Controller, Bus, PostmortemStore) -> Infrastructure) {

    var infra: Infrastructure
    var statusServer: StatusServer
    val controller: Controller
    val bus: Bus
    val postmortems: PostmortemStore
    {
        val dbUrl = "jdbc:mysql://localhost:3306/kontrol?user=root"
        val eventLog = HibernateEventLog(dbUrl)
        bus = NullBus()
        controller = DefaultController(bus, eventLog)
        postmortems = HibernatePostmortemStore(dbUrl)
        postmortems.last(1)
        infra = infraFactory(controller, bus, postmortems)
        statusServer = StatusServer(infra, bus, postmortems, eventLog)

    }


    fun start(gracePeriod: Int = 120) {
        controller.start(gracePeriod)
        infra.start()
        statusServer.start()
    }


    fun stop() {
        statusServer.stop()
        infra.stop()
        controller.stop()
    }

    fun reload() {
        infra.stop()
        infra = infraFactory(controller, bus, postmortems)
        infra.start()
    }
}


