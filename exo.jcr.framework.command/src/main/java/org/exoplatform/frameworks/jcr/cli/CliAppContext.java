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
package org.exoplatform.frameworks.jcr.cli;

import org.exoplatform.frameworks.jcr.command.BasicAppContext;
import org.exoplatform.services.jcr.core.ManageableRepository;

import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class CliAppContext extends BasicAppContext
{

   protected final String currentItemKey = "CURRENT_ITEM";

   protected final String parametersKey;

   protected final String outputKey = "OUTPUT";

   public CliAppContext(ManageableRepository rep, String parametersKey) throws NamingException
   {
      super(rep);
      this.parametersKey = parametersKey;
   }

   @Deprecated
   public CliAppContext(ManageableRepository rep, String parametersKey, Credentials cred) throws NamingException
   {
      super(rep);
      this.parametersKey = parametersKey;
   }

   public String getUserName()
   {
      try
      {
         return getSession().getUserID();
      }
      catch (Exception e)
      {
         log.error("GetUserName error: " + e);
         return "Undefined";
      }
   }

   public String getCurrentWorkspace()
   {
      return currentWorkspace;
   }

   public List<String> getParameters()
   {
      return (List<String>)get(parametersKey);
   }

   public String getParameter(int index) throws ParameterNotFoundException
   {
      List<String> params = getParameters();
      if (params.size() <= index)
         throw new ParameterNotFoundException("Not enough number of parameters expected at least: " + (index + 1)
            + " found: " + params.size());
      return params.get(index);
   }

   public void setCurrentItem(Item item)
   {
      put(currentItemKey, item);
   }

   public Item getCurrentItem()
   {
      return (Item)get(currentItemKey);
   }

   public String getOutput()
   {
      return (String)get(outputKey);
   }

   public void setOutput(String output)
   {
      put(outputKey, output);
   }

   public void clearOutput()
   {
      put(outputKey, "");
   }

}
