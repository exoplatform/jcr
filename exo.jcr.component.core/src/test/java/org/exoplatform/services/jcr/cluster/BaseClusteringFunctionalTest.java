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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.cluster;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public abstract class BaseClusteringFunctionalTest extends TestCase
{

   private String realm = "eXo REST services";

   private String workspacePath = "/rest/jcr/repository/production/";

   private JCRWebdavConnection[] connections;

   protected String nodeName;

   /**
    * {@inheritDoc}
    */
   protected void setUp() throws Exception
   {
      super.setUp();

      connections =
         new JCRWebdavConnection[]{new JCRWebdavConnection("localhost", 8080, "root", "exo", realm, workspacePath)
         /**,
         new JCRWebdavConnection("localhost", 8082, "root", "exo", realm, workspacePath),
         new JCRWebdavConnection("localhost", 8083, "root", "exo", realm, workspacePath),
         new JCRWebdavConnection("localhost", 8084, "root", "exo", realm, workspacePath)
         */
         };

      nodeName = generateUniqueName("removed_node_over_webdav");
   }

   public String generateUniqueName(String prefix)
   {
      return prefix + "-" + Math.random();
   }

   /**
    * {@inheritDoc}
    */
   protected void tearDown() throws Exception
   {
      super.tearDown();

      connections[0].removeNode(nodeName);
      connections[0].stop();
   }

   protected JCRWebdavConnection[] getConnections()
   {
      return connections;
   }

   protected JCRWebdavConnection getConnection()
   {
      return connections[(int)(Math.random() * 100) % connections.length];
   }

   /**
    * Create BLOB.
    * 
    * @param prefix
    * @param sizeInKb
    * @return
    * @throws IOException
    */
   protected File createBLOBTempFile(String prefix, int sizeInKb) throws IOException
   {
      // create test file
      byte[] data = new byte[1024]; // 1Kb

      File testFile = File.createTempFile(prefix, ".tmp");
      FileOutputStream tempOut = new FileOutputStream(testFile);
      Random random = new Random();

      for (int i = 0; i < sizeInKb; i++)
      {
         random.nextBytes(data);
         tempOut.write(data);
      }
      tempOut.close();
      testFile.deleteOnExit(); // delete on test exit
      return testFile;
   }

   protected String getPropertyValue(byte[] responseData, String propertyName) throws XMLStreamException,
      FactoryConfigurationError, IOException
   {
      InputStream input = new ByteArrayInputStream(responseData);

      XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);

      boolean valueIsFound = false;

      String propertyValue = null;

      try
      {
         while (reader.hasNext())
         {
            int eventCode = reader.next();

            switch (eventCode)
            {

               case StartElement.START_ELEMENT : {

                  if (propertyName.equals(reader.getName()))
                  {
                     valueIsFound = true;
                  }
                  break;
               }

               case StartElement.CHARACTERS : {
                  if (valueIsFound)
                  {
                     propertyValue = reader.getText();
                  }
                  break;
               }
            }
         }
      }
      finally
      {
         reader.close();
         input.close();
      }

      return propertyValue;
   }
}
