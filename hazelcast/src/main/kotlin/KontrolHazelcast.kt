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
package kontrol.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast

var hazelcastInstance: HazelcastInstance? = null;
var hazelcastConfig = Config();

fun initHazelcast(): HazelcastInstance {
    hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
    return hazelcastInstance!!
}

fun hazelcast(): HazelcastInstance {
    return synchronized(hazelcastConfig) {
        if (hazelcastInstance == null) {
            initHazelcast()
        } else {
            hazelcastInstance!!
        }
    }

}