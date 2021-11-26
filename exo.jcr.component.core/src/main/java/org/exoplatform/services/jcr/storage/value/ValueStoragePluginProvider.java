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

package org.exoplatform.services.jcr.storage.value;

import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.io.IOException;
import java.net.URL;

/**
 * Created by The eXo Platform SAS 04.09.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueStoragePluginProvider.java 12843 2007-02-16 09:11:18Z peterit $
 */
public interface ValueStoragePluginProvider
{

   /**
    * Returns the <code>ValueIOChannel</code> that matches with this <code>property</code> and
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
    * Returns the <code>ValueIOChannel</code> associated with given <code>storageId</code>.
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
    * Gives the {@link ValueStorageURLConnection} corresponding to the given <code>storageId</code>
    * and <code>idResource</code>.
    * @param storageId
    *          String with storage Id (see configuration)
    * @param url
    *          the {@link URL} of the resource to which we want to access
    * @return the {@link ValueStorageURLConnection} corresponding to this storageId
    * @throws ValueStorageNotFoundException
    *           if no such storage found for storageId
    * @throws IOException
    *           if an error occurs while creating the connection
    * @throws UnsupportedOperationException if {@link URL} are not supported by the corresponding value storage
    */
   ValueStorageURLConnection createURLConnection(String storageId, URL url) throws ValueStorageNotFoundException,
      IOException;

   /**
    * Runs the consistency check operation on each registered plug-in.
    * 
    * @param dataConnection
    *          WorkspaceStorageConnection persistent connection
    */
   void checkConsistency(WorkspaceStorageConnection dataConnection);

}
