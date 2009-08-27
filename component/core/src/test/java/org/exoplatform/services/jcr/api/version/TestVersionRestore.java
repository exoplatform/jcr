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
package org.exoplatform.services.jcr.api.version;

import java.io.ByteArrayInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.exoplatform.services.jcr.impl.Constants;

/**
 * <code>TestVersionRestore</code> covers tests related to the methods of the
 * {@link javax.jcr.version.Version} class.
 * 
 * @test
 * @sources TestVersionRestore.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionTest
 * @keywords versioning
 */
public class TestVersionRestore
   extends BaseVersionTest
{

   /**
    * Tests if we add one node to versionable and checkin. Later we have to be able to restore each
    * one version created.
    * 
    * We have versionableNode node of type nt:folder. OPV is VERSION. So, we have to add a version on
    * the node child node.
    */
   public void testRestore() throws Exception
   {

      try
      {
         // Preparing versions
         Node doc = versionableNode.addNode("doc1", "nt:file");
         Node docContent = doc.addNode("jcr:content", "nt:unstructured");
         docContent.setProperty("doc1ContentProperty", "doc1 content"); // doc2/jcr:content/
         // doc1ContentProperty
         root.save();
         // Version verDoc1 = doc.checkin(); // make a version for doc1
         Version ver1 = versionableNode.checkin();
         versionableNode.checkout();

         doc = versionableNode.addNode("doc2", "nt:file");
         docContent = doc.addNode("jcr:content", "nt:unstructured");
         docContent.setProperty("doc2ContentProperty", "doc2 content"); // doc2/jcr:content/
         // doc2ContentProperty
         makeVersionable(doc);
         root.save();
         doc.checkin();
         doc.checkout();
         root.save();
         Version ver2 = versionableNode.checkin();
         versionableNode.checkout();

         log.info("VH1 " + versionableNode.getVersionHistory().getUUID());
         log.info("VH2 " + doc.getVersionHistory().getUUID());

         doc = versionableNode.addNode("doc3", "nt:file");
         doc.addNode("jcr:content", "nt:base");
         // makeVersionable(doc);
         root.save();
         Version ver3 = versionableNode.checkin();
         versionableNode.checkout();

         // Check version consistency
         // do restore ver1
         versionableNode.restore(ver1, true);

         Node doc1 = checkExisted("doc1", new String[]
         {"jcr:content/jcr:primaryType", "jcr:content/doc1ContentProperty"});

         checkNotExisted("doc2");
         checkNotExisted("doc3");

         versionableNode.checkout();
         doc1.remove();
         root.save();
         // doc1.save();ipossible to call save() on removed node

         // do restore ver2
         versionableNode.restore(ver2, true);

         doc1 = checkExisted("doc1", new String[]
         {"jcr:content/jcr:primaryType", "jcr:content/doc1ContentProperty"});
         Node doc2 = checkExisted("doc2", new String[]
         {"jcr:content/jcr:primaryType", "jcr:content/doc2ContentProperty"});

         checkNotExisted("doc3");

         return;
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         log.error("testRestore()", e);
         throw e;
      }
      catch (VersionException e)
      {
         log.error("testRestore()", e);
         throw e;
      }
      catch (LockException e)
      {
         log.error("testRestore()", e);
         throw e;
      }
      catch (RepositoryException e)
      {
         log.error("testRestore()", e);
         throw e;
      }
      catch (Exception e)
      {
         log.error("testRestore()", e);
         throw e;
      }
      // fail("An exception occurs in testRestore()");
   }

   /**
    * Test right version number calculation. We wuill create three version then delete second and
    * create one new. Tha last version must have number 4.
    */
   public void testDelete() throws Exception
   {
      try
      {
         // Preparing versions
         Node doc = versionableNode.addNode("doc1", "nt:file");
         Node docContent = doc.addNode("jcr:content", "nt:unstructured");
         docContent.setProperty("doc1ContentProperty", "doc1 content"); // doc2/jcr:content/
         // doc1ContentProperty
         root.save();
         // Version verDoc1 = doc.checkin(); // make a version for doc1
         Version ver1 = versionableNode.checkin();
         versionableNode.checkout();

         doc = versionableNode.addNode("doc2", "nt:file");
         docContent = doc.addNode("jcr:content", "nt:unstructured");
         docContent.setProperty("doc2ContentProperty", "doc2 content"); // doc2/jcr:content/
         // doc2ContentProperty
         makeVersionable(doc);
         root.save();
         doc.checkin();
         doc.checkout();
         root.save();
         Version ver2 = versionableNode.checkin();
         versionableNode.checkout();

         doc = versionableNode.addNode("doc3", "nt:file");
         doc.addNode("jcr:content", "nt:base");
         root.save();
         versionableNode.checkin();
         versionableNode.checkout();

         // do delete the ver2
         versionableNode.getVersionHistory().removeVersion(ver2.getName());

         Version ver4 = versionableNode.checkin();
         versionableNode.checkout();
         assertEquals("Version created has wrong version number", "4", ver4.getName());

         return;
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         log.error("testDelete()", e);
         throw e;
      }
      catch (VersionException e)
      {
         log.error("testDelete()", e);
         throw e;
      }
      catch (LockException e)
      {
         log.error("testDelete()", e);
         throw e;
      }
      catch (RepositoryException e)
      {
         log.error("testDelete()", e);
         throw e;
      }
      catch (Exception e)
      {
         log.error("testDelete()", e);
         throw e;
      }
   }

   /**
    * Test if Node.restore(version, relPath, removeExisted) works ok with relPath different from
    * versionable node. The problem occurs with subnodes of versionable subtree which has
    * OnParentVersion.COPY or VERSION.
    * 
    * @throws Exception
    */
   public void testRestoreRelPath() throws Exception
   {
      // prepare
      Node vnode = root.addNode("versionableNode");
      vnode.addMixin("mix:versionable");
      root.save();

      vnode.checkin();// v.1
      vnode.checkout();
      // Subnode will cause an error!!!
      vnode.addNode("Subnode").setProperty("Property", "property of subnode");
      vnode.save();

      Version v2 = vnode.checkin();// v.2

      vnode.checkout();
      vnode.addNode("Another subnode").setProperty("Property", "property of another subnode");
      vnode.save();
      vnode.checkin();// v.3
      vnode.checkout();

      // gen a relPath for a restore
      Node rnode = root.addNode("restoredNode");
      root.save();

      String relPath = "../" + rnode.getName() + "/" + vnode.getName() + "_restored";

      // test it
      vnode.restore(v2, relPath, true);
   }

   /**
    * Test if Workspace.restore works ok with existing versionable childs.
    * 
    * @throws Exception
    */
   public void testWorkspaceRestore() throws Exception
   {
      Node nodeA = root.addNode("versionableNodeA");
      nodeA.addMixin("mix:versionable");
      root.save();
      nodeA.checkin();// v.1
      nodeA.checkout();

      Node nodeB = nodeA.addNode("Subnode B");
      nodeA.save();
      nodeB.addMixin("mix:versionable");
      nodeA.save();
      nodeB.checkin();// B v.1

      Node nodeC = nodeA.addNode("Subnode C");
      nodeA.save();
      nodeC.addMixin("mix:versionable");
      nodeA.save();
      nodeC.checkin();// C v.1
      nodeC.checkout();
      nodeC.setProperty("Property Y", nodeB); // ref to Subnode B
      nodeC.save();
      Version vC = nodeC.checkin();// C v.2
      nodeC.checkout();

      nodeB.checkout();
      nodeB.setProperty("Property X", nodeC); // ref to Subnode C
      nodeB.save();
      Version vB = nodeB.checkin();// B v.2
      nodeB.checkout();

      // add some stuff
      nodeA.setProperty("Property", "property of subnode");
      nodeA.save();
      Version vA = nodeA.checkin();// v.3
      nodeA.checkout();

      nodeB.remove();
      nodeC.remove();
      nodeA.save();

      Version[] vs = new Version[]
      {vA, vB, vC};

      // test it
      session.getWorkspace().restore(vs, true);// restore A v.3, B v.2, C v.2
   }

   /**
    * Tests multiple restores of same version.
    * 
    * @throws Exception
    *           if error
    */
   public void testMultipleRestore() throws Exception
   {
      String content = "Binary content";
      byte[] bytes = content.getBytes(Constants.DEFAULT_ENCODING);

      Node file = root.addNode("testMultipleRestore_File", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", content, PropertyType.BINARY);
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      file.addMixin("mix:versionable");
      session.save();

      file.checkin(); // v1
      file.checkout();

      compareStream(new ByteArrayInputStream(bytes), file.getNode("jcr:content").getProperty("jcr:data").getStream());

      String content2 = content + " #2";
      file.getNode("jcr:content").setProperty("jcr:data", content2, PropertyType.BINARY);
      file.checkin(); // v2
      file.checkout();

      String content3 = content + " #3";
      file.getNode("jcr:content").setProperty("jcr:data", content3, PropertyType.BINARY);
      session.save();

      // restore version v2
      Version v2 = file.getBaseVersion();
      file.restore(v2, true);

      compareStream(new ByteArrayInputStream(content2.getBytes(Constants.DEFAULT_ENCODING)), file
               .getNode("jcr:content").getProperty("jcr:data").getStream());

      // restore version v1
      Version v1 = file.getBaseVersion().getPredecessors()[0];
      file.restore(v1, true);

      compareStream(new ByteArrayInputStream(bytes), file.getNode("jcr:content").getProperty("jcr:data").getStream());

      // restore version v2 again
      file.restore(v2, true);

      compareStream(new ByteArrayInputStream(content2.getBytes(Constants.DEFAULT_ENCODING)), file
               .getNode("jcr:content").getProperty("jcr:data").getStream());
   }
}
