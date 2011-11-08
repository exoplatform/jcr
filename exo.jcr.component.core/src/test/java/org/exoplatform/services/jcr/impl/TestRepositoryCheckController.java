/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.impl.RepositoryCheckController.DataStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.jcr.Node;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 10.10.2011 skarpenko $
 *
 */
public class TestRepositoryCheckController extends BaseStandaloneTest
{

   private RepositoryCheckController checkController;

   /**
    * @see org.exoplatform.services.jcr.BaseStandaloneTest#getRepositoryName()
    */
   @Override
   protected String getRepositoryName()
   {
      String repName = System.getProperty("test.repository");
      if (repName == null)
      {
         throw new RuntimeException(
            "Test repository is undefined. Set test.repository system property "
               + "(For maven: in project.properties: maven.junit.sysproperties=test.repository\ntest.repository=<rep-name>)");
      }
      return repName;

   }

   public void setUp() throws Exception
   {
      super.setUp();
      checkController = new RepositoryCheckController(repositoryService.getRepository("db1"));
   }

   public void tearDown() throws Exception
   {
      File f = checkController.getLastLogFile();
      if (f != null)
      {
         f.delete();
      }
      super.tearDown();
   }

   public void testDB()
   {
      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB});
      assertNotNull(result);
      assertTrue("Repository data is not consistent, result: " + result, result
         .startsWith("Repository data is consistent"));
   }

   public void testValueStorage() throws Exception
   {
      File f = this.createBLOBTempFile(20);
      InputStream is = new FileInputStream(f);
      try
      {
         Node n = root.addNode("node");
         n.setProperty("prop", is);

         root.save();

         String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.VALUE_STORAGE});
         assertNotNull(result);
         assertTrue("Repository data is not consistent, result: " + result, result
            .startsWith("Repository data is consistent"));
      }
      finally
      {
         is.close();
         f.delete();
      }
   }

   public void testSearchIndex()
   {
      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.LUCENE_INDEX});
      assertNotNull(result);
      assertTrue("Repository data is not consistent, result: " + result, result
         .startsWith("Repository data is consistent"));
   }

   public void testAll()
   {
      String result =
         checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB, DataStorage.VALUE_STORAGE,
            DataStorage.LUCENE_INDEX});
      assertNotNull(result);
      assertTrue("Repository data is not consistent, result: " + result, result
         .startsWith("Repository data is consistent"));
   }
}
