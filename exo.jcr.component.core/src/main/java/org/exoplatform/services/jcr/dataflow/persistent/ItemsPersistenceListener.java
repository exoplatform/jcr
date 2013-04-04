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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemsPersistenceListener.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Platform
 */
public interface ItemsPersistenceListener
{
   /**
    * Will be called when data is permanently saved.
    * 
    * @param itemStates ItemStateChangesLog
    */
   void onSaveItems(ItemStateChangesLog itemStates);

   /**
    * Return true if listener must be called in transaction, false if not.
    * @return boolean
    */
   boolean isTXAware();
}
