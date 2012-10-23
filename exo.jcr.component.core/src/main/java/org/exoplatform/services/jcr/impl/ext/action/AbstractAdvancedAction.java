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

package org.exoplatform.services.jcr.impl.ext.action;

import org.apache.commons.chain.Context;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:dvishinskiy@exoplatform.com">Dmitriy Vyshinskiy</a>
 * @version $Id: $
 */
public abstract class AbstractAdvancedAction implements AdvancedAction
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.AbstractAdvancedAction");

   /**
    * Simple implementation of onError() method. It reverts all pending changes of the current JCR session for any kind of event corresponding to a write operation.
    * Then in case the provided parentException is an instance of type AdvancedActionException, it will throw it otherwise it will log simply it.
    * An AdvancedActionException will be thrown in case the changes could not be reverted.
    * @see org.exoplatform.services.jcr.impl.ext.action.AdvancedAction#onError(java.lang.Exception, org.apache.commons.chain.Context)
    */
   public void onError(Exception parentException, Context context) throws AdvancedActionException
   {
      int eventId = (Integer)context.get(InvocationContext.EVENT);
      if (eventId != ExtendedEvent.READ)
      {
         Item item = (Item)context.get(InvocationContext.CURRENT_ITEM);
         try
         {
            item.getSession().refresh(false);
         }
         catch (RepositoryException e)
         {
            e.initCause(parentException);
            throw new AdvancedActionException(this.getClass().getName() + " changes rollback failed:", e);
         }
      }
      if (parentException instanceof AdvancedActionException)
      {
         throw (AdvancedActionException)parentException;
      }
      else
      {
         LOG.error(this.getClass().getName() + " throwed an exception:", parentException);
      }
   }
}
