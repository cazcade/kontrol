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

package kontrol.ext.string.ssh


import net.schmizz.sshj.SSHClient
import java.util.concurrent.TimeUnit

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

public fun String.onHost(host: String? = "localhost", user: String = "root", retry: Int = 3, timeoutInSeconds: Int = 30): String {
    println("ssh ${user}@${host}  '$this' ")
    if (host == null) {
        return ""
    }
    val ssh: SSHClient = SSHClient();
    ssh.addHostKeyVerifier { a, b, c -> true };
    for ( i in 1..retry) {
        try {
            ssh.connect(host);
            return ssh use {
                ssh.authPublickey(user);

                ssh.startSession()?.use { session ->
                    val result = session.exec(this)?.getInputStream()?.use { ins -> String(ins.readBytes(2048)) }
                    session.join(timeoutInSeconds, TimeUnit.SECONDS)
                    result
                }?:throw Exception("Could not start session on $host")
            }
        } catch (e: Exception) {
            if ( i < retry ) {
                println("Retrying for the $i time to ssh $user@$host -c $this  (${e.getMessage()})")
                Thread.sleep(100)
            } else {
                throw e
            }
        }
    }
    throw Exception("Failed to execute '$this' on $host as $user after $retry attempts")
}