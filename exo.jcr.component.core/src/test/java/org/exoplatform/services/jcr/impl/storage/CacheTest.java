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

package org.exoplatform.services.jcr.impl.storage;

import org.exoplatform.services.jcr.BaseStandaloneTest;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: CacheTest.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class CacheTest extends BaseStandaloneTest
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
