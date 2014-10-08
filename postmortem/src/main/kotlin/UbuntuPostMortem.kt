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
import kontrol.api.PostmortemPart

public class UbuntuPostmortem(val logMax: Int = 1000) : Postmortem {

    override fun perform(machine: Machine): PostmortemResult {
        println("Performing Centos Postmortem for ${machine.name()}")
        return PostmortemResult("centos", machine, arrayListOf<PostmortemPart>(
                LogPart("syslog", "cat /var/log/messages | tail -${logMax}" onHost machine.ip()),
                TextPart("ps", "ps aux" onHost machine.ip()),
                TextPart("who", "who" onHost machine.ip()),
                TextPart("w", "w" onHost machine.ip()),
                TextPart("uptime", "uptime" onHost machine.ip()),
                TextPart("uname", "uname -a" onHost machine.ip()),
                TextPart("ifconfig", "ifconfig -a" onHost machine.ip()),
                TextPart("mem", "cat /proc/meminfo" onHost machine.ip()),
                TextPart("cpu", "cat /proc/cpuinfo" onHost machine.ip()),
                TextPart("ulimit", "ulimit -a" onHost machine.ip()),
                TextPart("du", "du -sh /*".onHost (machine.ip(), timeoutInSeconds = 120)),
                TextPart("df", "df -h" onHost machine.ip()),
                TextPart("services", "service --status-all" onHost machine.ip()),
                TextPart("netstat", "netstat -tulpn" onHost machine.ip())

        ), HashMap(machine.latestDataValues()));

    }


}