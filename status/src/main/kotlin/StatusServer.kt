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
import kontrol.common.*
import kontrol.api.Infrastructure
import kontrol.api.Bus
import kontrol.api.PostmortemStore
import kontrol.api.EventLog
import kontrol.api.Action

public class StatusServer(infra: Infrastructure, bus: Bus, postmortem: PostmortemStore, eventLog: EventLog, prefix: String = "/status-server", val refresh: Int = 60) {
    var infra: Infrastructure = infra

    val server = VelocityWebServer(prefix = prefix) { session, context ->
        context["topology"] = infra.topology()
        context["refresh"] = refresh
        if (session.uri.endsWith(".do")) {
            when(session.uri.substring(1, session.uri.length - 3)) {
                "upgrade" -> {
                    val machineGroup = infra.topology().members[session.parms["group"]]
                    machineGroup?.controller?.execute(machineGroup!!, { true }, Action.UPGRADE);
                    context["message"] = "Upgrade requested for ${machineGroup?.name()}"
                    "full-status.html.vm";
                }
                "log" -> {
                    context["events"] = eventLog.last(500)
                    "log.html.vm"
                }
                "costing" -> {
                    "costing.html.vm"
                }
                "postmortems" -> {
                    context["postmortems"] = postmortem.last(100)
                    "postmortems.html.vm"
                }
                "defcon" -> {
                    var defcon = 5;
                    if (infra.topology().members.values().any { it.machines().size() > 0 && it.overloadedMachines().size() > 0 }) {
                        defcon = 4;
                    }
                    if (infra.topology().members.values().any { it.machines().size() > 0 && it.overloadedMachines().size() == it.machines().size() }) {
                        defcon = 3;
                    }
                    if (infra.topology().members.values().any { it.machines().size() > 0 && it.workingSize() == 0 }) {
                        defcon = 2;
                    }
                    if (infra.topology().members.values().any { it.machines().size() > 0 && it.failedMachines().size() == it.machines().size() }) {
                        defcon = 1;
                    }
                    context["defcon"] = defcon;
                    "defcon.html.vm"
                }
                "postmortem_detail" -> {
                    context["postmortem"] = postmortem.getWithParts(session.parms["id"]?.toInt()?:-1)
                    "postmortem_detail.html.vm"
                }
                else -> "404.html"
            }
        } else {
            session.uri.substring(1) + ".vm"
        }
    }

    fun start() {
        server.startServer()
    }

    fun stop() {
        server.stopServer()
    }

    fun updateInfrastructure(infra: Infrastructure) {
        this.infra = infra
    }

}