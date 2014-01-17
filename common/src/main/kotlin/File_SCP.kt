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
import java.io.File

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

fun File.scp(host: String? = "localhost", user: String = "root", path: String = "~/", retry: Int = 3) {
    if (host == null) {
        return
    }
    val ssh: SSHClient = SSHClient();
    ssh.addHostKeyVerifier { a, b, c -> true };

    for ( i in 1..retry) {
        try {
            ssh.connect(host);
            ssh use {
                ssh.authPublickey(user);
                ssh.newSCPFileTransfer()?.upload(this.getAbsolutePath(), path)
            }
        } catch (e: Exception) {
            println("Retrying for the $i time to scp to $user@$host/$path - ${e.getMessage()}")
            Thread.sleep(100)
        }
        return
    }
}
