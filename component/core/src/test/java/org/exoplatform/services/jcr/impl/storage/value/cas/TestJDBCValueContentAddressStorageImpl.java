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
package org.exoplatform.services.jcr.impl.storage.value.cas;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 19.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestJDBCValueContentAddressStorageImpl.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestJDBCValueContentAddressStorageImpl extends JcrImplBaseTest
{

   private JDBCValueContentAddressStorageImpl vcas;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      Properties props = new Properties();

      // find jdbc-source-name
      String jdbcSourceName = null;
      String jdbcDialect = null;
      for (WorkspaceEntry wse : repository.getConfiguration().getWorkspaceEntries())
      {
         if (wse.getName().equals(session.getWorkspace().getName()))
         {
            jdbcSourceName = wse.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);
            jdbcDialect = wse.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT);
         }
      }

      if (jdbcSourceName == null)
         fail(JDBCWorkspaceDataContainer.SOURCE_NAME + " required in workspace container config");

      props.put(JDBCValueContentAddressStorageImpl.JDBC_SOURCE_NAME_PARAM, jdbcSourceName);
      props.put(JDBCValueContentAddressStorageImpl.JDBC_DIALECT_PARAM, jdbcDialect);
      props.put(JDBCValueContentAddressStorageImpl.TABLE_NAME_PARAM,
         JDBCValueContentAddressStorageImpl.DEFAULT_TABLE_NAME + "_TEST");

      vcas = new JDBCValueContentAddressStorageImpl();
      vcas.init(props);
   }

   public void testAddRecord() throws Exception
   {
      String propertyId, hashId;
      vcas.addValue(propertyId = IdGenerator.generate(), 0, hashId = IdGenerator.generate());

      assertEquals("id should be same but ", hashId, vcas.getIdentifier(propertyId, 0));
   }

   public void testAddRecords() throws Exception
   {
      List<String> testSet = new ArrayList<String>();
      String propertyId = IdGenerator.generate();

      for (int i = 0; i < 100; i++)
      {
         String hashId = IdGenerator.generate();
         vcas.addValue(propertyId, i, hashId);
         testSet.add(hashId);
      }

      List<String> ids = vcas.getIdentifiers(propertyId, true);
      for (int i = 0; i < testSet.size(); i++)
      {
         assertEquals("id should be same but ", testSet.get(i), ids.get(i));
      }
   }

   public void testSharedRecords() throws Exception
   {
      List<String> testSet = new ArrayList<String>();

      // add shared in multivalued property notation
      String property1Id = IdGenerator.generate();
      String sharedHashId = null;
      for (int i = 0; i < 10; i++)
      {
         String hashId = IdGenerator.generate();
         if (i == 5)
            sharedHashId = hashId;
         vcas.addValue(property1Id, i, hashId);
         testSet.add(hashId);
      }

      String property2Id = IdGenerator.generate();
      for (int i = 0; i < 10; i++)
      {
         String hashId;
         if (i == 2)
            hashId = sharedHashId;
         else
            hashId = IdGenerator.generate();
         vcas.addValue(property2Id, i, hashId);
      }

      // any stuf
      vcas.addValue(IdGenerator.generate(), 0, IdGenerator.generate());
      vcas.addValue(IdGenerator.generate(), 0, IdGenerator.generate());

      // shared in singlevalued property notation
      vcas.addValue(IdGenerator.generate(), 0, sharedHashId);

      // test if can get full values list of proeprty incl. shared
      List<String> ids = vcas.getIdentifiers(property1Id, false);
      for (int i = 0; i < testSet.size(); i++)
      {
         assertEquals("id should be same but ", testSet.get(i), ids.get(i));
      }

      // test if can get list of owned only values (DELETE usecase)
      testSet.remove(sharedHashId);
      ids = vcas.getIdentifiers(property1Id, true);
      for (int i = 0; i < testSet.size(); i++)
      {
         assertEquals("id should be same but ", testSet.get(i), ids.get(i));
      }
   }

   public void testDeleteRecord() throws Exception
   {
      String propertyId;
      vcas.addValue(propertyId = IdGenerator.generate(), 0, IdGenerator.generate());

      vcas.deleteProperty(propertyId);

      try
      {
         vcas.getIdentifier(propertyId, 0);
         fail("Record was deleted " + propertyId);
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }
   }

   public void testAddExisting() throws Exception
   {
      String propertyId, hashId;
      vcas.addValue(propertyId = IdGenerator.generate(), 0, hashId = IdGenerator.generate());

      try
      {
         vcas.addValue(propertyId, 0, hashId);
         fail("RecordAlreadyExistsException should be thrown, record exists");
      }
      catch (RecordAlreadyExistsException e)
      {
         // ok
      }
   }

   public void testReadNotExisting() throws Exception
   {
      try
      {
         vcas.getIdentifier(IdGenerator.generate(), 0);
         fail("RecordNotFoundException should be thrown, record not found");
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }
   }

   public void testReadNotExistingList() throws Exception
   {
      try
      {
         vcas.getIdentifiers(IdGenerator.generate(), false);
         fail("RecordNotFoundException should be thrown, record not found");
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }

      try
      {
         vcas.getIdentifiers(IdGenerator.generate(), true);
         fail("RecordNotFoundException should be thrown, record not found");
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }
   }

   public void testDeleteNotExisting() throws Exception
   {
      try
      {
         vcas.deleteProperty(IdGenerator.generate());
         fail("RecordNotFoundException should be thrown, record not found");
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }
   }

   public void testHasSharedContent() throws Exception
   {
      String propertyId, hashId;
      // multiplevalues property record
      vcas.addValue(propertyId = IdGenerator.generate(), 0, hashId = IdGenerator.generate());
      vcas.addValue(propertyId, 1, IdGenerator.generate());

      // singlevalued one
      vcas.addValue(IdGenerator.generate(), 0, hashId);

      assertTrue("Property has shared content but the answer - false", vcas.hasSharedContent(propertyId));
   }
}
