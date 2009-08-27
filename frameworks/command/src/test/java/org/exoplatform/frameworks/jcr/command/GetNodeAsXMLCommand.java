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
package org.exoplatform.frameworks.jcr.command;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: GetNodeAsXMLCommand.java 5800 2006-05-28 18:03:31Z geaz $
 */

public class GetNodeAsXMLCommand implements Command
{

   private String pathKey = DefaultKeys.PATH;

   private String incomingNodeKey = DefaultKeys.RESULT;

   private String resultKey = DefaultKeys.RESULT;

   public boolean execute(Context context) throws Exception
   {
      Object obj = context.get(incomingNodeKey);
      if (obj == null || !(obj instanceof Node))
         throw new Exception("Invalid incoming node " + obj);
      Node node = (Node)context.get(incomingNodeKey);
      String xml = "<node path='" + node.getPath() + "'/>";
      context.put(resultKey, xml);
      return false;
   }

   public String getIncomingNodeKey()
   {
      return incomingNodeKey;
   }

   public String getPathKey()
   {
      return pathKey;
   }

   public String getResultKey()
   {
      return resultKey;
   }

}
