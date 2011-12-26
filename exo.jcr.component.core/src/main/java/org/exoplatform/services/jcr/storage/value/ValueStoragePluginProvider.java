/*
 * Copyright (C) 2009 eXo Platform SAS.
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

import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS 04.09.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueStoragePluginProvider.java 12843 2007-02-16 09:11:18Z peterit $
 */
public interface ValueStoragePluginProvider
{

   /**
    * Return <code>ValueIOChannel</code> matched this <code>property</code> and
    * <code>valueOrderNumer</code>. Null will be returned if no channel matches.
    * 
    * @param property
    *          PropertyData will be stored
    * @return ValueIOChannel appropriate for this property (by path, id etc) or null if no such
    *         channel found
    * @throws IOException
    *           if error occurs
    */
   ValueIOChannel getApplicableChannel(PropertyData property, int valueOrderNumer) throws IOException;

   /**
    * Return <code>ValueIOChannel</code> associated with given <code>storageId</code>.
    * 
    * @param storageId
    *          String with storage Id (see configuration)
    * @return ValueIOChannela associated with this storageId
    * @throws IOException
    *           if error occurs
    * @throws ValueStorageNotFoundException
    *           if no such storage found for storageId
    */
   ValueIOChannel getChannel(String storageId) throws IOException, ValueStorageNotFoundException;

   /**
    * Run consistency check operation on each plugin registered.
    * 
    * @param dataConnection
    *          WorkspaceStorageConnection persistent connection
    */
   void checkConsistency(WorkspaceStorageConnection dataConnection);

}
