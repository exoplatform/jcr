/*
 * Copyright (C) 2011 eXo Platform SAS.
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
