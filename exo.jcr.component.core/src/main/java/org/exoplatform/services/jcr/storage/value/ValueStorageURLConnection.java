/*
 * Copyright (C) 2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.storage.value;

import java.net.URL;
import java.net.URLConnection;

/**
 * This is class is the root class of all the implementations of 
 * {@link URLConnection} that a value storage that supports {@link URL}
 * can provide.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public abstract class ValueStorageURLConnection extends URLConnection
{

   /**
    * The id of the resource that we want to access
    */
   protected String idResource;

   protected ValueStorageURLConnection(URL url)
   {
      super(url);
   }

   /**
    * @return the id of the resource to which we want to access
    */
   public String getIdResource()
   {
      return idResource;
   }

   /**
    * Sets the id of the resource
    */
   void setIdResource(String idResource)
   {
      this.idResource = idResource;
   }

}
