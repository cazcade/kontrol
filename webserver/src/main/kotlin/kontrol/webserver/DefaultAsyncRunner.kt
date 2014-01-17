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

package kontrol.webserver


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
 * Default threading strategy for NanoHttpd.
 * <p/>
 * <p>By default, the server spawns a new Thread for every incoming request.  These are set
 * to <i>daemon</i> status, and named according to the request number.  The name is
 * useful when profiling the application.</p>
 */
public open class DefaultAsyncRunner() : AsyncRunner {
    private var requestCount: Long = 0
    public override fun exec(code: () -> Unit): Unit {
        ++requestCount
        val t = Thread(code)
        t.setDaemon(true)
        t.setName("NanoHttpd Request Processor (#" + requestCount + ")")
        t.start()
    }


}
