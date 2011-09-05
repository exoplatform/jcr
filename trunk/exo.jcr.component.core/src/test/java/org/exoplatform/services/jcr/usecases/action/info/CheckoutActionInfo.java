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
package org.exoplatform.services.jcr.usecases.action.info;

import org.apache.commons.chain.Context;
import org.exoplatform.services.jcr.observation.ExtendedEvent;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: CheckoutActionInfo.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class CheckoutActionInfo extends ActionInfo
{

   @Override
   public void execute(Context ctx) throws RepositoryException
   {
      Node node = (Node)ctx.get("node");
      if (node.canAddMixin("mix:versionable"))
         node.addMixin("mix:versionable");
      node.getSession().save();
      if (node.isCheckedOut())
         node.checkin();
      node.checkout();
   }

   @Override
   public int getEventType()
   {
      return ExtendedEvent.CHECKOUT;
   }

   @Override
   public void tearDown(Context ctx) throws RepositoryException
   {

      Node node = (Node)ctx.get("node");
      if (node.isCheckedOut())
      {
         node.checkin();
      }
      super.tearDown(ctx);
   }

}
