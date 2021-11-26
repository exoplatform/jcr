/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.script.groovy;

import org.exoplatform.services.rest.ext.groovy.BaseResourceId;
import org.exoplatform.services.rest.ext.groovy.ResourceId;

import java.net.URL;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: URLScriptKey.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class URLScriptKey extends BaseResourceId implements ResourceId
{
   public URLScriptKey(URL url)
   {
      super(url.toString());
   }
}
