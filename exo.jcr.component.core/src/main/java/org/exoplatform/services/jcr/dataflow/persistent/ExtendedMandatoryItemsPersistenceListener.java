/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ExtendedMandatoryItemsPersistenceListener.java Aug 22, 2012 tolusha $
 */
public interface ExtendedMandatoryItemsPersistenceListener extends MandatoryItemsPersistenceListener
{
   /**
    * Will be called when rollback is invoked at persistence layer.
    */
   void onRollback();

   /**
    * Will be called when commit is invoked at persistence layer .
    */
   void onCommit();
}
