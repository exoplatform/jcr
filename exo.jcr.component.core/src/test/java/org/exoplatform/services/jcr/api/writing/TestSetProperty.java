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
package org.exoplatform.services.jcr.api.writing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.value.NameValue;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.GregorianCalendar;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestSetProperty.java 14508 2008-05-20 10:07:45Z ksm $
 */
public class TestSetProperty extends JcrAPIBaseTest implements ItemsPersistenceListener
{

   static protected String TEST_MULTIVALUED = "testMultivalued";

   protected Node testMultivalued = null;

   private TransactionChangesLog cLog;

   public void setUp() throws Exception
   {
      super.setUp();

      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer(session.getWorkspace().getName());
      CacheableWorkspaceDataManager dm =
         (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      dm.addItemPersistenceListener(this);
   }

   public void initRepository() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node propDef = root.addNode("propertyDefNode", "nt:propertyDefinition");
      propDef.setProperty("jcr:name", valueFactory.createValue("test", PropertyType.NAME));

      propDef.setProperty("jcr:autoCreated", false);
      propDef.setProperty("jcr:mandatory", false);
      propDef.setProperty("jcr:onParentVersion", OnParentVersionAction.ACTIONNAME_COPY);
      propDef.setProperty("jcr:protected", false);
      propDef.setProperty("jcr:requiredType", PropertyType.TYPENAME_STRING.toUpperCase());
      propDef.setProperty("jcr:multiple", false);
      // Unknown Property Type. Should set something!
      Value[] defVals = {session.getValueFactory().createValue("testString")};
      propDef.setProperty("jcr:defaultValues", defVals);

      Node childNodeDefNode = root.addNode("childNodeDefNode", "nt:childNodeDefinition");
      childNodeDefNode.setProperty("jcr:name", valueFactory.createValue("test"), PropertyType.NAME);
      childNodeDefNode.setProperty("jcr:autoCreated", false);
      childNodeDefNode.setProperty("jcr:mandatory", false);
      childNodeDefNode.setProperty("jcr:onParentVersion", OnParentVersionAction.ACTIONNAME_COPY);
      childNodeDefNode.setProperty("jcr:protected", false);
      childNodeDefNode.setProperty("jcr:requiredPrimaryTypes", new NameValue[]{(NameValue)valueFactory.createValue(
         "nt:base", PropertyType.NAME)});
      childNodeDefNode.setProperty("jcr:sameNameSiblings", false);

      root.addNode("unstructured", "nt:unstructured");

      testMultivalued = root.addNode(TEST_MULTIVALUED);

      session.save();
   }

   public void tearDown() throws Exception
   {

      try
      {
         // testMultivalued.getSession().refresh(false);
         testMultivalued.remove();
         testMultivalued.getSession().save();
      }
      catch (RepositoryException e)
      {
         log.error("Error delete '" + TEST_MULTIVALUED + "' node", e);
      }

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      root.getNode("unstructured").remove();

      // session.getItem("/propertyDefNode").remove();
      root.getNode("propertyDefNode").remove();
      root.getNode("childNodeDefNode").remove();
      session.save();
      
      // Unregister the listener in order to make it available to the GC
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer(session.getWorkspace().getName());
      CacheableWorkspaceDataManager dm =
         (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      dm.removeItemPersistenceListener(this);
      
      super.tearDown();
   }

   public void testSetPropertyNameValue() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("propertyDefNode");
      // Node node = (Node)session.getItem("/propertyDefNode");

      try
      {
         node.setProperty("jcr:multiple", valueFactory.createValue(20l));
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
      session.refresh(false);
   }

   public void testSetPropertyNameValueType() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("propertyDefNode");

      session.refresh(false);
      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue(10l)}); // ,
      // PropertyType
      // .LONG
      assertEquals(PropertyType.LONG, node.getProperty("jcr:defaultValues").getValues()[0].getType());
      assertEquals(10, node.getProperty("jcr:defaultValues").getValues()[0].getLong());
      node.save();
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      node = session.getRootNode().getNode("propertyDefNode");
      assertEquals(10, node.getProperty("jcr:defaultValues").getValues()[0].getLong());
   }

   public void testSetPropertyNameValuesType() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNodeDefNode");
      Value[] values = {session.getValueFactory().createValue("not"), session.getValueFactory().createValue("in")};

      // it converts to required !
      // node.setProperty("jcr:requiredPrimaryTypes", values, PropertyType.LONG);
      node.setProperty("jcr:requiredPrimaryTypes", values, PropertyType.NAME);

      try
      {
         Property prop = node.setProperty("jcr:onParentVersion", values, PropertyType.STRING);
         fail("exception should have been thrown " + prop.getString());
      }
      catch (ValueFormatException e)
      {
      }

      Value[] nameValues =
         {valueFactory.createValue("jcr:unstructured", PropertyType.NAME),
            valueFactory.createValue("jcr:base", PropertyType.NAME)};
      node.setProperty("jcr:requiredPrimaryTypes", nameValues, PropertyType.NAME);
      node.save();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      node = session.getRootNode().getNode("childNodeDefNode");
      assertEquals(2, node.getProperty("jcr:requiredPrimaryTypes").getValues().length);
   }

   public void testSetPropertyNameStringValueType() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("propertyDefNode");

      session.refresh(false);

      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue((long)10)});
      assertEquals(PropertyType.LONG, node.getProperty("jcr:defaultValues").getValues()[0].getType());
      assertEquals(10, node.getProperty("jcr:defaultValues").getValues()[0].getLong());
      node.save();
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      node = session.getRootNode().getNode("propertyDefNode");
      assertEquals(10, node.getProperty("jcr:defaultValues").getValues()[0].getLong());
   }

   public void testSetPropertyNameStringValuesType() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNodeDefNode");
      String[] values = {"not", "in"};
      try
      {
         // it converts to required !
         node.setProperty("jcr:requiredPrimaryTypes", values, PropertyType.LONG);
      }
      catch (ValueFormatException e)
      {
      }
      try
      {
         node.setProperty("jcr:onParentVersion", values, PropertyType.STRING);
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }

      Value[] nameValues =
         {valueFactory.createValue("jcr:unstructured", PropertyType.NAME),
            valueFactory.createValue("jcr:base", PropertyType.NAME)};
      node.setProperty("jcr:requiredPrimaryTypes", nameValues, PropertyType.NAME);
      node.save();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      node = session.getRootNode().getNode("childNodeDefNode");
      assertEquals(2, node.getProperty("jcr:requiredPrimaryTypes").getValues().length);
   }

   public void testSetPropertyMultivaluedString() throws RepositoryException
   {
      String[] values = {"binary string 1", "binary string 2"};
      Property mvp1 = null;
      try
      {
         mvp1 = testMultivalued.setProperty("Multivalued Property", values, PropertyType.BINARY);
         testMultivalued.save();
      }
      catch (ValueFormatException e)
      {
         fail("Can't add 'Multivalued Property'. Error: " + e.getMessage());
      }
      try
      {
         assertTrue("'Multivalued Property' must have size 2", mvp1.getLengths().length == 2);
      }
      catch (RepositoryException e)
      {
         fail("Error of 'Multivalued Property' length reading. Error: " + e.getMessage());
      }

      SessionImpl newSession = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node test = (Node)newSession.getItem(testMultivalued.getPath());
      assertEquals("Node '" + TEST_MULTIVALUED + "' must have values length 2", 2, test.getProperty(
         "Multivalued Property").getValues().length);
      test = newSession.getRootNode().getNode(TEST_MULTIVALUED);
      assertEquals("Node '" + TEST_MULTIVALUED + "' must have values length 2", 2, test.getProperty(
         "Multivalued Property").getValues().length);
   }

   public void testSetPropertyMultivaluedBinary() throws RepositoryException
   {
      Value[] values =
         {valueFactory.createValue(new ByteArrayInputStream("binary string 1".getBytes())),
            valueFactory.createValue(new ByteArrayInputStream("binary string 2".getBytes()))};
      Property mvp1 = null;
      try
      {
         mvp1 = testMultivalued.setProperty("Multivalued Property", values, PropertyType.BINARY);
         testMultivalued.save();
      }
      catch (ValueFormatException e)
      {
         fail("Can't add 'Multivalued Property'. Error: " + e.getMessage());
      }
      try
      {
         assertTrue("'Multivalued Property' must have size 2", mvp1.getValues().length == 2);
      }
      catch (RepositoryException e)
      {
         fail("Error of 'Multivalued Property' length reading. Error: " + e.getMessage());
      }

      SessionImpl newSession = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node test = (Node)newSession.getItem(testMultivalued.getPath());
      assertEquals("Node '" + TEST_MULTIVALUED + "' must have values length 2", 2, test.getProperty(
         "Multivalued Property").getValues().length);
      test = newSession.getRootNode().getNode(TEST_MULTIVALUED);
      assertEquals("Node '" + TEST_MULTIVALUED + "' must have values length 2", 2, test.getProperty(
         "Multivalued Property").getValues().length);
   }

   public void testSetPropertyNameTypedValue() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("propertyDefNode");

      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue("default")});
      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue(new ByteArrayInputStream(new String(
         "default").getBytes()))});
      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue(true)});
      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue(new GregorianCalendar())});
      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue(20D)});
      node.setProperty("jcr:defaultValues", new Value[]{valueFactory.createValue(20L)});

      try
      {
         node.setProperty("jcr:multiple", 20D);
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }

      try
      {
         node.setProperty("jcr:versionStorage", 20D);
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      try
      {
         node.setProperty("jcr:versionStorage", valueFactory.createValue(20L), PropertyType.LONG);
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      try
      {
         node.setProperty("jcr:versionStorage", "20", PropertyType.LONG);
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testSetPathProperty() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node1 = root.addNode("node1", "nt:unstructured");
      node1.setProperty("pathValue", valueFactory.createValue("/root-node/node_1", PropertyType.PATH));
      assertNotNull(session.getItem("/node1/pathValue"));
      assertEquals("/root-node/node_1", ((Property)session.getItem("/node1/pathValue")).getString());
      root.save();
      assertNotNull(session.getItem("/node1/pathValue"));
      assertEquals("/root-node/node_1", ((Property)session.getItem("/node1/pathValue")).getString());
      node1.remove();
      root.save();
      // node1.save();//impossible
   }

   public void testInvalidItemStateException() throws RepositoryException
   {
      Property p = session.getRootNode().setProperty("sameProperty", "test");

      Session session2 = repository.login(credentials, "ws");
      Property p2 = session2.getRootNode().setProperty("sameProperty", "test");
      session.save();

      try
      {
         session2.save();
         fail("InvalidItemStateException should have been thrown");
      }
      catch (ItemExistsException e)
      {
      }

   }

   public void testSetPropertySeveralTime() throws Exception
   {

      Node node = root.addNode("testNode");

      File tmpFile1 = createBLOBTempFile(250);
      node.setProperty("testProp", new FileInputStream(tmpFile1));

      File tmpFile2 = createBLOBTempFile(500);
      node.setProperty("testProp", new FileInputStream(tmpFile2));

      File tmpFile3 = createBLOBTempFile(1000);
      node.setProperty("testProp", new FileInputStream(tmpFile3));

      session.save();

      File tempFiles[] = new File[3];
      tempFiles[0] = tmpFile1;
      tempFiles[1] = tmpFile2;
      tempFiles[2] = tmpFile3;

      for (int i = 2; i < cLog.getAllStates().size(); i++)
      {
         ItemState item = cLog.getAllStates().get(i);
         // TODO doesn't pass with FileTree VS, ok with CAS if contents different
         //compareStream(((PropertyData) item.getData()).getValues().get(0).getAsStream(),
         //              new FileInputStream(tempFiles[i - 2]));
      }
   }

   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      cLog = (TransactionChangesLog)itemStates;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }
}
