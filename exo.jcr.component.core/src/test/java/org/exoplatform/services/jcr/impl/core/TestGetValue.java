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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import java.io.ByteArrayInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2009
 *
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a> 
 * @version $Id: TestGetValue.java 1518 2010-01-20 23:33:30Z sergiykarpenko $
 */
public class TestGetValue extends JcrImplBaseTest
{

   // Reproduces the issue described in http://jira.exoplatform.org/browse/JCR-1094
   public void testGetValue() throws Exception
   {
      String data = "Test JCR";

      Node testRoot = root.addNode("NodeRepresentationTest", "nt:unstructured");

      Node file = testRoot.addNode("file", "nt:file");
      Node d = file.addNode("jcr:content", "nt:resource");
      d.setProperty("jcr:mimeType", "text/plain");
      d.setProperty("jcr:lastModified", Calendar.getInstance());
      d.setProperty("jcr:data", new ByteArrayInputStream(data.getBytes()));

      session.save();

      d.getProperties();

      compareStream(d.getProperty("jcr:data").getValue().getStream(), new ByteArrayInputStream(data.getBytes()));
   }
}
