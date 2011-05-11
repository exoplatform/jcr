package org.exoplatform.services.jcr.lab.cluster.test;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;

public class TestBLOBValue extends JcrAPIBaseTest
{
   private static final String TEST_ROOT_NAME = "TestBLOBValue";

   private static final int FILE_SIZE_KB = 512;

   private static final int FILES_COUNT = 10;

   private static File testFile;

   private Node testRoot;

   private Node addNTFile(String fileName, File blob) throws Exception
   {
      Node file = testRoot.addNode(fileName, "nt:file");

      Node contentNode = file.addNode("jcr:content", "nt:resource");
      // contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      InputStream is = new FileInputStream(blob);
      try
      {
         contentNode.setProperty("jcr:data", is);
      }
      finally
      {
         is.close();
      }
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      return file;
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      if (root.hasNode(TEST_ROOT_NAME))
      {
         testRoot = root.getNode(TEST_ROOT_NAME);
      }
      else
      {
         testRoot = root.addNode(TEST_ROOT_NAME);
         root.save();
      }

      if (testFile == null)
      {
         testFile = createBLOBTempFile(FILE_SIZE_KB);
      }
   }

   @Override
   protected void tearDown() throws Exception
   {

      if (testRoot.hasProperty("blob"))
      {
         testRoot.getProperty("blob").remove();
         testRoot.save();
      }

      if (testRoot.hasNodes())
      {
         for (NodeIterator children = testRoot.getNodes(); children.hasNext();)
         {
            children.nextNode().remove();
         }
         
         testRoot.save();
      }

      testRoot.remove();
      root.save();

      super.tearDown();
   }

   public void testAddProperty() throws Exception
   {
      // write
      Property text = testRoot.setProperty("text", "string property");

      FileInputStream fis = new FileInputStream(testFile);
      Property blob = testRoot.setProperty("blob", fis);

      testRoot.save();
      fis.close();

      // read
      Session user1 = repository.login(credentials, root.getSession().getWorkspace().getName());
      Node troot = user1.getRootNode().getNode(TEST_ROOT_NAME);
      Property tblob = troot.getProperty("blob");
      InputStream blobStream = tblob.getStream();

      byte[] buff = new byte[1024];
      int r = 0;
      int size = 0;
      while ((r = blobStream.read(buff)) >= 0)
      {
         size += r;
      }

      assertEquals(testFile.length(), size);
   }

   // for read on another node of a cluster 
   public void _testReadProperty() throws Exception
   {
      // read
      Property blob = testRoot.getProperty("blob");
      InputStream blobStream = blob.getStream();

      byte[] buff = new byte[1024];
      int r = 0;
      int size = 0;
      while ((r = blobStream.read(buff)) >= 0)
      {
         size += r;
      }

      assertEquals(testFile.length(), size);
   }

   public void testAddNTFiles() throws Exception
   {
      // write series of FILES_COUNT adding to each next 1K bytes
      // do it in one save
      int size = FILE_SIZE_KB;
      for (int i = 0; i < FILES_COUNT; i++)
      {
         File blob = createBLOBTempFile(size);
         addNTFile("file" + i, blob);

         size += 1;
      }

      testRoot.save();

      // it's ready for read
   }

   // for read on another node of a cluster
   public void _testReadNTFiles() throws Exception
   {
      // read series of FILES_COUNT were added each with size + 1K bytes
      int size = FILE_SIZE_KB;
      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node file = testRoot.getNode("file" + i);

         Property blob = file.getProperty("jcr:content/jcr:data");
         InputStream blobStream = blob.getStream();

         byte[] buff = new byte[1024];
         int r = 0;
         int bsize = 0;
         while ((r = blobStream.read(buff)) >= 0)
         {
            bsize += r;
         }

         assertEquals(size * 1024, bsize);

         // calc next
         size += 1;
      }
   }
}

