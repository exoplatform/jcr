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
