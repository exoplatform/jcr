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

package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;

/**
 * Created by The eXo Platform SAS<br>
 *
 * The Items persistence listener interface, it'll be called when data is permanently saved.
 *
 * @author Gennady Azarenkov
 * @version $Id: ItemsPersistenceListener.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Unsupported
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
