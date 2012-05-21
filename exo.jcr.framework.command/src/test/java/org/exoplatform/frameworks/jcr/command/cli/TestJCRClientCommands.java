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
package org.exoplatform.frameworks.jcr.command.cli;

import junit.framework.TestCase;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.frameworks.jcr.cli.AddNodeCommand;
import org.exoplatform.frameworks.jcr.cli.CliAppContext;
import org.exoplatform.frameworks.jcr.cli.CopyNodeCommand;
import org.exoplatform.frameworks.jcr.cli.GetContextInfoCommand;
import org.exoplatform.frameworks.jcr.cli.GetItemCommand;
import org.exoplatform.frameworks.jcr.cli.GetNodeCommand;
import org.exoplatform.frameworks.jcr.cli.GetNodesCommand;
import org.exoplatform.frameworks.jcr.cli.GetPropertiesCommand;
import org.exoplatform.frameworks.jcr.cli.GetPropertyCommand;
import org.exoplatform.frameworks.jcr.cli.HelpCommand;
import org.exoplatform.frameworks.jcr.cli.LoginCommand;
import org.exoplatform.frameworks.jcr.cli.MoveNodeCommand;
import org.exoplatform.frameworks.jcr.cli.RemoveItemCommand;
import org.exoplatform.frameworks.jcr.cli.SetPropertyCommand;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SARL .
 * 
 * @author <a href="mailto:vitaliy.obmanjuk@exoplatform.com.ua">Vitaliy Obmanjuk </a>
 * @version $Id: TestJCRClientCommands.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class TestJCRClientCommands extends TestCase
{

   private StandaloneContainer container;

   private CommandService cservice;

   private static CliAppContext ctx;

   private ArrayList<String> params = new ArrayList<String>();

   private final static String PARAMETERS_KEY = "parametersss";

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

      // we need to login (see BasicAppContext, 38) and set current item before ctx using
      if (ctx == null)
      {
         // login via Authenticator
         Authenticator authr = (Authenticator)container.getComponentInstanceOfType(Authenticator.class);
         String validUser =
            authr.validateUser(new Credential[]{new UsernameCredential("root"), new PasswordCredential("exo")});
         Identity id = authr.createIdentity(validUser);
         ConversationState s = new ConversationState(id);
         ConversationState.setCurrent(s);

         ctx = new CliAppContext(repService.getDefaultRepository(), PARAMETERS_KEY);
         Node root = ctx.getSession().getRootNode();
         ctx.setCurrentItem(root);
         if (root.hasNode("testJCRClientCommands") == false)
         {
            Node node = root.addNode("testJCRClientCommands").addNode("childOftestJCRClientCommands");
            node.setProperty("testProperty", "test");
            root.save();
         }
      }
      assertNotNull(ctx);
   }

   /*
    * /testJCRClientCommands /testJCRClientCommands/childOftestJCRClientCommands
    * /testJCRClientCommands/childOftestJCRClientCommands/childOftestJCRClientCommands2
    * /copyOftestJCRClientCommands /newCopyOftestJCRClientCommands
    */

   public void testCtxLogin() throws Exception
   {
      params.clear();
      LoginCommand loginCommand = (LoginCommand)cservice.getCatalog("CLI").getCommand("login");
      params.add("ws");
      ctx.put(PARAMETERS_KEY, params);
      loginCommand.execute(ctx);

      assertEquals(ctx.getCurrentItem(), ctx.getSession().getRootNode());
      assertEquals("ws", ctx.getSession().getWorkspace().getName());
   }

   public void testGetCtxItem() throws Exception
   {

      params.clear();
      GetItemCommand getItemCommand = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("/testJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommand.execute(ctx);

      assertEquals(ctx.getCurrentItem().getName(), "testJCRClientCommands");
   }

   public void testGetCtxNode() throws Exception
   {
      params.clear();
      GetNodeCommand getNodeCommand = (GetNodeCommand)cservice.getCatalog("CLI").getCommand("getnode");
      params.add("childOftestJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      getNodeCommand.execute(ctx);

      assertEquals(ctx.getCurrentItem().getName(), "childOftestJCRClientCommands");
   }

   public void testGetCtxProperty() throws Exception
   {
      params.clear();
      GetPropertyCommand getPropertyCommand = (GetPropertyCommand)cservice.getCatalog("CLI").getCommand("getproperty");
      params.add("testProperty");
      ctx.put(PARAMETERS_KEY, params);
      getPropertyCommand.execute(ctx);

      assertEquals(((Property)ctx.getCurrentItem()).getValue().getString(), "test");
   }

   public void testGetCtxNodes() throws Exception
   {
      params.clear();
      // current item is Property, need to go to Node
      GetItemCommand getItemCommand = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("/testJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommand.execute(ctx);
      // ok, now currentItem is Node "/testJCRClientCommands"
      GetNodesCommand getNodesCommand = (GetNodesCommand)cservice.getCatalog("CLI").getCommand("getnodes");
      getNodesCommand.execute(ctx);

      assertTrue(ctx.getOutput().contains("childOftestJCRClientCommands"));
   }

   public void testGetCtxProperties() throws Exception
   {
      GetPropertiesCommand getPropertiesCommand =
         (GetPropertiesCommand)cservice.getCatalog("CLI").getCommand("getproperties");
      getPropertiesCommand.execute(ctx);

      assertTrue(ctx.getOutput().contains("jcr:primaryType"));
   }

   public void testAddNode1() throws Exception
   {
      params.clear();
      AddNodeCommand addNodeCommand = (AddNodeCommand)cservice.getCatalog("CLI").getCommand("addnode");
      params.add("childOftestJCRClientCommands1");
      ctx.put(PARAMETERS_KEY, params);
      addNodeCommand.execute(ctx);

      assertEquals(ctx.getCurrentItem().getName(), "childOftestJCRClientCommands1");
   }

   public void testAddNode2() throws Exception
   {
      params.clear();
      AddNodeCommand addNodeCommand = (AddNodeCommand)cservice.getCatalog("CLI").getCommand("addnode");
      params.add("childOftestJCRClientCommands2");
      params.add("nt:unstructured");
      ctx.put(PARAMETERS_KEY, params);
      addNodeCommand.execute(ctx);

      assertEquals(((Node)ctx.getCurrentItem()).getPrimaryNodeType().getName(), "nt:unstructured");
   }

   public void testSetProperty1() throws Exception
   {
      params.clear();
      SetPropertyCommand setPropertyCommand = (SetPropertyCommand)cservice.getCatalog("CLI").getCommand("setproperty");
      params.add("propertyName1");
      params.add("propertyValue1");
      ctx.put(PARAMETERS_KEY, params);
      setPropertyCommand.execute(ctx);

      assertEquals(((Property)ctx.getCurrentItem()).getName(), "propertyName1");
      assertEquals(((Property)ctx.getCurrentItem()).getValue().getString(), "propertyValue1");
   }

   public void testSetProperty2() throws Exception
   {
      params.clear();
      //
      GetItemCommand getItemCommand = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("..");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommand.execute(ctx);
      //
      params.clear();
      SetPropertyCommand setPropertyCommand = (SetPropertyCommand)cservice.getCatalog("CLI").getCommand("setproperty");
      params.add("propertyName2");
      params.add("12345");
      params.add((new Integer(PropertyType.LONG)).toString());
      ctx.put(PARAMETERS_KEY, params);
      setPropertyCommand.execute(ctx);

      assertEquals(((Property)ctx.getCurrentItem()).getName(), "propertyName2");
      assertEquals(((Property)ctx.getCurrentItem()).getValue().getLong(), 12345);
   }

   public void testContextInfoCommand() throws Exception
   {
      GetContextInfoCommand getContextInfoCommand =
         (GetContextInfoCommand)cservice.getCatalog("CLI").getCommand("getcontextinfo");
      getContextInfoCommand.execute(ctx);

      assertTrue(ctx.getOutput().contains("root"));
      assertTrue(ctx.getOutput().contains("ws"));
   }

   public void testRemoveItemCommand() throws Exception
   {
      RemoveItemCommand removeItemCommand = (RemoveItemCommand)cservice.getCatalog("CLI").getCommand("remove");
      removeItemCommand.execute(ctx);

      assertEquals(((Node)ctx.getCurrentItem()).getName(), "childOftestJCRClientCommands2");
   }

   public void testCopyNodeCommand() throws Exception
   {
      params.clear();
      CopyNodeCommand copyNodeCommand = (CopyNodeCommand)cservice.getCatalog("CLI").getCommand("copynode");
      params.add("/testJCRClientCommands");
      params.add("/copyOftestJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      copyNodeCommand.execute(ctx);

      assertNotNull(ctx.getSession().getRootNode().getNode("copyOftestJCRClientCommands"));
   }

   public void testMoveNodeCommand() throws Exception
   {
      params.clear();
      MoveNodeCommand moveNodeCommand = (MoveNodeCommand)cservice.getCatalog("CLI").getCommand("movenode");
      params.add("/copyOftestJCRClientCommands");
      params.add("/newCopyOftestJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      moveNodeCommand.execute(ctx);

      assertNotNull(ctx.getSession().getRootNode().getNode("newCopyOftestJCRClientCommands"));
   }

   public void testHelpCommand() throws Exception
   {
      params.clear();
      HelpCommand helpCommand = (HelpCommand)cservice.getCatalog("CLI").getCommand("help");
      // params.add("addnode");
      ctx.put(PARAMETERS_KEY, params);
      helpCommand.execute(ctx);

      assertTrue(ctx.getOutput().contains("addnode"));
   }

   public void testCdCommand() throws Exception
   {
      // go to root
      params.clear();
      GetItemCommand getItemCommand1 = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("/");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommand1.execute(ctx);
      // test absPath
      params.clear();
      GetItemCommand getItemCommandAbsPath = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("/testJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommandAbsPath.execute(ctx);
      assertEquals(ctx.getCurrentItem().getName(), "testJCRClientCommands");
      // go to root again
      params.clear();
      GetItemCommand getItemCommand2 = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("/");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommand2.execute(ctx);
      // test relPath
      params.clear();
      GetItemCommand getItemCommandRelPath = (GetItemCommand)cservice.getCatalog("CLI").getCommand("getitem");
      params.add("testJCRClientCommands");
      ctx.put(PARAMETERS_KEY, params);
      getItemCommandRelPath.execute(ctx);
      assertEquals(ctx.getCurrentItem().getName(), "testJCRClientCommands");
   }

   public void testFinallyRemoveNodes() throws Exception
   {
      Node root = ctx.getSession().getRootNode();
      NodeIterator nodeIterator = root.getNodes();
      while (nodeIterator.hasNext())
      {
         Node node = nodeIterator.nextNode();
         if (!node.getPath().startsWith("/jcr:system"))
            node.remove();
      }
      root.save();
      assertTrue(root.getNodes().getSize() == 1 && root.getNode("jcr:system") != null);
   }
}
