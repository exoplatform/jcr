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
package org.exoplatform.frameworks.jcr.command.core;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.exoplatform.frameworks.jcr.command.DefaultKeys;
import org.exoplatform.frameworks.jcr.command.JCRAppContext;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SetPropertyCommand.java 7137 2006-07-18 15:01:20Z vetal_ok $
 */

public class SetPropertyCommand implements Command
{

   private String nameKey = DefaultKeys.NAME;

   private String currentNodeKey = DefaultKeys.CURRENT_NODE;

   private String resultKey = DefaultKeys.RESULT;

   private String propertyTypeKey = DefaultKeys.PROPERTY_TYPE;

   private String valuesKey = DefaultKeys.VALUES;

   private String multiValuedKey = DefaultKeys.MULTI_VALUED;

   public boolean execute(Context context) throws Exception
   {

      Session session = ((JCRAppContext)context).getSession();

      Node parentNode = (Node)session.getItem((String)context.get(currentNodeKey));
      String name = (String)context.get(nameKey);

      int type = PropertyType.valueFromName((String)context.get(propertyTypeKey));
      boolean multi;// = ((Boolean)context.get(multiValuedKey)).booleanValue();
      if (context.get(multiValuedKey).equals("true"))
      {
         multi = true;
      }
      else
      {
         multi = false;
      }
      Object values = context.get(valuesKey);
      if (values instanceof String)
         context.put(resultKey, parentNode.setProperty(name, (String)values, type));
      else if (values instanceof String[])
         context.put(resultKey, parentNode.setProperty(name, (String[])values, type));
      else if (values instanceof InputStream)
         context.put(resultKey, parentNode.setProperty(name, (InputStream)values));
      else
         throw new Exception("Values other than String, String[], InputStream is not supported");

      return false;
   }

}
