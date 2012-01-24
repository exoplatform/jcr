/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.backup.usecase;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.impl.core.ItemImpl;

import javax.jcr.Node;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 20 січ. 2012 skarpenko $
 *
 */
public class SetPropertyAction implements Action
{

   private String addPropertyName;

   private String addPropertyValue;

   /**
    * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
    */
   @Override
   public boolean execute(Context context) throws Exception
   {
      ItemImpl currentItem = (ItemImpl)context.get("currentItem");
      Node myActionNode = ((Node)currentItem).addNode("myActionNode");
      myActionNode.setProperty(addPropertyName, addPropertyValue);

      return false;
   }
}
