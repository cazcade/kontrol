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
package kontrol.doclient;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * DigitalOcean API client Constants
 * 
 * @author Jeevanandam M. (jeeva@myjeeva.com)
 */
public interface Constants {


	// Gson Type Tokens
	Type TYPE_DROPLET_LIST = new TypeToken<List<Droplet>>() { }.getType();

	Type TYPE_IMAGE_LIST = new TypeToken<List<DropletImage>>() { }.getType();

	Type TYPE_REGION_LIST = new TypeToken<List<Region>>() { }.getType();

	Type TYPE_SIZE_LIST = new TypeToken<List<DropletSize>>() { }.getType();

	Type TYPE_DOMAIN_LIST = new TypeToken<List<Domain>>() { }.getType();

	Type TYPE_DOMAIN_RECORD_LIST = new TypeToken<List<DomainRecord>>() { }.getType();

	Type TYPE_SSH_KEY_LIST = new TypeToken<List<SshKey>>() { }.getType();
}
