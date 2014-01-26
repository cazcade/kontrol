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
package kontrol.postmortem

import kontrol.api.Postmortem
import kontrol.api.Machine
import kontrol.ext.string.ssh.onHost
import kontrol.api.PostmortemResult
import java.util.HashMap

public class JettyPostmortem(val jettyDir: String, val logMax: Int = 2000) : Postmortem{

    override fun perform(machine: Machine): PostmortemResult {
        println("Performing Jetty Postmortem for ${machine.name()}")

        return PostmortemResult("jetty", machine, arrayListOf(
                LogPart("jetty-log", "cat $jettyDir/logs/$(ls $jettyDir/logs/ -t | head -1) | tail -${logMax}" onHost machine.ip())
        ), HashMap(machine.latestDataValues()));
    }

}