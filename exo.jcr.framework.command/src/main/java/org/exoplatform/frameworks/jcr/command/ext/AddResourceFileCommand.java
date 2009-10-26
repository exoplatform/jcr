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
package org.exoplatform.frameworks.jcr.command.ext;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.exoplatform.frameworks.jcr.command.DefaultKeys;
import org.exoplatform.frameworks.jcr.command.JCRAppContext;
import org.exoplatform.frameworks.jcr.command.JCRCommandHelper;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: AddResourceFileCommand.java 5800 2006-05-28 18:03:31Z geaz $
 */

public class AddResourceFileCommand implements Command
{

   private String pathKey = DefaultKeys.PATH;

   private String currentNodeKey = DefaultKeys.CURRENT_NODE;

   private String resultKey = DefaultKeys.RESULT;

   private String dataKey = "data";

   private String mimeTypeKey = "mimeType";

   public boolean execute(Context context) throws Exception
   {

      Session session = ((JCRAppContext)context).getSession();

      Node parentNode = (Node)session.getItem((String)context.get(currentNodeKey));
      String relPath = (String)context.get(pathKey);
      Object data = context.get(dataKey);
      String mimeType = (String)context.get(mimeTypeKey);

      Node file = JCRCommandHelper.createResourceFile(parentNode, relPath, data, mimeType);

      // Node file = parentNode.addNode(relPath, "nt:file");
      // Node contentNode = file.addNode("jcr:content", "nt:resource");
      // if(data instanceof InputStream)
      // contentNode.setProperty("jcr:data", (InputStream)data);
      // else
      // contentNode.setProperty("jcr:data", (String)data);
      // contentNode.setProperty("jcr:mimeType",
      // (String)context.get(mimeTypeKey));
      // contentNode.setProperty("jcr:lastModified", session
      // .getValueFactory().createValue(Calendar.getInstance()));

      context.put(resultKey, file);
      return true;
   }

   public String getResultKey()
   {
      return resultKey;
   }

   public String getCurrentNodeKey()
   {
      return currentNodeKey;
   }

   public String getPathKey()
   {
      return pathKey;
   }
}
