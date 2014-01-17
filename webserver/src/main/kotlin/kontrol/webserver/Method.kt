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


public enum class Method {
    GET
    PUT
    POST
    DELETE
    HEAD
    OPTIONS

}

public fun lookupMethod(method: String?): Method? {
    if (method == null) {
        return null;
    }
    for (m in Method.values()) {
        if (m.toString().equalsIgnoreCase(method)) {
            return m
        }
    }
    System.out.println("Looking up " + method + " failed")
    return null
}
