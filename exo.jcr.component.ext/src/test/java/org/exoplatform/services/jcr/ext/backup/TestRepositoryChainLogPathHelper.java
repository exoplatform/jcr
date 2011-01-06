/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.backup;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class TestRepositoryChainLogPathHelper
   extends TestCase
{

   public void testGetRelativePathOSLinux() throws Exception
   {
      RepositoryChainLogPathHelper helper = new RepositoryChainLogPathHelper();

      String path = "/path/to/repository-backup-dir/workspace-backup-dir/workspace-backup-log.xml";
      String backupDirCanonicalPath = "/path/to/repository-backup-dir";

      String relativePath = helper.getRelativePath(path, backupDirCanonicalPath);

      assertEquals("workspace-backup-dir/workspace-backup-log.xml", relativePath);
   }

   public void testGetRelativePathOSWindows() throws Exception
   {
      RepositoryChainLogPathHelper helper = new RepositoryChainLogPathHelper();

      String path = "c:\\\\path\\to\\repository-backup-dir\\workspace-backup-dir\\workspace-backup-log.xml";
      String backupDirCanonicalPath = "c:\\\\path\\to\\repository-backup-dir";

      String relativePath = helper.getRelativePath(path, backupDirCanonicalPath);

      assertEquals("workspace-backup-dir/workspace-backup-log.xml", relativePath);
   }

   public void testGetPathOSLinux() throws Exception
   {
      RepositoryChainLogPathHelper helper = new RepositoryChainLogPathHelper();

      String relativePath = "workspace-backup-dir/workspace-backup-log.xml";
      String backupDirCanonicalPath = "/path/to/repository-backup-dir";

      String path = helper.getPath(relativePath, backupDirCanonicalPath);

      assertEquals("/path/to/repository-backup-dir/workspace-backup-dir/workspace-backup-log.xml", path);
   }

   public void testGetPathOSWindiws() throws Exception
   {
      RepositoryChainLogPathHelper helper = new RepositoryChainLogPathHelper();

      String relativePath = "workspace-backup-dir/workspace-backup-log.xml";
      String backupDirCanonicalPath = "c:\\\\path\\to\\repository-backup-dir";

      String path = helper.getPath(relativePath, backupDirCanonicalPath);

      assertEquals("/c://path/to/repository-backup-dir/workspace-backup-dir/workspace-backup-log.xml", path);
   }

}
