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
package org.exoplatform.services.jcr.impl.storage;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.BaseStandaloneTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: CacheTest.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class CacheTest
   extends BaseStandaloneTest
{

   protected String getRepositoryName()
   {
      return "db1";
   }

   public void testIfCacheIsNotSharedBetweenWorkspaces() throws Exception
   {

      String[] wsNames = repository.getWorkspaceNames();
      if (wsNames.length < 2)
         fail("Too few number of ws for test should be > 1");

      Session s1 = repository.getSystemSession(wsNames[0]);
      Session s2 = repository.getSystemSession(wsNames[1]);

      s1.getRootNode().addNode("test1", "nt:unstructured");

      s1.save();

      try
      {
         s2.getRootNode().getNode("test1");
         fail("PathNotFoundException should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

   }

}
