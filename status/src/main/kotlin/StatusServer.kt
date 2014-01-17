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

package kontrol.status

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import kontrol.webserver.VelocityWebServer
import kontrol.api.Topology
import kontrol.common.*

public class StatusServer(topology: Topology, prefix: String = "/status-server", refresh: Int = 60) {
    val server = VelocityWebServer(prefix = prefix) { session, context ->
        context["topology"] = topology
        context["refresh"] = refresh
    }

    fun start() {
        server.startServer()
    }

    fun stop() {
        server.stopServer()
    }

}