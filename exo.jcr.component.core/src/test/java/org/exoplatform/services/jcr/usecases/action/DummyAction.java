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
