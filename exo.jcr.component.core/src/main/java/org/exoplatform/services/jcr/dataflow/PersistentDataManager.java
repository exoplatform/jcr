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
package org.exoplatform.services.jcr.dataflow;

import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListenerFilter;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 30.12.2008
 *
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a> 
 * @version $Id: PersistentDataManager.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public interface PersistentDataManager extends DataManager
{

   void addItemPersistenceListener(ItemsPersistenceListener listener);

   void removeItemPersistenceListener(ItemsPersistenceListener listener);

   void addItemPersistenceListenerFilter(ItemsPersistenceListenerFilter filter);

   void removeItemPersistenceListenerFilter(ItemsPersistenceListenerFilter filter);

}
