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

package org.exoplatform.services.jcr.ext.backup;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.ContainerRequest;

import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS.
 * @author <a href="mailto:vitaly.parfonov@gmail.com">Vitaly Parfonov</a>
 * @version $Id: $
 */
public class ContainerRequestUserRole extends ContainerRequest
{
   public ContainerRequestUserRole(String method, URI requestUri, URI baseUri, InputStream entityStream,
      MultivaluedMap<String, String> httpHeaders)
   {
      super(method, requestUri, baseUri, entityStream, httpHeaders);
   }

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.ext.ContainerRequestUserRole");

   @Override
   public boolean isUserInRole(String role)
   {
      return true;
   }
}
