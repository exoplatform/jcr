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

package org.exoplatform.services.jcr.usecases;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SimpleGetDataTest.java 11907 2008-03-13 15:36:21Z ksm $ JCR Use Case test sample
 */

public class SimpleGetDataTest extends BaseUsecasesTest
{

   /**
    * Sample test. An example how to make it
    * 
    * @throws Exception
    */
   public void testSimpleGetData() throws Exception
   {
      // Local small files creating
      Node testLocalSmallFiles = root.addNode("testLocalSmallFiles");
      Node localSmallFile = testLocalSmallFiles.addNode("smallFile", "nt:file");
      Node contentNode = localSmallFile.addNode("jcr:content", "nt:resource");
      // byte[] data = new byte[32];
      // Need to copy a file named test.txt to resources folder
      InputStream is = SimpleGetDataTest.class.getResourceAsStream("/test.txt");
      byte[] byteContent = new byte[is.available()];
      is.read(byteContent);

      contentNode.setProperty("jcr:data", new ByteArrayInputStream(byteContent));
      contentNode.setProperty("jcr:mimeType", "text/html");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      assertNotNull(session.getRootNode().getNode("testLocalSmallFiles").getNode("smallFile").getNode("jcr:content")
         .getProperty("jcr:data").getValue());
   }
}
