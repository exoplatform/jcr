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
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.impl.core.value.StringValue;

import java.util.Calendar;

import javax.jcr.Node;

public class LoadTest extends BaseUsecasesTest
{
   private int ntFolders = 1;

   private int ntSubFolders = 10;

   private int ntFiles = 1;

   public void testInitTree() throws Exception
   {
      Node root = session.getRootNode();
      Node fsn = root.addNode("FSN", "nt:folder");

      for (int l = 1; l <= ntFolders; l++)
      {
         Node folder = fsn.addNode("Folder" + l, "nt:folder");

         for (int i = 1; i <= ntSubFolders; i++)
         {
            Node subFolder = folder.addNode("SubFolder" + i, "nt:folder");

            for (int j = 1; j <= ntFiles; j++)
            {
               Node file = subFolder.addNode("File" + j, "nt:file");
               Node contentNode = file.addNode("jcr:content", "nt:resource");
               contentNode.setProperty("jcr:data", LoadTest.class.getResourceAsStream("/test_tiff_file.tiff"));
               contentNode.setProperty("jcr:mimeType", new StringValue("image/tiff"));
               contentNode.setProperty("jcr:lastModified", session.getValueFactory()
                  .createValue(Calendar.getInstance()));
               /*
                * contentNode.addMixin("dc:elementSet"); Node elementNode =
                * file.getNode("dc:elementSet"); elementNode.setProperty("dc:title", "Title"+j);
                * elementNode.setProperty("dc:description", "Description"+j);
                * elementNode.setProperty("dc:creator", "Creator"+j);
                * elementNode.setProperty("dc:subject", "Subject"+j);
                * elementNode.setProperty("dc:publisher", "Publisher"+j);
                * elementNode.setProperty("dc:contributor", "Contributor"+j);
                * elementNode.setProperty("dc:identifier", "Identifier"+j);
                * elementNode.setProperty("dc:language", "Language"+j);
                * elementNode.setProperty("dc:source", "Source"+j); elementNode.setProperty("dc:rights",
                * "Rights"+j);
                */
            }
            session.save();
            assertEquals("nt:file->", 1, subFolder.getNodes().getSize());
         }
         assertEquals("nt:subFolder->", 10, folder.getNodes().getSize());
      }
      assertEquals("nt:folder->", 1, fsn.getNodes().getSize());
   }

}
