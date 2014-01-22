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
package org.exoplatform.services.jcr.usecases.action;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;

import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: DummyAction.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class DummyAction implements Action
{
   private int actionExecuterCount = 0;

   private Context info;

   public boolean execute(Context ctx) throws Exception
   {
      actionExecuterCount++;
      info=ctx;
      return false;
   }

   public int getActionExecuterCount()
   {
      return actionExecuterCount;
   }

   public void setActionExecuterCount(int actionExecuterCount)
   {
      this.actionExecuterCount = actionExecuterCount;
   }

   public Map<String, Object> getInfo()
   {
      return info;
   }

}
