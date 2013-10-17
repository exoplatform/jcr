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

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionHistory;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 18.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestVersionable.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestVersionable extends BaseVersionTest
{

   private Node testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("testRemoveVersionable");
      root.save();
      testRoot.addMixin("mix:versionable");
      root.save();

      testRoot.checkin();
      testRoot.checkout();

      testRoot.addNode("node1");
      testRoot.addNode("node2").setProperty("prop1", "a property #1");
      testRoot.save();

      testRoot.checkin();
      testRoot.checkout();

      testRoot.getNode("node1").remove();
      testRoot.save();

      testRoot.checkin();
   }

   @Override
   protected void tearDown() throws Exception
   {
      testRoot.remove();
      root.save();

      super.tearDown();
   }

   public void testRemoveMixVersionable() throws Exception
   {

      testRoot.checkout();

      try
      {
         testRoot.removeMixin("mix:versionable");
         testRoot.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("removeMixin(\"mix:versionable\") impossible due to error " + e.getMessage());
      }
   }

   public void testRemoveVersionableFile() throws Exception
   {
      int versionsCount = 5;
      Node testFolder = root.addNode("testVFolader", "nt:folder");
      testFolder.addMixin("mix:versionable");
      Random r = new Random();
      byte[] content = new byte[1024 * 1024];
      r.nextBytes(content);

      Node localSmallFile = testFolder.addNode("smallFile", "nt:file");
      Node contentNode = localSmallFile.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream(content));
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      contentNode.addMixin("mix:versionable");
      session.save();
      String vsUuid = contentNode.getProperty("jcr:versionHistory").getString();
      for (int i = 0; i < versionsCount; i++)
      {
         contentNode.checkin();
         contentNode.checkout();
         r.nextBytes(content);
         contentNode.setProperty("jcr:data", new ByteArrayInputStream(content));

         session.save();
      }
      testFolder.remove();
      session.save();
      try
      {
         session.getNodeByUUID(vsUuid);
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }

   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-437
    * 
    * @throws Exception
    */
   public void testRemoveNonMixVersionableParent() throws Exception
   {
      Node testroot = root.addNode("testRoot");
      Node verionableChild = testroot.addNode("verionableChild");
      verionableChild.addMixin("mix:versionable");
      root.save();
      assertFalse(testroot.isNodeType("mix:versionable"));
      assertTrue(verionableChild.isNodeType("mix:versionable"));

      VersionHistory vHistory = verionableChild.getVersionHistory();
      assertNotNull(vHistory);

      String vhId = ((NodeImpl)vHistory).getUUID();

      assertNotNull(session.getTransientNodesManager().getItemData(vhId));
      testroot.remove();
      root.save();
      assertFalse(root.hasNode("testRoot"));
      ItemData vhdata = session.getTransientNodesManager().getItemData(vhId);
      // !!!!
      assertNull(vhdata);
   }

   public void testRemoveMixVersionableTwice() throws Exception
   {

      testRoot.checkout();

      testRoot.removeMixin("mix:versionable");
      testRoot.save();

      try
      {
         testRoot.removeMixin("mix:versionable");
         fail("removeMixin(\"mix:versionable\") should throw NoSuchNodeTypeException exception");
      }
      catch (NoSuchNodeTypeException e)
      {
         // ok
      }
   }

   public void testIsCheckedOut() throws Exception
   {
      // create versionable subnode and checkin its versionable parent
      // testRoot - versionable ancestor

      testRoot.checkout();
      Node subNode = testRoot.addNode("node1").addNode("node2").addNode("subNode");
      testRoot.save();

      subNode.addMixin("mix:versionable");
      testRoot.save();

      subNode.checkin();
      subNode.checkout();
      subNode.setProperty("property1", "property1 v1");
      subNode.save();
      subNode.checkin();
      subNode.checkout();

      // test
      testRoot.checkin(); // make subtree checked-in
      try
      {
         assertTrue("subNode should be checked-out as it's a mix:versionable", subNode.isCheckedOut());
      }
      catch (RepositoryException e)
      {

      }
   }

   public void testJCR1438() throws Exception
   {
      // node content
      Node testRoot = root.addNode("testRoot");
      root.save();

      Node web = testRoot.addNode("web", "exo:webContent");

      web.addMixin("exo:datetime");
      web.addMixin("exo:owneable");
      web.addMixin("exo:modify");
      web.addMixin("mix:votable");
      web.addMixin("mix:commentable");
      web.addMixin("publication:stateAndVersionBasedPublication");
      web.addMixin("mix:versionable");

      web.setProperty("exo:dateCreated", Calendar.getInstance());
      web.setProperty("exo:dateModified", Calendar.getInstance());
      web.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      web.setProperty("exo:lastModifier", "root");
      web.setProperty("exo:summary", "text summary");
      web.setProperty("exo:title", "web title");
      web.setProperty("exo:voteTotal", "1");
      web.setProperty("exo:voteTotalOfLang", "1");
      web.setProperty("exo:votingRate", "1");
      web.setProperty("publication:currentState", "draft");
      web.setProperty("publication:history", new String[]{"13", "12", "14"});
      web.setProperty("publication:lifecycleName", "lf_name");
      web.setProperty("publication:revisionData", new String[]{"r_data_1", "r_data_2"});

      Node defHtml = web.addNode("default.html", "nt:file");
      defHtml.addMixin("exo:datetime");
      defHtml.addMixin("exo:owneable");
      defHtml.addMixin("exo:modify");
      defHtml.addMixin("exo:htmlFile");

      defHtml.setProperty("exo:dateCreated", Calendar.getInstance());
      defHtml.setProperty("exo:dateModified", Calendar.getInstance());
      defHtml.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defHtml.setProperty("exo:lastModifier", "root");

      Node contentDefHtml = defHtml.addNode("jcr:content", "nt:resource");
      contentDefHtml.addMixin("exo:datetime");
      contentDefHtml.addMixin("exo:owneable");
      contentDefHtml.addMixin("dc:elementSet");

      contentDefHtml.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefHtml.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:data", "def_html_data");
      contentDefHtml.setProperty("jcr:encoding", "UTF-8");
      contentDefHtml.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:mimeType", "text/html");

      Node css = web.addNode("css", "exo:cssFolder");
      css.addMixin("exo:datetime");
      css.addMixin("exo:owneable");
      css.addMixin("exo:modify");

      css.setProperty("exo:dateCreated", Calendar.getInstance());
      css.setProperty("exo:dateModified", Calendar.getInstance());
      css.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      css.setProperty("exo:lastModifier", "root");

      Node defCss = css.addNode("default.css", "nt:file");

      defCss.addMixin("exo:datetime");
      defCss.addMixin("exo:owneable");
      defCss.addMixin("exo:modify");

      defCss.setProperty("exo:dateCreated", Calendar.getInstance());
      defCss.setProperty("exo:dateModified", Calendar.getInstance());
      defCss.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defCss.setProperty("exo:lastModifier", "root");

      Node contentDefCss = defCss.addNode("jcr:content", "nt:resource");
      contentDefCss.addMixin("exo:datetime");
      contentDefCss.addMixin("exo:owneable");
      contentDefCss.addMixin("dc:elementSet");

      contentDefCss.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefCss.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefCss.setProperty("jcr:data", "def_css_data");
      contentDefCss.setProperty("jcr:encoding", "UTF-8");
      contentDefCss.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefCss.setProperty("jcr:mimeType", "text/css");

      Node medias = web.addNode("medias", "exo:multimediaFolder");
      medias.addMixin("exo:datetime");
      medias.addMixin("exo:owneable");
      medias.addMixin("exo:modify");

      medias.setProperty("exo:dateCreated", Calendar.getInstance());
      medias.setProperty("exo:dateModified", Calendar.getInstance());
      medias.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      medias.setProperty("exo:lastModifier", "root");

      Node videos = medias.addNode("videos", "nt:folder");
      videos.addMixin("exo:datetime");
      videos.addMixin("exo:owneable");
      videos.addMixin("exo:modify");

      videos.setProperty("exo:dateCreated", Calendar.getInstance());
      videos.setProperty("exo:dateModified", Calendar.getInstance());
      videos.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      videos.setProperty("exo:lastModifier", "root");

      Node images = medias.addNode("images", "nt:folder");
      images.addMixin("exo:datetime");
      images.addMixin("exo:owneable");
      images.addMixin("exo:modify");

      images.setProperty("exo:dateCreated", Calendar.getInstance());
      images.setProperty("exo:dateModified", Calendar.getInstance());
      images.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      images.setProperty("exo:lastModifier", "root");

      Node illustration = images.addNode("illustration", "nt:file");
      illustration.addMixin("exo:datetime");
      illustration.addMixin("exo:owneable");
      illustration.addMixin("exo:modify");
      illustration.addMixin("mix:referenceable");

      illustration.setProperty("exo:dateCreated", Calendar.getInstance());
      illustration.setProperty("exo:dateModified", Calendar.getInstance());
      illustration.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      illustration.setProperty("exo:lastModifier", "root");

      Node contentIllustration = illustration.addNode("jcr:content", "nt:resource");
      contentIllustration.addMixin("exo:datetime");
      contentIllustration.addMixin("exo:owneable");
      contentIllustration.addMixin("dc:elementSet");

      contentIllustration.setProperty("exo:dateCreated", Calendar.getInstance());
      contentIllustration.setProperty("exo:dateModified", Calendar.getInstance());
      contentIllustration.setProperty("jcr:data", "illustration_data");
      contentIllustration.setProperty("jcr:encoding", "UTF-8");
      contentIllustration.setProperty("jcr:lastModified", Calendar.getInstance());
      contentIllustration.setProperty("jcr:mimeType", "text/jpeg");

      Node documents = web.addNode("documents", "nt:unstructured");
      documents.addMixin("exo:datetime");
      documents.addMixin("exo:owneable");
      documents.addMixin("exo:modify");
      documents.addMixin("exo:documentFolder");

      documents.setProperty("exo:dateCreated", Calendar.getInstance());
      documents.setProperty("exo:dateModified", Calendar.getInstance());
      documents.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      documents.setProperty("exo:lastModifier", "root");

      Node js = web.addNode("js", "exo:jsFolder");
      js.addMixin("exo:datetime");
      js.addMixin("exo:owneable");
      js.addMixin("exo:modify");

      js.setProperty("exo:dateCreated", Calendar.getInstance());
      js.setProperty("exo:dateModified", Calendar.getInstance());
      js.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      js.setProperty("exo:lastModifier", "root");

      Node defJs = js.addNode("default.js", "nt:file");
      defJs.addMixin("exo:datetime");
      defJs.addMixin("exo:owneable");
      defJs.addMixin("exo:modify");

      defJs.setProperty("exo:dateCreated", Calendar.getInstance());
      defJs.setProperty("exo:dateModified", Calendar.getInstance());
      defJs.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defJs.setProperty("exo:lastModifier", "root");

      Node contentDefJs = defJs.addNode("jcr:content", "nt:resource");
      contentDefJs.addMixin("exo:datetime");
      contentDefJs.addMixin("exo:owneable");
      contentDefJs.addMixin("dc:elementSet");

      contentDefJs.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefJs.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefJs.setProperty("jcr:data", "def_js_data");
      contentDefJs.setProperty("jcr:encoding", "UTF-8");
      contentDefJs.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefJs.setProperty("jcr:mimeType", "text/js");

      root.save();

      web.checkin();
      web.checkout();
      root.save();

      Node testImage = images.addNode("test_image", "nt:file");
      testImage.addMixin("exo:datetime");
      testImage.addMixin("exo:owneable");
      testImage.addMixin("exo:modify");
      testImage.addMixin("mix:referenceable");
      testImage.addMixin("mix:versionable");

      testImage.setProperty("exo:dateCreated", Calendar.getInstance());
      testImage.setProperty("exo:dateModified", Calendar.getInstance());
      testImage.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      testImage.setProperty("exo:lastModifier", "root");

      Node contentTestImage = testImage.addNode("jcr:content", "nt:resource");
      contentTestImage.addMixin("exo:datetime");
      contentTestImage.addMixin("exo:owneable");
      contentTestImage.addMixin("dc:elementSet");

      contentTestImage.setProperty("exo:dateCreated", Calendar.getInstance());
      contentTestImage.setProperty("exo:dateModified", Calendar.getInstance());
      contentTestImage.setProperty("jcr:data", "content_test_image_data");
      contentTestImage.setProperty("jcr:encoding", "UTF-8");
      contentTestImage.setProperty("jcr:lastModified", Calendar.getInstance());
      contentTestImage.setProperty("jcr:mimeType", "text/jpeg");
      root.save();

      testImage.checkin();
      testImage.checkout();
      testImage.save();

      web.checkin();
      web.checkout();
      root.save();

      Node rTestImage = web.getNode("medias").getNode("images").getNode("test_image");
      assertNotNull(rTestImage);
      Node rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());

      web.restore("2", true);
      root.save();

      rTestImage = web.getNode("medias").getNode("images").getNode("test_image");
      assertNotNull(rTestImage);
      rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());

   }

   public void testJCR1438_Simple() throws Exception
   {
      Node testRoot = root.addNode("testRoot");
      root.save();

      Node l1Node = testRoot.addNode("l1");
      l1Node.addMixin("mix:versionable");
      Node folder = l1Node.addNode("folder", "nt:folder");
      root.save();

      l1Node.checkin();
      l1Node.checkout();
      root.save();

      Node testImage = folder.addNode("test_image", "nt:file");
      testImage.addMixin("mix:versionable");

      Node contentTestImage = testImage.addNode("jcr:content", "nt:resource");
      contentTestImage.setProperty("jcr:data", "content_test_image_data");
      contentTestImage.setProperty("jcr:encoding", "UTF-8");
      contentTestImage.setProperty("jcr:lastModified", Calendar.getInstance());
      contentTestImage.setProperty("jcr:mimeType", "text/jpeg");
      root.save();

      testImage.checkin();
      testImage.checkout();
      testImage.save();

      l1Node.checkin();
      l1Node.checkout();
      root.save();

      Node rTestImage = testRoot.getNode("l1").getNode("folder").getNode("test_image");
      assertNotNull(rTestImage);
      Node rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());

      /*testImage.remove();
      root.save();*/

      l1Node.restore("2", true);

      rTestImage = testRoot.getNode("l1").getNode("folder").getNode("test_image");
      assertNotNull(rTestImage);
      rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());
   }

   public void testJCR1438_Simple2() throws Exception
   {
      Node testRoot = root.addNode("testRoot");
      root.save();

      Node l1Node = testRoot.addNode("l1");
      l1Node.addMixin("mix:versionable");
      Node folder = l1Node.addNode("folder", "nt:folder");
      root.save();

      l1Node.checkin();
      l1Node.checkout();
      root.save();

      Node folderL2 = folder.addNode("folder_l2", "nt:folder");
      folderL2.addMixin("mix:versionable");

      Node testImage = folderL2.addNode("test_image", "nt:file");

      Node contentTestImage = testImage.addNode("jcr:content", "nt:resource");
      contentTestImage.setProperty("jcr:data", "content_test_image_data");
      contentTestImage.setProperty("jcr:encoding", "UTF-8");
      contentTestImage.setProperty("jcr:lastModified", Calendar.getInstance());
      contentTestImage.setProperty("jcr:mimeType", "text/jpeg");
      root.save();

      folderL2.checkin();
      folderL2.checkout();
      folderL2.save();

      // sub version node l2
      Node folderL3 = folderL2.addNode("folder_l3", "nt:folder");
      folderL3.addMixin("mix:versionable");

      testImage = folderL3.addNode("test_image", "nt:file");

      contentTestImage = testImage.addNode("jcr:content", "nt:resource");
      contentTestImage.setProperty("jcr:data", "content_test_image_data");
      contentTestImage.setProperty("jcr:encoding", "UTF-8");
      contentTestImage.setProperty("jcr:lastModified", Calendar.getInstance());
      contentTestImage.setProperty("jcr:mimeType", "text/jpeg");
      root.save();

      folderL3.checkin();
      folderL3.checkout();
      folderL3.save();
      //

      folderL2.checkin();
      folderL2.checkout();
      folderL2.save();

      l1Node.checkin();
      l1Node.checkout();
      root.save();

      Node rTestImage = testRoot.getNode("l1").getNode("folder").getNode("folder_l2").getNode("test_image");
      assertNotNull(rTestImage);
      Node rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());

      rTestImage =
         testRoot.getNode("l1").getNode("folder").getNode("folder_l2").getNode("folder_l3").getNode("test_image");
      assertNotNull(rTestImage);
      rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());

      l1Node.restore("2", true);

      rTestImage = testRoot.getNode("l1").getNode("folder").getNode("folder_l2").getNode("test_image");
      assertNotNull(rTestImage);
      rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());

      rTestImage =
         testRoot.getNode("l1").getNode("folder").getNode("folder_l2").getNode("folder_l3").getNode("test_image");
      assertNotNull(rTestImage);
      rTestImageContent = rTestImage.getNode("jcr:content");
      assertNotNull(rTestImageContent);
      assertNotNull(rTestImageContent.getProperty("jcr:data"));
      assertEquals("content_test_image_data", rTestImageContent.getProperty("jcr:data").getString());
   }

   public void testMultiThreading1OnWS1() throws Exception
   {
      Session s = repository.login(credentials, "ws1");
      Node testMultiThreading = s.getRootNode().addNode("testMultiThreading1");
      s.save();
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      final String path = testMultiThreading.getPath();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            boolean beforeAwait = true;
            Session session = null;
            try
            {
               session = repository.login(credentials, "ws1");
               Node testMultiThreading = (Node)session.getItem(path);
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (ItemExistsException e)
            {
               // The mixin has been added several times so it is normal to have such issue
            }
            catch (RepositoryException e)
            {
               if (beforeAwait)
                  ex.set(e);
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // reload the node
      testMultiThreading = (Node)s.getItem(path);
      assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
      assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
      s.logout();
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading2OnWS1() throws Exception
   {
      Session s = repository.login(credentials, "ws1");
      Node testMultiThreading = s.getRootNode().addNode("testMultiThreading2");
      testMultiThreading.addMixin("mix:referenceable");
      s.save();
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      final String path = testMultiThreading.getPath();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            Session session = null;
            boolean beforeAwait = true;
            try
            {
               session = repository.login(credentials, "ws1");
               Node testMultiThreading = (Node)session.getItem(path);
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (RepositoryException e)
            {
               // Concurrent modifications of the property mixinTypes
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // reload the node
      testMultiThreading = (Node)s.getItem(path);
      assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
      Set<String> mixins = new HashSet<String>();
      mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
      mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
      assertTrue(mixins.contains("mix:referenceable"));
      assertTrue(mixins.contains("mix:versionable"));
      s.logout();
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading3OnWS1() throws Exception
   {
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            Session session = null;
            boolean beforeAwait = true;
            try
            {
               session = repository.login(credentials, "ws1");
               Node testMultiThreading = session.getRootNode().addNode("testMultiThreading3");
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (ItemExistsException e)
            {
               // The node has not been created so far
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // load the node
      Session s = repository.login(credentials, "ws1");
      Node testMultiThreading = (Node)s.getItem("/testMultiThreading3");
      assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
      assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
      assertFalse(s.itemExists("/testMultiThreading3[2]"));
      s.logout();
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading4OnWS1() throws Exception
   {
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            Session session = null;
            boolean beforeAwait = true;
            try
            {
               session = repository.login(credentials, "ws1");
               Node testMultiThreading = session.getRootNode().addNode("testMultiThreading4");
               testMultiThreading.addMixin("mix:referenceable");
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (ItemExistsException e)
            {
               // The node has not been created so far
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // load the node
      Session s = repository.login(credentials, "ws1");
      Node testMultiThreading = (Node)s.getItem("/testMultiThreading4");
      assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
      Set<String> mixins = new HashSet<String>();
      mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
      mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
      assertTrue(mixins.contains("mix:referenceable"));
      assertTrue(mixins.contains("mix:versionable"));
      assertFalse(s.itemExists("/testMultiThreading4[2]"));
      s.logout();
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading1OnWS() throws Exception
   {
      Node testMultiThreading = root.addNode("testMultiThreading1");
      root.save();
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      final String path = testMultiThreading.getPath();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            boolean beforeAwait = true;
            Session session = null;
            try
            {
               session = repository.login(credentials, "ws");
               Node testMultiThreading = (Node)session.getItem(path);
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (ItemExistsException e)
            {
               // The mixin has been added several times so it is normal to have such issue
            }
            catch (RepositoryException e)
            {
               if (beforeAwait)
                  ex.set(e);
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // reload the node
      testMultiThreading = (Node)session.getItem(path);
      assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
      assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading2OnWS() throws Exception
   {
      Node testMultiThreading = root.addNode("testMultiThreading2");
      testMultiThreading.addMixin("mix:referenceable");
      root.save();
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      final String path = testMultiThreading.getPath();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            Session session = null;
            boolean beforeAwait = true;
            try
            {
               session = repository.login(credentials, "ws");
               Node testMultiThreading = (Node)session.getItem(path);
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (RepositoryException e)
            {
               // Concurrent modifications of the property mixinTypes
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // reload the node
      testMultiThreading = (Node)session.getItem(path);
      assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
      Set<String> mixins = new HashSet<String>();
      mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
      mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
      assertTrue(mixins.contains("mix:referenceable"));
      assertTrue(mixins.contains("mix:versionable"));
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading3OnWS() throws Exception
   {
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            Session session = null;
            boolean beforeAwait = true;
            try
            {
               session = repository.login(credentials, "ws");
               Node testMultiThreading = session.getRootNode().addNode("testMultiThreading3");
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (ItemExistsException e)
            {
               // The node has not been created so far
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // load the node
      Node testMultiThreading = (Node)session.getItem("/testMultiThreading3");
      assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
      assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
      assertFalse(session.itemExists("/testMultiThreading3[2]"));
      if (ex.get() != null)
         throw ex.get();
   }

   public void testMultiThreading4OnWS() throws Exception
   {
      ExtendedNode vs = (ExtendedNode)session.getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      long totalNodes = vs.getNodesCount();
      int threadCount = 5;
      final CyclicBarrier startSignal = new CyclicBarrier(threadCount);
      final CountDownLatch endSignal = new CountDownLatch(threadCount);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      Runnable r = new Runnable()
      {
         public void run()
         {
            Session session = null;
            boolean beforeAwait = true;
            try
            {
               session = repository.login(credentials, "ws");
               Node testMultiThreading = session.getRootNode().addNode("testMultiThreading4");
               testMultiThreading.addMixin("mix:referenceable");
               testMultiThreading.addMixin("mix:versionable");
               startSignal.await();
               beforeAwait = false;
               session.save();
            }
            catch (ItemExistsException e)
            {
               // The node has not been created so far
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (beforeAwait)
               {
                  try
                  {
                     startSignal.await();
                  }
                  catch (Exception e)
                  {
                     // ignore me
                  }
               }
               endSignal.countDown();
               if (session != null)
                  session.logout();
            }
         }
      };
      for (int i = 0; i < threadCount; i++)
         new Thread(r).start();
      endSignal.await();
      assertEquals(totalNodes + 1, vs.getNodesCount());
      assertEquals(totalNodes + 1, vs.getNodes().getSize());
      // load the node
      Node testMultiThreading = (Node)session.getItem("/testMultiThreading4");
      assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
      Set<String> mixins = new HashSet<String>();
      mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
      mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
      assertTrue(mixins.contains("mix:referenceable"));
      assertTrue(mixins.contains("mix:versionable"));
      assertFalse(session.itemExists("/testMultiThreading4[2]"));
      if (ex.get() != null)
         throw ex.get();
   }
}
