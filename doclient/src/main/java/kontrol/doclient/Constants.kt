package kontrol.doclient

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/* Original  MIT License from  Java version: https://github.com/jeevatkm/digitalocean-api-java
 *
 * Copyright (c) 2010-2013 Jeevanandam M. (myjeeva.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE. 
 * 
 */
/**
 * DigitalOcean API client Constants
 *
 * @author Jeevanandam M. (jeeva@myjeeva.com)
 */
public trait Constants {


    class object {
        // Gson Type Tokens
        val TYPE_DROPLET_LIST: Type = object : TypeToken<MutableList<Droplet>>() {


        }.getType()!!
        val TYPE_IMAGE_LIST: Type = object : TypeToken<MutableList<DropletImage>>() {


        }.getType()!!
        val TYPE_REGION_LIST: Type = object : TypeToken<MutableList<Region>>() {


        }.getType()!!
        val TYPE_SIZE_LIST: Type = object : TypeToken<MutableList<DropletSize>>() {


        }.getType()!!
        val TYPE_DOMAIN_LIST: Type = object : TypeToken<MutableList<Domain>>() {


        }.getType()!!
        val TYPE_DOMAIN_RECORD_LIST: Type = object : TypeToken<MutableList<DomainRecord>>() {


        }.getType()!!
        val TYPE_SSH_KEY_LIST: Type = object : TypeToken<MutableList<SshKey>>() {


        }.getType()!!
    }
}
