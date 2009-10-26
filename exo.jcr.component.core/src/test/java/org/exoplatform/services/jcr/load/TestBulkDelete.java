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
package org.exoplatform.services.jcr.load;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import java.util.Calendar;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 16.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestBulkDelete.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestBulkDelete extends JcrImplBaseTest
{

   protected final int NODES_COUNT = 1000;

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      addNtFiles();
   }

   /**
    * Add small nt:files with dc:elementSet.
    * 
    */
   protected void addNtFiles() throws Exception
   {

      long start = System.currentTimeMillis();

      Node testRoot = root.addNode("deleteLoadTest");
      root.save();

      for (int i = 0; i < NODES_COUNT; i++)
      {
         // nt:file
         Node file = testRoot.addNode("File #" + i, "nt:file");
         Node content = file.addNode("jcr:content", "nt:resource");
         content.setProperty("jcr:data", "small content " + i);
         content.setProperty("jcr:encoding", "UTF-8");
         content.setProperty("jcr:mimeType", "text/plain");
         content.setProperty("jcr:lastModified", Calendar.getInstance());

         // dc:elementSet
         file.addMixin("dc:elementSet");
         file.setProperty("dc:title", new String[]{"File #" + i});
         file.setProperty("dc:creator", new String[]{"exo"});
         file.setProperty("dc:description", new String[]{"load test on postgres"});

         testRoot.save();
      }

      System.out.println("add time " + (System.currentTimeMillis() - start) + "ms");
   }

   public void testDelete() throws Exception
   {
      long start = System.currentTimeMillis();
      root.getNode("deleteLoadTest").remove();

      root.save();
      System.out.println("delete time " + (System.currentTimeMillis() - start) + "ms");
   }

}
