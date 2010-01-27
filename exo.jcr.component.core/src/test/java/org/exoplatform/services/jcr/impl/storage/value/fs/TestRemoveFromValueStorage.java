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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS. <br/>
 * Test to reproduce bug when values from value storages are not removed in some
 * cases.
 * 
 * @author Nikolay Zamosenchuk
 * @version $Id$
 */
public class TestRemoveFromValueStorage extends BaseStandaloneTest
{

   private Node testRoot;

   private Property prop = null;

   private Value[] values;

   private int largeCount = 5;

   private int smallCount = 5;

   private int largeValueSize = 1024 * 1024 * 2;

   private int smallValueSize = 1000 * 1024;

   private SessionImpl my_session;

   private Node my_root;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      // This test uses special workspace ("ws3"), with complex value storage. So
      // we need to login into another workspace.
      my_session = (SessionImpl)repository.login(credentials, "ws3");
      my_root = my_session.getRootNode();

      // creating property with binary values.
      testRoot = my_root.addNode("TestRoot");

      values = new Value[largeCount + smallCount];

      // creating large one's
      for (int i = 0; i < largeCount; i++)
      {
         // 2M will be stored in first value storage
         byte[] largeValue = new byte[largeValueSize];
         Random generator = new Random();
         generator.nextBytes(largeValue);
         values[smallCount + i] =
            testRoot.getSession().getValueFactory().createValue(new ByteArrayInputStream(largeValue));
      }

      // creating small one's
      for (int i = 0; i < smallCount; i++)
      {
         // 1000K will be stored in second value storage
         byte[] smallValue = new byte[smallValueSize];
         Random generator = new Random();
         generator.nextBytes(smallValue);
         values[i] = testRoot.getSession().getValueFactory().createValue(new ByteArrayInputStream(smallValue));
      }
      if (values.length == 1)
      {
         prop = testRoot.setProperty("binaryProperty", values[0]);
      }
      else
      {
         prop = testRoot.setProperty("binaryProperty", values);
      }
      my_session.save();
   }

   public void testRemoveValue() throws Exception
   {
      // reading values directly from value storage
      PropertyImpl propertyImpl = (PropertyImpl)prop;
      ValueStoragePluginProvider storageProvider =
         (ValueStoragePluginProvider)my_session.getContainer().getComponentInstanceOfType(
            ValueStoragePluginProvider.class);

      String propertyId = propertyImpl.getInternalIdentifier();
      int count = prop.getValues().length;
      Map<Integer, FileIOChannel> channels = new HashMap<Integer, FileIOChannel>();

      for (int i = 0; i < count; i++)
      {
         ValueIOChannel channel = storageProvider.getApplicableChannel((PropertyData)propertyImpl.getData(), i);
         if (channel != null)
         {
            channels.put(i, (FileIOChannel)channel);
         }
      }

      for (int i = 0; i < count; i++)
      {
         try
         {
            channels.get(i).read(propertyId, i, 2100 * 1024);
         }
         catch (Exception e)
         {
            fail("Poperty value " + i + " can't be read!");
         }
      }

      prop.remove();
      my_session.save();

      // checking whether values are still in value storage.
      for (int i = 0; i < count; i++)
      {
         try
         {
            // TreeFileIOChannel always returns a File. But if this file doesn't
            // really exists is size is 0.
            File value = channels.get(i).getFile(propertyId, i);
            if (value.length() == 0)
            {
               throw new Exception("");
            }
            fail("Poperty value still can be found in value-storage but should have been already deleted!");
         }
         catch (Exception e)
         {
            // ok
         }
      }

   }

   @Override
   protected void tearDown() throws Exception
   {
      if (my_session != null)
      {
         testRoot.remove();
         my_session.logout();
      }
      super.tearDown();
   }

   @Override
   protected String getRepositoryName()
   {
      return repository.getName();
   }

}
