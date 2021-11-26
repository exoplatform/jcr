/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.dataflow.session;


/**
 * Allows to be execute actions when an event occurs at {@link TransactionableResourceManager}
 * level
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public interface TransactionableResourceManagerListener
{
   /**
    * This method is called within the commit method of the TransactionableResourceManager
    * @param onePhase indicates whether it is one phase commit or not
    * @throws Exception if an error occurs
    */
   void onCommit(boolean onePhase) throws Exception;
   
   /**
    * This method is called within the abort method of the TransactionableResourceManager
    * @throws Exception if an error occurs
    */
   void onAbort() throws Exception;
   
   /**
    * This method is called within the afterCompletion method of the TransactionableResourceManager
    * @param status the status of the tx
    * @throws Exception if an error occurs
    */
   void onAfterCompletion(int status) throws Exception;
}
