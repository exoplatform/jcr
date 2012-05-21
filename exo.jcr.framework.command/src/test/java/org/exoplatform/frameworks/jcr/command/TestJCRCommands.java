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
package org.exoplatform.frameworks.jcr.command;

import junit.framework.TestCase;

import org.apache.commons.chain.Command;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.frameworks.jcr.command.core.AddNodeCommand;
import org.exoplatform.frameworks.jcr.command.core.SaveCommand;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

import java.util.Iterator;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SARL .
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: TestJCRCommands.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class TestJCRCommands extends TestCase
{

   private StandaloneContainer container;

   private CommandService cservice;

   private BasicAppContext ctx;

   public void setUp() throws Exception
   {

      String containerConf = getClass().getResource("/conf/standalone/test-configuration.xml").toString();
      String loginConf = Thread.currentThread().getContextClassLoader().getResource("login.conf").toString();

      if (System.getProperty("java.security.auth.login.config") == null)
         System.setProperty("java.security.auth.login.config", loginConf);

      StandaloneContainer.addConfigurationURL(containerConf);
      container = StandaloneContainer.getInstance();

      RepositoryService repService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      cservice = (CommandService)container.getComponentInstanceOfType(CommandService.class);

      // login via Authenticator
      Authenticator authr = (Authenticator)container.getComponentInstanceOfType(Authenticator.class);
      String validUser =
         authr.validateUser(new Credential[]{new UsernameCredential("root"), new PasswordCredential("exo")});
      Identity id = authr.createIdentity(validUser);
      ConversationState s = new ConversationState(id);
      ConversationState.setCurrent(s);

      ctx = new BasicAppContext(repService.getDefaultRepository());

      // System.out.println("CTX "+ctx);
   }

   public void testCatalogInit() throws Exception
   {
      Iterator cs = cservice.getCatalog().getNames();
      assertTrue(cs.hasNext());
   }

   public void testAddNode() throws Exception
   {

      AddNodeCommand addNode = (AddNodeCommand)cservice.getCatalog().getCommand("addNode");

      ctx.put("currentNode", "/");
      ctx.put(addNode.getPathKey(), "test");
      addNode.execute(ctx);

      SaveCommand save = (SaveCommand)cservice.getCatalog().getCommand("save");
      // ctx.remove(save.getPathKey());
      ctx.put(addNode.getPathKey(), "/");
      save.execute(ctx);
   }

   public void testSetProperty() throws Exception
   {

      Command c = cservice.getCatalog().getCommand("setProperty");
      ctx.put("currentNode", "/test");
      ctx.put("name", "testProperty");
      ctx.put("propertyType", PropertyType.TYPENAME_STRING);
      ctx.put("values", "testValue");
      ctx.put("multiValued", Boolean.FALSE);

      c.execute(ctx);

      Command save = cservice.getCatalog().getCommand("save");
      save.execute(ctx);

   }

   public void testGetNodes() throws Exception
   {

      Command c = cservice.getCatalog().getCommand("getNodes");
      ctx.put("currentNode", "/");
      c.execute(ctx);

      assertTrue(ctx.get("result") instanceof NodeIterator);
      NodeIterator nodes = (NodeIterator)ctx.get("result");

      // System.out.println("> getNodes >> "+nodes.getSize());

      assertTrue(nodes.getSize() > 0);
   }

   public void testAddResourceFile() throws Exception
   {

      Command c = cservice.getCatalog().getCommand("addResourceFile");
      ctx.put("currentNode", "/");
      ctx.put("path", "resource");
      ctx.put("data", "Node data");
      ctx.put("mimeType", "text/html");

      c.execute(ctx);

      Command save = cservice.getCatalog().getCommand("save");
      ctx.put("path", "/");
      save.execute(ctx);

   }

   public void testGetNodeChain() throws Exception
   {
      Command cmd = cservice.getCatalog().getCommand("retrieveNodeCommand");
      ctx.put("currentNode", "/");
      ctx.put("path", "test");
      cmd.execute(ctx);
   }
}
