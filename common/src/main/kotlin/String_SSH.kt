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
import org.apache.commons.io.IOUtils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

public fun String.onHost(host: String? = "localhost", user: String = "root", timeoutInSeconds: Int = 120, retry: Int = 3): String {
    val timeoutTimer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    //    println("ssh ${user}@${host}  '$this' ")
    if (host == null) {
        return ""
    }
    val ssh: SSHClient = SSHClient();
    timeoutTimer.schedule({
        ssh.disconnect();
        ssh.close();
        println("SESSION TIMEOUT on $host when trying to '$this'")
    }, timeoutInSeconds.toLong() + 1, TimeUnit.SECONDS)
    try {

        ssh.addHostKeyVerifier { a, b, c -> true };
        for ( i in 1..retry) {
            try {
                ssh.connect(host);
                return ssh use {
                    ssh.authPublickey(user);

                    ssh.startSession()?.use { session ->
                        val command = session.exec(this);
                        val result = IOUtils.toString(command?.getInputStream())
                        command?.join(timeoutInSeconds, TimeUnit.SECONDS)
                        //                        println("> $result")
                        result
                    }?:throw Exception("Could not start session on $host")
                }
            } catch (e: Exception) {
                if ( i < retry ) {
                    println("Retrying for the $i time to ssh $user@$host -c $this  (${e.getMessage()})")
                    Thread.sleep(100)
                } else {
                    System.err.println("Error executing '$this' on $host as $user after $retry attempts, message was ${e.getMessage()}")
                }
            }
        }

        throw Exception("Failed to execute '$this' on $host as $user after $retry attempts")

    } finally {

        timeoutTimer.shutdown();
    }
}