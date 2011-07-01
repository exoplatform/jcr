/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.repository.creation.cluster;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestRepositoryCreationService.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestRepositoryCreationServiceInClusterNode2 extends AbstractBackupTestCase
{
   public void testCreateRepositorySingleDBWithSpecificCreationProps() throws Exception
   {
      log.info("Node2: Waits for the repository creation");
      Thread.sleep(60000);
      
      String tenantName = "tenant_2";

      // check
      ManageableRepository restoredRepository = repositoryService.getRepository(tenantName);
      assertNotNull(restoredRepository);

      SessionImpl session =
         (SessionImpl)restoredRepository.login(credentials, restoredRepository.getConfiguration()
            .getSystemWorkspaceName());
      session.getRootNode();

      log.info("Node2: test passed. I have root node");
      Thread.sleep(100000);
   }

   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      return getRDBMSBackupManager();
   }
}
