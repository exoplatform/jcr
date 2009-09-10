/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.index.IndexReader;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestSystemSearchManager.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          There is test of system Search Manager
 */
public class TestSystemSearchManager extends JcrImplBaseTest
{

   public static final Log logger = ExoLogger.getLogger(TestSearchManagerIndexing.class);

   private SystemSearchManager manager;

   public void testRegisterNamespace() throws Exception
   {
      assertNotNull(manager);
      SearchIndex si = (SearchIndex)manager.getHandler();

      IndexReader ir = si.getIndex().getIndexReader();

      SearchManager sManager =
         (SearchManager)this.session.getContainer().getComponentInstanceOfType(SearchManager.class);
      SearchIndex ssi = (SearchIndex)sManager.getHandler();
      IndexReader sir = ssi.getIndex().getIndexReader();

      // remember document number, before any test
      int sysdocnum = ir.numDocs();
      int secdocnum = sir.numDocs();

      logger.info("  DOCNUM in system index[" + sysdocnum + "]");
      logger.info("  DOCNUM in second index[" + secdocnum + "]");

      workspace.getNamespaceRegistry().registerNamespace("test_my", "http://www.test_my.org/test_my");

      session.save();

      ir = si.getIndex().getIndexReader();
      sir = ssi.getIndex().getIndexReader();
      // assert that document num increased in system index
      assertEquals(sysdocnum + 1, ir.numDocs());
      // assert that document num havn't changed in other indexes
      assertEquals(secdocnum, sir.numDocs());

      ir.close();
      sir.close();
   }

   public void testCheckIn() throws Exception
   {
      assertNotNull(manager);

      final String nodename = "test_node";

      SearchIndex si = (SearchIndex)manager.getHandler();

      IndexReader ir = si.getIndex().getIndexReader();
      // remeber document number, before any test
      int docnum = ir.numDocs();
      logger.info("  DOCNUM [" + docnum + "]");

      Node node = root.addNode(nodename);
      node.addMixin("mix:versionable");
      root.save();
      node.checkin();
      root.save();

      ir = si.getIndex().getIndexReader();

      // jcr:VersionLabels doc
      // ??? doc
      // jcr:frozenNode doc
      // 1 doc
      // jcr:RoorVersion
      // 5 at all
      assertEquals(docnum + 5, ir.numDocs());
      ir.close();
   }

   public void testAddNodeType() throws Exception
   {
      SearchIndex si = (SearchIndex)manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();

      // remeber document number, before any test
      int docnum = ir.numDocs();
      logger.info("  DOCNUM [" + docnum + "]");

      ExtendedNodeTypeManager typeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();

      String cnd =
         "<nodeTypes><nodeType name='test_my:referenceable' isMixin='true' hasOrderableChildNodes='false' primaryItemName=''>"
            + "<supertypes>" + "     <supertype>mix:referenceable</supertype>" + "</supertypes>" + "</nodeType>"
            + "</nodeTypes>";

      typeManager.registerNodeTypes(new ByteArrayInputStream(cnd.getBytes()), ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
      session.save();

      ir = si.getIndex().getIndexReader();

      assertEquals(docnum + 1, ir.numDocs());
      ir.close();
   }

   public void setUp() throws Exception
   {
      super.setUp();
      manager = (SystemSearchManager)this.session.getContainer().getComponentInstanceOfType(SystemSearchManager.class);
   }

}
