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

package kontrol.common


import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import java.util.concurrent.TimeUnit

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

fun String.onHost(host: String? = "localhost", user: String = "root", retry: Int = 3): String? {
    if (host == null) {
        return null
    }
    val ssh: SSHClient = SSHClient();
    ssh.addHostKeyVerifier { a, b, c -> true };
    for ( i in 1..retry) {
        try {
            ssh.connect(host);
            try {
                ssh.authPublickey(user);
                val session: Session = ssh.startSession()!!
                try {
                    val cmd = session.exec(this)!!;
                    val ins = cmd.getInputStream()
                    return if (ins != null) {
                        val result = String(ins.readBytes(2048))
                        cmd.join(5, TimeUnit.SECONDS);
                        result;
                    } else {
                        null
                    }
                } finally {
                    session.close();
                }
            } finally {
                ssh.disconnect();
            }
        } catch (e: Exception) {
            println("Retrying for the $i time to ssh $user@$host -c $this  (${e.getMessage()})")
            Thread.sleep(100)
        }
    }
    return null
}