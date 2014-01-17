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

import java.util.ArrayList
import java.util.HashMap

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
 * Provides rudimentary support for cookies.
 * Doesn't support 'path', 'secure' nor 'httpOnly'.
 * Feel free to improve it and/or add unsupported features.
 *
 * @author LordFokas
 */
public  class CookieHandler(httpHeaders: Map<String, String>?) : Iterable<String> {
    private var cookies: HashMap<String, String> = HashMap<String, String>()
    private var queue: ArrayList<Cookie> = ArrayList<Cookie>()
    public override fun iterator(): Iterator<String> {
        return cookies.keySet().iterator()
    }
    /**
     * Read a cookie from the HTTP Headers.
     *
     * @param name The cookie's name.
     * @return The cookie's value if it exists, null otherwise.
     */
    public  fun read(name: String): String? {
        return cookies.get(name)
    }
    /**
     * Sets a cookie.
     *
     * @param name    The cookie's name.
     * @param value   The cookie's value.
     * @param expires How many days until the cookie expires.
     */
    public  fun set(name: String, value: String, expires: Int): Boolean = queue.add(Cookie(name, value, getHTTPTime(expires)))

    public fun set(cookie: Cookie): Boolean = queue.add(cookie)
    /**
     * Set a cookie with an expiration date from a month ago, effectively deleting it on the client side.
     *
     * @param name The cookie name.
     */
    public  fun delete(name: String): Boolean = set(name, "-delete-", -30)

    /**
     * Internally used by the webserver to add all queued cookies into the Response's HTTP Headers.
     *
     * @param response The Response object to which headers the queued cookies will be added.
     */
    public  fun unloadQueue(response: Response): Unit {
        for (cookie in queue) {
            response.addHeader("Set-kontrol.webserver.Cookie", cookie.getHTTPHeader())
        }
    }


    {
        val raw = httpHeaders?.get("cookie")
        if (raw != null)
        {
            val tokens = raw.split(";")
            for (token in tokens)
            {
                val data = token.trim().split("=")
                if (data.size == 2)
                {
                    cookies.put(data[0], data[1])
                }

            }
        }

    }

}
