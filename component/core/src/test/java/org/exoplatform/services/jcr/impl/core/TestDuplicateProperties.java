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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="work.visor.ck@gmail.com">Dmytro Katayev</a>
 * 
 *         May 14, 2009
 */
public class TestDuplicateProperties extends JcrImplBaseTest
{

   // Reproduces the issue described in http://jira.exoplatform.org/browse/JCR-953
   public void testDuplicateProperties() throws Exception
   {

      String nodeName = "testDuplNod";
      String propName = "jcr:mimeType";
      String transientValue = "text/xml";

      root.addNode(nodeName);
      root.save();

      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Session session2 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));

      Node node1 = session1.getRootNode().getNode(nodeName);
      node1.setProperty(propName, transientValue);

      Node node2 = session2.getRootNode().getNode(nodeName);
      node2.setProperty(propName, "text/html");
      node2.save();

      int propCount = 0;
      Property neededProp = null;

      PropertyIterator iter = node1.getProperties();
      while (iter.hasNext())
      {
         Property prop = (Property)iter.next();
         if (prop.getName().equals(propName))
         {
            neededProp = prop;
            propCount++;
         }
      }

      assertEquals(1, propCount);
      assertEquals(transientValue, neededProp.getValue().getString());
   }

}
