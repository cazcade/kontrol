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

import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import java.io.File
import kontrol.common.scp
import kontrol.common.on
import kotlin.test.assertEquals

public class ExtensionsTests {

    test fun testScp() {
        var file = File("/tmp/test.scp.extension.txt")
        val testValue = "Testing 1 2 3"
        file.writeText(testValue)
        file.scp(host = "teamcity.cazcade.com", path = "/tmp/test.scp.extension.txt.receipt", user = "root")
        assertEquals(testValue, "cat /tmp/test.scp.extension.txt.receipt".on("teamcity.cazcade.com"))

    }

    after before fun clean() {
        File("/tmp/test.scp.extension.txt.receipt").delete()
        "rm /tmp/test.scp.extension.txt.receipt".on("teamcity.cazcade.com")
    }
}