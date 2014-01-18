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
import kontrol.common.on
import kontrol.api.PostmortemResult
import java.util.HashMap

public class CentosPostmortem(val logMax: Int = 10000) : Postmortem{

    override fun perform(machine: Machine): PostmortemResult {
        return PostmortemResult("centos", machine, hashMapOf(
                "syslog" to LogPart("cat /var/log/messages | tail -${logMax}" on machine.ip()),
                "ps" to TextPart("ps aux" on machine.ip()),
                "who" to TextPart("who" on machine.ip()),
                "w" to TextPart("w" on machine.ip()),
                "uptime" to TextPart("uptime" on machine.ip()),
                "uname" to TextPart("uanme -a" on machine.ip()),
                "ifconfig" to TextPart("ifconfig -a" on machine.ip()),
                "mem" to TextPart("cat /proc/meminfo" on machine.ip()),
                "cpu" to TextPart("cat /proc/cpuinfo" on machine.ip()),
                "ulimit" to TextPart("ulimit -a" on machine.ip()),
                "du" to TextPart("du -h /*" on machine.ip()),
                "df" to TextPart("df -h" on machine.ip())
        ), HashMap(machine.data));

    }


}