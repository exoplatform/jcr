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
package org.exoplatform.services.jcr.api.importing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ExtendedWorkspace;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: AbstractImportTest.java 14244 2008-05-14 11:44:54Z ksm $
 */
public abstract class AbstractImportTest extends JcrAPIBaseTest
{
   /**
    * Initialization flag.
    */
   private static boolean isInitialized = false;

   /**
    * Logger.
    */
   private Log log = ExoLogger.getLogger("jcr.BaseImportTest");

   private final Random random = new Random();

   @Override
   public void initRepository() throws RepositoryException
   {
      super.initRepository();
      if (!isInitialized)
      {
         NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager();
         InputStream is = TestDocumentViewImport.class.getResourceAsStream("/nodetypes/ext-registry-nodetypes.xml");
         ntManager.registerNodeTypes(is, 0);
         ntManager.registerNodeTypes(TestDocumentViewImport.class
            .getResourceAsStream("/org/exoplatform/services/jcr/api/nodetypes/ecm/nodetypes-config.xml"), 0);
         ntManager.registerNodeTypes(TestDocumentViewImport.class
            .getResourceAsStream("/org/exoplatform/services/jcr/api/nodetypes/ecm/nodetypes-config-extended.xml"), 0);
         isInitialized = true;
      }
   }

   /**
    * Deserialize xml content to specific node from InputStream.
    * 
    * @param importRoot
    * @param saveType
    * @param isImportedByStream
    * @param uuidBehavior
    * @param is
    * @throws RepositoryException
    * @throws SAXException
    * @throws IOException
    */
   protected void deserialize(Node importRoot, XmlSaveType saveType, boolean isImportedByStream, int uuidBehavior,
      InputStream is) throws RepositoryException, SAXException, IOException
   {

      ExtendedSession extendedSession = (ExtendedSession)importRoot.getSession();
      ExtendedWorkspace extendedWorkspace = (ExtendedWorkspace)extendedSession.getWorkspace();
      if (isImportedByStream)
      {
         if (saveType == XmlSaveType.SESSION)
         {
            extendedSession.importXML(importRoot.getPath(), is, uuidBehavior);
         }
         else if (saveType == XmlSaveType.WORKSPACE)
         {
            extendedWorkspace.importXML(importRoot.getPath(), is, uuidBehavior);
         }

      }
      else
      {
         XMLReader reader = XMLReaderFactory.createXMLReader();
         if (saveType == XmlSaveType.SESSION)
         {
            reader.setContentHandler(extendedSession.getImportContentHandler(importRoot.getPath(), uuidBehavior));

         }
         else if (saveType == XmlSaveType.WORKSPACE)
         {
            reader.setContentHandler(extendedWorkspace.getImportContentHandler(importRoot.getPath(), uuidBehavior));
         }
         InputSource inputSource = new InputSource(is);

         reader.parse(inputSource);

      }
   }

   protected void executeSingeleThreadImportTests(int attempts, Class<? extends BeforeExportAction> firstAction,
      Class<? extends BeforeImportAction> secondAction, Class<? extends AfterImportAction> thirdAction)
      throws TransformerConfigurationException, IOException, RepositoryException, SAXException, InterruptedException
   {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      executeImportTests(executor, attempts, firstAction, secondAction, thirdAction);
      executor.shutdown();
   }

   protected void executeMultiThreadImportTests(int threadCount, int attempts,
      Class<? extends BeforeExportAction> firstAction, Class<? extends BeforeImportAction> secondAction,
      Class<? extends AfterImportAction> thirdAction) throws TransformerConfigurationException, IOException,
      RepositoryException, SAXException, InterruptedException
   {
      ExecutorService executor = Executors.newCachedThreadPool();
      executeImportTests(executor, attempts, firstAction, secondAction, thirdAction);
      executor.shutdown();
   }

   /**
    * boolean isExportedByStream, boolean isImportedByStream, boolean
    * isSystemViewExport
    * 
    * @param firstAction
    * @param secondAction
    * @param thirdAction
    * @throws TransformerConfigurationException
    * @throws IOException
    * @throws RepositoryException
    * @throws SAXException
    * @throws InterruptedException
    */
   private void executeImportTests(ExecutorService executor, int attempts,
      Class<? extends BeforeExportAction> firstAction, Class<? extends BeforeImportAction> secondAction,
      Class<? extends AfterImportAction> thirdAction) throws TransformerConfigurationException, IOException,
      RepositoryException,

      SAXException, InterruptedException
   {
      XmlSaveType[] posibleSaveTypes = new XmlSaveType[]{XmlSaveType.SESSION, XmlSaveType.WORKSPACE};

      int[] posibleImportUUIDBehaviors =
         new int[]{ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
            ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW};

      boolean[] posibleExportedByStream = new boolean[]{true, false};
      boolean[] posibleImportedByStream = new boolean[]{true, false};
      boolean[] posibleSystemViewExport = new boolean[]{true, false};

      Collection<Callable<XmlTestResult>> tasks = new ArrayList<Callable<XmlTestResult>>();
      List<Session> sessions = new ArrayList<Session>();
      for (int z = 0; z < attempts; z++)
      {

         for (int i = 0; i < posibleSaveTypes.length; i++)
         {
            for (int j = 0; j < posibleImportUUIDBehaviors.length; j++)
            {
               for (int j2 = 0; j2 < posibleExportedByStream.length; j2++)
               {
                  for (int k = 0; k < posibleImportedByStream.length; k++)
                  {
                     for (int k2 = 0; k2 < posibleSystemViewExport.length; k2++)
                     {
                        BeforeExportAction be;
                        BeforeImportAction bi;
                        AfterImportAction ai;
                        try
                        {

                           Session testSession = repository.login(credentials);
                           String testRootName = IdGenerator.generate();
                           Node testRoot = testSession.getRootNode().addNode(testRootName);
                           testSession.save();

                           be = ((BeforeExportAction)initImportExportAction(firstAction, testSession, testRoot));
                           bi = ((BeforeImportAction)initImportExportAction(secondAction, testSession, testRoot));
                           ai = ((AfterImportAction)initImportExportAction(thirdAction, testSession, testRoot));

                           if (testSession.getRootNode().hasNode(testRootName))
                           {
                              testSession.getRootNode().getNode(testRootName).remove();
                              testSession.save();

                           }

                           sessions.add(testSession);
                        }
                        catch (IllegalArgumentException e)
                        {
                           throw new RepositoryException(e);
                        }
                        catch (InstantiationException e)
                        {
                           throw new RepositoryException(e);
                        }
                        catch (IllegalAccessException e)
                        {
                           throw new RepositoryException(e);
                        }
                        catch (InvocationTargetException e)
                        {
                           throw new RepositoryException(e);
                        }

                        tasks.add(new XmlTestTask<XmlTestResult>(be, bi, ai, posibleSaveTypes[i],
                           posibleImportUUIDBehaviors[j], posibleExportedByStream[j2], posibleImportedByStream[k],
                           posibleSystemViewExport[k2]));

                     }
                  }
               }

            }
         }
      }
      executor.invokeAll(tasks);
      for (Session testSession : sessions)
      {
         testSession.logout();
      }
   }

   /**
    * Serialize content of MIX_REFERENCEABLE_NODE_NAME to byte array.
    * 
    * @param rootNode
    * @param isSystemView
    * @param isStream
    * @return
    * @throws IOException
    * @throws RepositoryException
    * @throws SAXException
    * @throws TransformerConfigurationException
    */
   protected byte[] serialize(Node exportRootNode, boolean isSystemView, boolean isStream) throws IOException,
      RepositoryException, SAXException, TransformerConfigurationException
   {

      ExtendedSession extendedSession = (ExtendedSession)exportRootNode.getSession();

      ByteArrayOutputStream outStream = new ByteArrayOutputStream();

      if (isSystemView)
      {

         if (isStream)
         {
            extendedSession.exportSystemView(exportRootNode.getPath(), outStream, false, false);
         }
         else
         {
            SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = saxFact.newTransformerHandler();
            handler.setResult(new StreamResult(outStream));
            extendedSession.exportSystemView(exportRootNode.getPath(), handler, false, false);
         }
      }
      else
      {
         if (isStream)
         {
            extendedSession.exportDocumentView(exportRootNode.getPath(), outStream, false, false);
         }
         else
         {
            SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = saxFact.newTransformerHandler();
            handler.setResult(new StreamResult(outStream));
            extendedSession.exportDocumentView(exportRootNode.getPath(), handler, false, false);
         }
      }
      outStream.close();
      return outStream.toByteArray();
   }

   protected void serialize(Node rootNode, boolean isSystemView, boolean isStream, File content) throws IOException,
      RepositoryException, SAXException, TransformerConfigurationException
   {
      ExtendedSession extendedSession = (ExtendedSession)rootNode.getSession();

      OutputStream outStream = new FileOutputStream(content);
      if (isSystemView)
      {

         if (isStream)
         {
            extendedSession.exportSystemView(rootNode.getPath(), outStream, false, false);
         }
         else
         {
            SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = saxFact.newTransformerHandler();
            handler.setResult(new StreamResult(outStream));
            extendedSession.exportSystemView(rootNode.getPath(), handler, false, false);
         }
      }
      else
      {
         if (isStream)
         {
            extendedSession.exportDocumentView(rootNode.getPath(), outStream, false, false);
         }
         else
         {
            SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = saxFact.newTransformerHandler();
            handler.setResult(new StreamResult(outStream));
            extendedSession.exportDocumentView(rootNode.getPath(), handler, false, false);
         }
      }
      outStream.close();

   }

   private ImportExportAction initImportExportAction(final Class<? extends ImportExportAction> importExportAction,
      final Session initSession, final Node testRoot) throws IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException
   {

      Constructor<?>[] constructors = importExportAction.getDeclaredConstructors();

      Constructor<? extends ImportExportAction> constructor = null;
      for (int i = 0; i < constructors.length; i++)
      {
         if (constructors[i].getParameterTypes().length > 1)
            constructor = (Constructor<? extends ImportExportAction>)constructors[i];
      }

      ImportExportAction action = constructor.newInstance(new Object[]{this, initSession, testRoot});
      return action;
   }

   protected abstract class AfterImportAction extends ImportExportAction
   {

      public AfterImportAction(Session session, Node testRootNode) throws RepositoryException
      {
         super(session, testRootNode);
      }

      @Override
      public void cleanUp() throws RepositoryException
      {
         session.refresh(false);
         Node rootNode = session.getRootNode();
         if (rootNode.hasNodes())
         {
            // clean test root
            for (NodeIterator children = rootNode.getNodes(); children.hasNext();)
            {
               Node node = children.nextNode();
               if (!node.getPath().startsWith("/jcr:system"))
               {
                  // log.info("DELETing ------------- "+node.getPath());
                  node.remove();
               }
            }
            session.save();

         }
      }
   }

   protected abstract class BeforeExportAction extends ImportExportAction
   {

      public BeforeExportAction(Session session, Node testRootNode) throws RepositoryException
      {
         super(session, testRootNode);
      }

      public abstract Node getExportRoot() throws RepositoryException;
   }

   protected abstract class BeforeImportAction extends ImportExportAction
   {

      public BeforeImportAction(Session session, Node testRootNode) throws RepositoryException
      {
         super(session, testRootNode);
      }

      public abstract Node getImportRoot() throws RepositoryException;
   }

   protected abstract class ImportExportAction
   {

      protected final Node testRootNode;

      protected final Session testSession;

      public ImportExportAction(Session testSession, Node testRootNode) throws RepositoryException
      {
         super();
         this.testSession = testSession;
         this.testRootNode = testRootNode;
      }

      public void cleanUp() throws RepositoryException
      {

      };

      public void execute() throws RepositoryException
      {
      }

   }

   private class XmlTestTask<XmlTestResult> implements Callable<XmlTestResult>
   {

      private final BeforeExportAction firstAction;

      private final XmlSaveType importSaveType;

      private final int importUUIDBehavior;

      private final boolean isExportedByStream;

      private final boolean isImportedByStream;

      private final boolean isSystemViewExport;

      private final BeforeImportAction secondAction;

      private final AfterImportAction thirdAction;

      public XmlTestTask(BeforeExportAction firstAction, BeforeImportAction secondAction,
         AfterImportAction thirdAction, XmlSaveType importSaveType, int importUUIDBehavior, boolean isExportedByStream,
         boolean isImportedByStream, boolean isSystemViewExport)
      {
         super();
         this.firstAction = firstAction;
         this.secondAction = secondAction;
         this.thirdAction = thirdAction;
         this.importSaveType = importSaveType;
         this.importUUIDBehavior = importUUIDBehavior;
         this.isExportedByStream = isExportedByStream;
         this.isImportedByStream = isImportedByStream;
         this.isSystemViewExport = isSystemViewExport;

      }

      public XmlTestResult call() throws Exception
      {
         if (log.isDebugEnabled())
            log.debug("isSys=" + isSystemViewExport + "\t" + "isES=" + isExportedByStream + "\t" + "importST="
               + importSaveType.toString() + "\t" + "isIS=" + isImportedByStream + "\t" + "importBehavior="
               + importUUIDBehavior + "\t");
         firstAction.execute();
         firstAction.cleanUp();

         byte[] buf = serialize(firstAction.getExportRoot(), isSystemViewExport, isExportedByStream);

         secondAction.execute();
         secondAction.cleanUp();

         Node importRoot = secondAction.getImportRoot();
         Exception resultException = null;
         try
         {
            deserialize(importRoot, importSaveType, isImportedByStream, importUUIDBehavior, new ByteArrayInputStream(
               buf));
            if (importSaveType.equals(XmlSaveType.SESSION))
               importRoot.getSession().save();
         }
         catch (RepositoryException e)
         {
            resultException = e;
         }
         catch (SAXException e)
         {
            resultException = e;
         }
         if (resultException != null && importUUIDBehavior != ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW)
         {

            if (resultException instanceof RepositoryException)
               throw (RepositoryException)resultException;
            else if (resultException instanceof SAXException)
               throw (SAXException)resultException;

         }

         thirdAction.execute();
         thirdAction.cleanUp();

         return null;
      }

   }

   public class XmlTestResult
   {

   }
}
