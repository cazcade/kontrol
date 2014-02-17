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

package kontrol.sensor

import kontrol.api.LoadSensor
import kontrol.api.Machine
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session
import java.util.concurrent.TimeUnit
import net.schmizz.sshj.connection.channel.direct.Session.Command
import kontrol.api.sensor.SensorValue
import kontrol.api.OS

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class SSHLoadSensor(val user: String = "root", val os: OS = OS.LINUX) : LoadSensor{
    override fun name(): String {
        return "load";
    }

    override fun start() {
    }

    override fun stop() {
    }


    override fun value(machine: Machine): SensorValue {
        try {
            val ssh: SSHClient = SSHClient();
            ssh.addHostKeyVerifier { a, b, c -> true };
            ssh.connect(machine.ip());
            try {
                ssh.authPublickey(user);
                val session: Session? = ssh.startSession();
                try {
                    val cmd: Command?
                    cmd = when (os) {
                        OS.LINUX -> {
                            session?.exec("cat /proc/loadavg | cut -d' ' -f1 | tr -d ' '")
                        }
                        OS.OSX -> {
                            session?.exec("uptime | cut -d, -f3 | cut -d: -f2 | cut -d' ' -f2 | tr -d ' '");
                        }
                        else -> throw IllegalArgumentException("Unsupported OS $os")
                    }

                    val load = IOUtils.readFully(cmd?.getInputStream()).toString().trim().toDouble()
                    cmd?.join(5, TimeUnit.SECONDS);
                    return SensorValue(name(), load);
                } finally {
                    session?.close();
                }
            } finally {
                ssh.disconnect();
            }
        } catch (e: Exception) {
            println("SSHLoadSensor: ${e.javaClass} for ${machine.name()}")
            return  SensorValue(name(), null);
        }
    }
}