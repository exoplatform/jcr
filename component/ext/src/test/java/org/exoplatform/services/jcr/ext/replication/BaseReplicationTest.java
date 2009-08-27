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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 20.02.2007 17:10:01
 * 
 * @version $Id: BaseReplicationTest.java 20.02.2007 17:10:01 rainfox
 */
public abstract class BaseReplicationTest extends BaseStandaloneTest
{

   protected RepositoryImpl repository2;

   protected SessionImpl session2;

   protected Workspace workspace2;

   protected Node root2;

   protected ValueFactory valueFactory2;

   public void setUp() throws Exception
   {

      String containerConf = "src/test/java/conf/standalone/test-configuration-replication.xml";
      String loginConf = Thread.currentThread().getContextClassLoader().getResource("login.conf").toString();

      if (!new File(containerConf).exists())
      {
         containerConf = "component/ext/" + containerConf;
      }

      StandaloneContainer.addConfigurationPath(containerConf);

      container = StandaloneContainer.getInstance();

      if (System.getProperty("java.security.auth.login.config") == null)
         System.setProperty("java.security.auth.login.config", loginConf);

      credentials = new CredentialsImpl("admin", "admin".toCharArray());

      repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      repository = (RepositoryImpl)repositoryService.getRepository("db1");
      repository2 = (RepositoryImpl)repositoryService.getRepository("db2");

      session = (SessionImpl)repository.login(credentials, "ws");
      session2 = (SessionImpl)repository2.login(credentials, "ws");

      workspace = session.getWorkspace();
      workspace2 = session2.getWorkspace();

      root = session.getRootNode();
      root2 = session2.getRootNode();

      valueFactory = session.getValueFactory();
      valueFactory2 = session2.getValueFactory();

      Thread.sleep(10000);
   }

   protected void tearDown() throws Exception
   {
   }

}
