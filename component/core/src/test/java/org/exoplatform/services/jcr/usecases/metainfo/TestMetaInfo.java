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
package org.exoplatform.services.jcr.usecases.metainfo;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;

public class TestMetaInfo extends BaseUsecasesTest
{

   public void testXLSFile() throws Exception
   {
      InputStream is = TestMetaInfo.class.getResourceAsStream("/index/test_index.xls");

      Node file = root.addNode("testXLSFile", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/excel");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();

   }
}
