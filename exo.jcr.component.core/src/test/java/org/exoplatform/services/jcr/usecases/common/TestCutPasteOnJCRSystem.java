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

package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.version.OnParentVersionAction;

public class TestCutPasteOnJCRSystem extends BaseStandaloneTest
{

   @Override
   protected void tearDown() throws Exception
   {
      // super.tearDown();
      if (adminSession_ != null)
      {
         adminSession_.logout();
      }
      if (systemSession_ != null)
      {
         systemSession_.logout();
      }

   }

   private Session adminSession_;

   private Session systemSession_;

   private String workspaceName;

   public void testCutActionJcrSystem() throws Exception
   {
      createTaxonomyNodeType();
      initTaxonomyTree();
      /*
       * So, we have exo:taxonomies tree like that: jcr:system/exo:taxonomies(nt:untructured) /cms
       * /news /sport /calendar
       */

      Node adminTaxonomyHome = adminSession_.getRootNode().getNode("jcr:system/exo:taxonomies");
      Workspace workspace = adminSession_.getWorkspace();
      assertTrue("workspace is not null", workspace != null);

      Node newsTaxonomy = adminTaxonomyHome.getNode("cms/news");
      Node sportsTaxonomy = adminTaxonomyHome.getNode("cms/sports");
      Node calendarTaxonomy = adminTaxonomyHome.getNode("cms/calendar");
      assertNotNull(newsTaxonomy);
      assertNotNull(sportsTaxonomy);
      assertNotNull(calendarTaxonomy);
      // if we cut-paste a taxonomy Node at 4th level (if we suppose that exo:taxonomies is 1st level
      // It's OK
      try
      {
         calendarTaxonomy.getNode("calendarTest");
         fail("There is node with path /exo:taxonomies/cms/calendar/calendarTest");
      }
      catch (Exception e)
      {
         calendarTaxonomy.addNode("calendarTest", "exo:taxonomy");
         adminSession_.save();
      }
      Node calendarNode2cut = calendarTaxonomy.getNode("calendarTest");
      assertNotNull(calendarNode2cut);
      String cutPath = calendarNode2cut.getPath();
      String pastedPath = sportsTaxonomy.getPath() + cutPath.substring(cutPath.lastIndexOf("/"));
      workspace.move(cutPath, pastedPath);
      // use other admin session to check
      Session otherAdminSession =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), workspaceName);
      assertNotSame(adminSession_, otherAdminSession);
      try
      {
         otherAdminSession.getItem(cutPath);
         fail("=======>Node isn't cut yet");
      }
      catch (Exception e)
      {
      }
      try
      {
         otherAdminSession.getItem(pastedPath);
      }
      catch (Exception e)
      {
         fail("==========> Node isn't pasted");
      }
      // if we cut - paste a higher level Node(3 or 2). It's wrong
      // cut "cms/news" node and paste to "/cms/sports"
      String newsSrcPath = newsTaxonomy.getPath();
      String destPath2Paste = sportsTaxonomy.getPath() + newsSrcPath.substring(newsSrcPath.lastIndexOf("/"));
      // otherAdminSession.getItem(destPath2Paste) ;

      otherAdminSession.refresh(false);

      workspace.move(newsSrcPath, destPath2Paste);
      try
      {
         otherAdminSession.getItem(newsSrcPath);
         fail(newsSrcPath + " don't be cut");
      }
      catch (Exception e)
      {
      }

      otherAdminSession.getItem(destPath2Paste);
      // add "cms/testNode" to check FAILL
      Node testTaxonomy = null;
      try
      {
         adminTaxonomyHome.getNode("testNode");
      }
      catch (PathNotFoundException e)
      {
         testTaxonomy = adminTaxonomyHome.addNode("testNode", "exo:taxonomy");
         adminSession_.save();
      }

      String srcPath = testTaxonomy.getPath();
      String destPath = sportsTaxonomy.getPath() + srcPath.substring(srcPath.lastIndexOf("/"));

      workspace.move(srcPath, destPath);
      try
      {
         adminSession_.getItem(destPath);
      }
      catch (Exception e)
      {
         fail("Node wasn't moved(pasted) successfully:" + destPath);
      }

      adminTaxonomyHome.remove();
      adminSession_.save();
   }

   private void initTaxonomyTree() throws Exception
   {
      String systemWorkspaceName_ = repository.getSystemWorkspaceName();
      systemSession_ = repository.getSystemSession(systemWorkspaceName_);
      adminSession_ = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), systemWorkspaceName_);
      Node taxonomyHome = null;
      try
      {
         taxonomyHome = (Node)systemSession_.getItem("/jcr:system/exo:taxonomies");
         fail("There should not be /jcr:system/exo:taxonomies");
      }
      catch (PathNotFoundException e)
      {
      }
      taxonomyHome = systemSession_.getRootNode().addNode("jcr:system/exo:taxonomies", "nt:unstructured");
      systemSession_.save();

      try
      {
         taxonomyHome.getNode("cms");
         fail("There should not be /jcr:system/exo:taxonomies/cms");
      }
      catch (PathNotFoundException e)
      {
         taxonomyHome.addNode("cms", "exo:taxonomy");
         systemSession_.save();
      }

      try
      {
         taxonomyHome.getNode("cms/news");
         fail("There should not be /jcr:system/exo:taxonomies/cms/news");
      }
      catch (Exception e)
      {
         taxonomyHome.addNode("cms/news", "exo:taxonomy");
         systemSession_.save();
      }

      try
      {
         taxonomyHome.getNode("cms/sports");
         fail("There should not be /jcr:system/exo:taxonomies/cms/sports");
      }
      catch (Exception e)
      {
         taxonomyHome.addNode("cms/sports", "exo:taxonomy");
         systemSession_.save();
      }

      try
      {
         taxonomyHome.getNode("cms/calendar");
         fail("There should not be /jcr:system/exo:taxonomies/cms/calendar");
      }
      catch (Exception e)
      {
         taxonomyHome.addNode("cms/calendar", "exo:taxonomy");
         systemSession_.save();
      }

   }

   private void createTaxonomyNodeType() throws Exception
   {

      /*
       * this is NodeType config for exo:taxonomy <nodeType name="exo:taxonomy" isMixin="false"
       * hasOrderableChildNodes="false" primaryItemName=""> <supertypes>
       * <supertype>nt:base</supertype> </supertypes> <childNodeDefinitions> <childNodeDefinition
       * name="*" defaultPrimaryType="" autoCreated="false" mandatory="false"
       * onParentVersion="VERSION" protected="false" sameNameSiblings="false"> <requiredPrimaryTypes>
       * <requiredPrimaryType>nt:base</requiredPrimaryType> </requiredPrimaryTypes>
       * </childNodeDefinition> </childNodeDefinitions> </nodeType>
       */
      ExtendedNodeTypeManager manager = repository.getNodeTypeManager();
      NodeTypeValue exoTaxonomy = new NodeTypeValue();

      exoTaxonomy.setName("exo:taxonomy");
      exoTaxonomy.setMixin(false);
      exoTaxonomy.setOrderableChild(false);
      exoTaxonomy.setPrimaryItemName("");
      List<String> superTypeNames = new ArrayList<String>();
      superTypeNames.add("nt:base");
      exoTaxonomy.setDeclaredSupertypeNames(superTypeNames);

      NodeDefinitionValue childNodeDefinitionValue = new NodeDefinitionValue();
      childNodeDefinitionValue.setName("*");
      childNodeDefinitionValue.setDefaultNodeTypeName("");
      childNodeDefinitionValue.setMandatory(false);
      childNodeDefinitionValue.setAutoCreate(false);
      childNodeDefinitionValue.setRequiredNodeTypeNames(superTypeNames);
      childNodeDefinitionValue.setSameNameSiblings(false);
      List<NodeDefinitionValue> childNodeDefinitions = new ArrayList<NodeDefinitionValue>();
      childNodeDefinitions.add(childNodeDefinitionValue);
      childNodeDefinitionValue.setOnVersion(OnParentVersionAction.VERSION);
      exoTaxonomy.setDeclaredChildNodeDefinitionValues(childNodeDefinitions);

      manager.registerNodeType(exoTaxonomy, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
   }

   protected String getRepositoryName()
   {
      return null;
   }
}
