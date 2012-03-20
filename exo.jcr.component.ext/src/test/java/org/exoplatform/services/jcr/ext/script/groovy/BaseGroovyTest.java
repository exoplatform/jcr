/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.script.groovy;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public abstract class BaseGroovyTest extends BaseStandaloneTest
{

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
   }

   protected String createScript(Node parent, String packageName, String name, String text) throws RepositoryException
   {
      return createScript(parent, packageName, name, new ByteArrayInputStream(text.getBytes()));
   }

   /**
    * @param parent parent node
    * @param packageName package name. Segment of package name must be separated
    *           by '.'. If required folders in hierarchy does not exists they
    *           will be created
    * @param name name of file with extension
    * @param text source code
    * @return path where script was created
    */
   protected String createScript(Node parent, String packageName, String name, InputStream text)
      throws RepositoryException
   {
      Node current = parent;
      if (packageName != null && packageName.length() > 0)
      {
         for (String s : packageName.split("\\."))
         {
            if (!current.hasNode(s))
               current = current.addNode(s, "nt:folder");
            else
               current = current.getNode(s);
         }
      }
      Node script = current.addNode(name, "nt:file");
      Node scriptContent = script.addNode("jcr:content", "nt:resource");
      scriptContent.setProperty("jcr:mimeType", "script/groovy");
      scriptContent.setProperty("jcr:lastModified", Calendar.getInstance());
      scriptContent.setProperty("jcr:data", text);
      session.save();
      return script.getPath();
   }
}
