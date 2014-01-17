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

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

public enum class Status(public val requestStatus: Int, public val description: String) {
    OK : Status(200, "OK")
    CREATED : Status(201, "Created")
    ACCEPTED : Status(202, "Accepted")
    NO_CONTENT : Status(204, "No Content")
    PARTIAL_CONTENT : Status(206, "Partial Content")
    REDIRECT : Status(301, "Moved Permanently")
    NOT_MODIFIED : Status(304, "Not Modified")
    BAD_REQUEST : Status(400, "Bad Request")
    UNAUTHORIZED : Status(401, "Unauthorized")
    FORBIDDEN : Status(403, "Forbidden")
    NOT_FOUND : Status(404, "Not Found")
    METHOD_NOT_ALLOWED : Status(405, "kontrol.webserver.Method Not Allowed")
    RANGE_NOT_SATISFIABLE : Status(416, "Requested Range Not Satisfiable")
    INTERNAL_ERROR : Status(500, "Internal Server Error")


    public fun getFullDescription(): String {
        return "" + this.requestStatus + " " + description
    }

}