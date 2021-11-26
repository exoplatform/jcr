/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.CNDStreamReader;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.CNDStreamWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Created by The eXo Platform SAS.<br>
 * Class that tests read-write-read cycle for compact node type definition tools
 * ({@link CNDStreamReader} and {@link CNDStreamWriter})
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class TestCNDSerialization extends JcrImplBaseTest
{

   private static final String TEST_FILE = "cnd-reader-test-input.cnd";

   public void testSerialization() throws Exception
   {
      /** input stream */
      InputStream is = getClass().getClassLoader().getResourceAsStream("" + TEST_FILE);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      NamespaceRegistryImpl nsm = new NamespaceRegistryImpl();
      /** reading and writing */
      List<NodeTypeData> ntdList1 = new CNDStreamReader(nsm).read(is);
      new CNDStreamWriter(nsm).write(ntdList1, baos);
      /** new reader to read previous output */
      List<NodeTypeData> ntdList2 = new CNDStreamReader(nsm).read(new ByteArrayInputStream(baos.toByteArray()));
      /** checking equality */
      if (ntdList1.size() == 0 || ntdList1.size() != ntdList2.size())
      {
         fail("Exported node type definition was not successfully read back in");
      }
      else
      {
         for (int k = 0; k < ntdList1.size(); k++)
         {
            NodeTypeData ntd1 = ntdList1.get(k);
            NodeTypeData ntd2 = ntdList2.get(k);
            if (!ntd1.equals(ntd2))
            {
               fail("Exported node type definition was not successfully read back in. \r\n" + ntd2.getName()
                  + "differs from original " + ntd1.getName() + "\r\n" + baos.toString());
            }
         }
      }
   }
}
