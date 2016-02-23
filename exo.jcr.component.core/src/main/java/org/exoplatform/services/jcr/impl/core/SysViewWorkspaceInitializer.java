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
package org.exoplatform.services.jcr.impl.core;

import org.apache.ws.commons.util.Base64;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.jcr.impl.xml.importing.ACLInitializationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;

/**
 * Created by The eXo Platform SAS. <br>
 * 
 * Restores workspace from ready backupset. <br>
 * Should be configured with restore-path parameter. The path to a backup result file.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */

public class SysViewWorkspaceInitializer implements WorkspaceInitializer
{

   public static final String RESTORE_PATH_PARAMETER = "restore-path";

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceInitializer");

   protected final String workspaceName;

   /**
    * Workspace entry.
    */
   protected final WorkspaceEntry workspaceEntry;

   /**
    * Repository Entry.
    */
   protected final RepositoryEntry repositoryEntry;

   protected final DataManager dataManager;

   private final NamespaceRegistryImpl namespaceRegistry;

   private final LocationFactory locationFactory;

   protected String restorePath;

   protected final SpoolConfig spoolConfig;

   /**
    * Indicates if restore action in progress or not.
    */
   volatile private boolean isRestoreInProgress = false;

   protected class TempOutputStream extends ByteArrayOutputStream
   {

      byte[] getBuffer()
      {
         return buf;
      }

      int getSize()
      {
         return buf.length;
      }

      @Override
      public void close()
      {
         buf = null;
      }
   }

   protected abstract class ValueWriter
   {

      /**
       * Close writer. Should be called before getXXX method.
       * 
       * @throws IOException
       */
      abstract void close() throws IOException;

      /**
       * Write text.
       * 
       * @param text
       * @throws IOException
       */
      abstract void write(String text) throws IOException;

      /**
       * Return true if data is textual. False - if binary data.
       * 
       * @return
       */
      abstract boolean isText();

      /**
       * Get binary data.
       * 
       * @return
       * @throws IOException
       */
      abstract File getFile() throws IOException;

      /**
       * Get textual data.
       * 
       * @return
       * @throws IOException
       */
      abstract String getText() throws IOException;
   }

   protected class StringValueWriter extends ValueWriter
   {
      final StringBuilder string = new StringBuilder();

      @Override
      void close()
      {
         // do nothing
      }

      @Override
      void write(String text)
      {
         this.string.append(text);
      }

      @Override
      boolean isText()
      {
         return true;
      }

      @Override
      File getFile() throws IOException
      {
         // should never be used!!!
         throw new IOException("StringValueWriter.getInputStream() not supported. Use getText() instead.");
      }

      @Override
      String getText()
      {
         return this.string.toString();
      }
   }

   protected class BinaryValueWriter extends ValueWriter
   {
      final Base64Decoder decoder = new Base64Decoder();

      @Override
      void close() throws IOException
      {
         this.decoder.close();
      }

      @Override
      void write(String text) throws IOException
      {
         this.decoder.write(text.toCharArray(), 0, text.length());
      }

      @Override
      File getFile() throws IOException
      {
         return this.decoder.getFile();
      }

      @Override
      String getText() throws IOException
      {
         return new String(this.decoder.getByteArray() != null ? this.decoder.getByteArray() : new byte[0]);
      }

      @Override
      boolean isText()
      {
         return !this.decoder.isBuffered();
      }
   }

   protected class Base64Decoder extends Base64.Decoder
   {

      private File tmpFile;

      private OutputStream buff;

      Base64Decoder()
      {
         super(spoolConfig.maxBufferSize);
      }

      @Override
      protected void writeBuffer(byte[] buffer, int offset, int len) throws IOException
      {
         if (buff == null)
         {
            if (buffer.length >= spoolConfig.maxBufferSize)
            {
               buff =
                  PrivilegedFileHelper.fileOutputStream(tmpFile =
                     SpoolFile.createTempFile("jcrrestorewi", ".tmp", spoolConfig.tempDirectory));
            }
            else
            {
               buff = new TempOutputStream();
            }
         }
         else if (tmpFile == null && (((TempOutputStream)buff).getSize() + buffer.length) > spoolConfig.maxBufferSize)
         {
            // spool to file
            FileOutputStream fout =
               PrivilegedFileHelper.fileOutputStream(tmpFile =
                  SpoolFile.createTempFile("jcrrestorewi", ".tmp", spoolConfig.tempDirectory));
            fout.write(((TempOutputStream)buff).getBuffer());
            buff.close();
            buff = fout; // use file
         }

         buff.write(buffer, offset, len);
      }

      void close() throws IOException
      {
         super.flush();

         if (buff != null)
         {
            buff.close();
         }
      }

      File getFile() throws IOException
      {
         return tmpFile;
      }

      byte[] getByteArray() throws IOException
      {
         if (buff != null)
            return ((TempOutputStream)buff).getBuffer();
         else
            return null;
      }

      boolean isBuffered()
      {
         return tmpFile != null;
      }
   }

   protected class SVNodeData extends TransientNodeData
   {

      int orderNumber = 0;

      private String exoOwner;

      private List<String> exoPrivileges;

      HashMap<InternalQName, Integer> childNodesMap = new HashMap<InternalQName, Integer>();

      SVNodeData(QPath path, String identifier, String parentIdentifier, int version, int orderNum)
      {
         super(path, identifier, version, null, null, orderNum, parentIdentifier, null);
      }

      void setPrimartTypeName(InternalQName primaryTypeName)
      {
         this.primaryTypeName = primaryTypeName;
      }

      public void setMixinTypeNames(InternalQName[] mixinTypeNames)
      {
         this.mixinTypeNames = mixinTypeNames;
      }

      public String getExoOwner()
      {
         return exoOwner;
      }

      public List<String> getExoPrivileges()
      {
         return exoPrivileges;
      }

      public void setExoOwner(String exoOwner)
      {
         this.exoOwner = exoOwner;
      }

      public void setExoPrivileges(List<String> exoPrivileges)
      {
         this.exoPrivileges = exoPrivileges;
      }

      public void setACL(AccessControlList acl)
      {
         this.acl = acl;
      }

      /**
       * Add name of child node.
       * 
       * @return array of added node orderNumber and index
       */
      int[] addChildNode(InternalQName childName)
      {
         Integer count = childNodesMap.get(childName);
         if (count != null)
         {
            childNodesMap.put(childName, count + 1);
         }
         else
            childNodesMap.put(childName, 1);

         int index = childNodesMap.get(childName);
         return new int[]{orderNumber++, index};
      }
   }

   protected class SVPropertyData extends TransientPropertyData
   {

      SVPropertyData(QPath path, String identifier, int version, int type, String parentIdentifier, boolean multivalued)
      {
         super(path, identifier, version, type, parentIdentifier, multivalued);
         this.values = new ArrayList<ValueData>();
      }

      public void setMultiValued(boolean multiValued)
      {
         this.multiValued = multiValued;
      }

   }

   /**
    * SysViewWorkspaceInitializer constructor.
    * 
    * @param config
    *          WorkspaceEntry
    * @param repConfig
    *          RepositoryEntry
    * @param dataManager
    *          CacheableWorkspaceDataManager
    * @param namespaceRegistry
    *          NamespaceRegistryImpl
    * @param locationFactory
    *          LocationFactory
    * @param nodeTypeManager
    *          NodeTypeManagerImpl
    * @param valueFactory
    *          ValueFactoryImpl
    * @param accessManager
    *          AccessManager
    * @throws RepositoryConfigurationException
    *           if configuration restore-path is null
    * @throws PathNotFoundException
    *           if path not found
    * @throws RepositoryException
    *           if Repository error
    */
   public SysViewWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory,
      AccessManager accessManager) throws RepositoryConfigurationException, PathNotFoundException, RepositoryException
   {
      this(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
         accessManager, config.getInitializer().getParameterValue(RESTORE_PATH_PARAMETER, null));
   }

   /**
    * SysViewWorkspaceInitializer constructor.
    * 
    * @param config
    *          WorkspaceEntry
    * @param repConfig
    *          RepositoryEntry
    * @param dataManager
    *          CacheableWorkspaceDataManager
    * @param namespaceRegistry
    *          NamespaceRegistryImpl
    * @param locationFactory
    *          LocationFactory
    * @param nodeTypeManager
    *          NodeTypeManagerImpl
    * @param valueFactory
    *          ValueFactoryImpl
    * @param accessManager
    *          AccessManager
    * @param restorePath
    *          String
    * @throws RepositoryException
    *           if Repository error
    */
   public SysViewWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory,
      AccessManager accessManager, String restorePath) throws RepositoryException
   {
      this.workspaceEntry = config;
      this.workspaceName = config.getName();

      this.repositoryEntry = repConfig;

      this.dataManager = dataManager;

      this.namespaceRegistry = namespaceRegistry;
      this.locationFactory = locationFactory;

      this.spoolConfig = valueFactory.getSpoolConfig();

      this.restorePath = restorePath;

      if (this.restorePath == null)
      {
         throw new RepositoryException(RESTORE_PATH_PARAMETER + " is absent in workpsace [" + workspaceName
            + "] configuration ");
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeData initWorkspace() throws RepositoryException
   {
      if (isWorkspaceInitialized())
      {
         return (NodeData)dataManager.getItemData(Constants.ROOT_UUID);
      }

      long start = System.currentTimeMillis();

      isRestoreInProgress = true;
      try
      {
         doRestore();
      }
      catch (Throwable e) //NOSONAR
      {
         throw new RepositoryException(e);
      }
      finally
      {
         isRestoreInProgress = false;
      }

      final NodeData root = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

      LOG.info("Workspace [" + workspaceName + "] restored from storage " + restorePath + " in "
         + (System.currentTimeMillis() - start) * 1d / 1000 + "sec");

      return root;
   }

   /**
    * Perform restore operation.
    */
   protected void doRestore() throws Throwable
   {
      PlainChangesLog changes = read();

      TransactionChangesLog tLog = new TransactionChangesLog(changes);
      tLog.setSystemId(Constants.JCR_CORE_RESTORE_WORKSPACE_INITIALIZER_SYSTEM_ID); // mark changes

      dataManager.save(tLog);
   }

   /**
    * Parse of SysView export content and fill changes log within it.
    * 
    * @throws XMLStreamException
    *           if stream data corrupted
    * @throws FactoryConfigurationError
    *           if XML factory configured bad
    * @throws IOException
    *           fi IO error
    * @throws RepositoryException
    *           if Repository error
    * @throws NamespaceException
    *           if namespace is not registered
    * @throws IllegalNameException
    *           if illegal name
    */
   protected PlainChangesLog read() throws XMLStreamException, FactoryConfigurationError, IOException,
      NamespaceException, RepositoryException, IllegalNameException
   {

      InputStream input = PrivilegedFileHelper.fileInputStream(restorePath);
      try
      {
         XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);

         // SV prefix URIs
         String svURI = null;
         String exoURI = null;

         PlainChangesLog changes = new PlainChangesLogImpl();

         // SVNodeData currentNode = null;
         Stack<SVNodeData> parents = new Stack<SVNodeData>();

         SVPropertyData currentProperty = null;

         ValueWriter propertyValue = null;
         int propertyType = -1;

         while (reader.hasNext())
         {
            int eventCode = reader.next();

            switch (eventCode)
            {

               case StartElement.START_ELEMENT : {

                  String lname = reader.getLocalName();
                  String prefix = reader.getPrefix();
                  if (Constants.NS_SV_PREFIX.equals(prefix))
                  {
                     // read prefixes URIes from source SV XML
                     if (svURI == null)
                     {
                        svURI = reader.getNamespaceURI(Constants.NS_SV_PREFIX);
                        exoURI = reader.getNamespaceURI(Constants.NS_EXO_PREFIX);
                     }

                     if (Constants.SV_NODE.equals(lname))
                     {
                        String svName = reader.getAttributeValue(svURI, Constants.SV_NAME);
                        String exoId = reader.getAttributeValue(exoURI, Constants.EXO_ID);
                        if (svName != null && exoId != null)
                        {
                           // create subnode
                           QPath currentPath;
                           String parentId;
                           int orderNumber;
                           if (parents.size() > 0)
                           {
                              // path to a new node
                              SVNodeData parent = parents.peek();

                              InternalQName name = locationFactory.parseJCRName(svName).getInternalName();

                              int[] chi = parent.addChildNode(name);
                              orderNumber = chi[0];
                              int index = chi[1];
                              currentPath = QPath.makeChildPath(parent.getQPath(), name, index);

                              parentId = parent.getIdentifier();
                           }
                           else
                           {
                              // root
                              currentPath = Constants.ROOT_PATH;
                              parentId = null;
                              orderNumber = 0;

                              // register namespaces from jcr:root node
                              for (int i = 0; i < reader.getNamespaceCount(); i++)
                              {
                                 String nsp = reader.getNamespacePrefix(i);
                                 try
                                 {
                                    namespaceRegistry.getURI(nsp);
                                 }
                                 catch (NamespaceException e)
                                 {
                                    namespaceRegistry.registerNamespace(nsp, reader.getNamespaceURI(i));
                                 }
                              }
                           }

                           SVNodeData currentNode = new SVNodeData(currentPath, exoId, parentId, 0, orderNumber);

                           AccessControlList acl =
                              ACLInitializationHelper.initAcl(parents.size() == 0 ? null : parents.peek().getACL(),
                                 null, null);
                           currentNode.setACL(acl);

                           // push current node as parent
                           parents.push(currentNode);

                           // add current node to changes log.
                           // add node, no event fire, persisted, internally created, root is ancestor to save
                           changes.add(new ItemState(currentNode, ItemState.ADDED, false, Constants.ROOT_PATH, true,
                              true));
                        }
                        else
                           LOG.warn("Node skipped name=" + svName + " id=" + exoId + ". Context node "
                              + (parents.size() > 0 ? parents.peek().getQPath().getAsString() : "/"));

                     }
                     else if (Constants.SV_PROPERTY.equals(lname))
                     {
                        String svName = reader.getAttributeValue(svURI, Constants.SV_NAME);
                        String exoId = reader.getAttributeValue(exoURI, Constants.EXO_ID);
                        String svType = reader.getAttributeValue(svURI, Constants.SV_TYPE);
                        if (svName != null && svType != null && exoId != null)
                        {
                           if (parents.size() > 0)
                           {
                              SVNodeData parent = parents.peek();
                              QPath currentPath =
                                 QPath.makeChildPath(parent.getQPath(), locationFactory.parseJCRName(svName)
                                    .getInternalName());
                              try
                              {
                                 propertyType = PropertyType.valueFromName(svType);
                              }
                              catch (IllegalArgumentException e)
                              {
                                 propertyType = ExtendedPropertyType.valueFromName(svType);
                              }

                              // exo:multivalued optional, assigned for multivalued properties only
                              String exoMultivalued = reader.getAttributeValue(exoURI, Constants.EXO_MULTIVALUED);

                              currentProperty =
                                 new SVPropertyData(currentPath, exoId, 0, propertyType, parent.getIdentifier(),
                                    ("true".equals(exoMultivalued) ? true : false));
                           }
                           else
                              LOG.warn("Property can'b be first name=" + svName + " type=" + svType + " id=" + exoId
                                 + ". Node should be prior. Context node "
                                 + (parents.size() > 0 ? parents.peek().getQPath().getAsString() : "/"));
                        }
                        else
                           LOG.warn("Property skipped name=" + svName + " type=" + svType + " id=" + exoId
                              + ". Context node "
                              + (parents.size() > 0 ? parents.peek().getQPath().getAsString() : "/"));

                     }
                     else if (Constants.SV_VALUE.equals(lname) && propertyType != -1)
                     {
                        if (propertyType == PropertyType.BINARY)
                           propertyValue = new BinaryValueWriter();
                        else
                           propertyValue = new StringValueWriter();
                     }
                  }
                  break;
               }

               case StartElement.CHARACTERS : {
                  if (propertyValue != null)
                  {
                     // read property value text
                     propertyValue.write(reader.getText());
                  }

                  break;
               }

               case StartElement.END_ELEMENT : {
                  String lname = reader.getLocalName();
                  String prefix = reader.getPrefix();
                  if (Constants.NS_SV_PREFIX.equals(prefix))
                  {
                     if (Constants.SV_NODE.equals(lname))
                     {
                        // change current context
                        // - pop parent from the stack
                        SVNodeData parent = parents.pop();
                        if (parent.getMixinTypeNames() == null)
                        {
                           // mixins cannot be null
                           parent.setMixinTypeNames(new InternalQName[0]);
                        }
                     }
                     else if (Constants.SV_PROPERTY.equals(lname))
                     {
                        // apply property to the current node and changes log
                        if (currentProperty != null)
                        {
                           SVNodeData parent = parents.peek();

                           // check NodeData specific properties
                           if (currentProperty.getQPath().getName().equals(Constants.JCR_PRIMARYTYPE))
                           {
                              parent.setPrimartTypeName(InternalQName.parse(ValueDataUtil.getString(currentProperty
                                 .getValues().get(0))));
                           }
                           else if (currentProperty.getQPath().getName().equals(Constants.JCR_MIXINTYPES))
                           {
                              InternalQName[] mixins = new InternalQName[currentProperty.getValues().size()];
                              for (int i = 0; i < currentProperty.getValues().size(); i++)
                              {
                                 mixins[i] =
                                    InternalQName.parse(ValueDataUtil.getString(currentProperty.getValues().get(i)));
                              }
                              parent.setMixinTypeNames(mixins);
                           }
                           else if (currentProperty.getQPath().getName().equals(Constants.EXO_OWNER))
                           {
                              String exoOwner = ValueDataUtil.getString(currentProperty.getValues().get(0));
                              parent.setExoOwner(exoOwner);

                              SVNodeData curParent = parents.pop();

                              AccessControlList acl =
                                 ACLInitializationHelper.initAcl(parents.size() == 0 ? null : parents.peek().getACL(),
                                    exoOwner, curParent.getExoPrivileges());
                              curParent.setACL(acl);

                              parents.push(curParent);
                           }
                           else if (currentProperty.getQPath().getName().equals(Constants.EXO_PERMISSIONS))
                           {
                              List<String> exoPrivileges = new ArrayList<String>();
                              for (int i = 0; i < currentProperty.getValues().size(); i++)
                              {
                                 exoPrivileges.add(ValueDataUtil.getString(currentProperty.getValues().get(i)));
                              }
                              parent.setExoPrivileges(exoPrivileges);

                              SVNodeData curParent = parents.pop();

                              AccessControlList acl =
                                 ACLInitializationHelper.initAcl(parents.size() == 0 ? null : parents.peek().getACL(),
                                    curParent.getExoOwner(), exoPrivileges);
                              curParent.setACL(acl);

                              parents.push(curParent);
                           }

                           // add property, no event fire, persisted, internally created, root is ancestor to
                           // save
                           changes.add(new ItemState(currentProperty, ItemState.ADDED, false, Constants.ROOT_PATH,
                              true, true));

                           // reset property context
                           propertyType = -1;
                           currentProperty = null;
                        }

                     }
                     else if (Constants.SV_VALUE.equals(lname))
                     {
                        // apply property value to the current property
                        propertyValue.close();
                        TransientValueData vdata;
                        if (propertyType == PropertyType.NAME)
                        {
                           vdata =
                              new TransientValueData(currentProperty.getValues().size(), locationFactory.parseJCRName(
                                 propertyValue.getText()).getInternalName());
                        }
                        else if (propertyType == PropertyType.PATH)
                        {
                           vdata =
                              new TransientValueData(currentProperty.getValues().size(), locationFactory.parseJCRPath(
                                 propertyValue.getText()).getInternalPath());
                        }
                        else if (propertyType == PropertyType.DATE)
                        {
                           vdata =
                              new TransientValueData(currentProperty.getValues().size(), JCRDateFormat
                                 .parse(propertyValue.getText()));
                        }
                        else if (propertyType == PropertyType.BINARY)
                        {
                           if (propertyValue.isText())
                           {
                              vdata =
                                 new TransientValueData(currentProperty.getValues().size(), propertyValue.getText());
                           }
                           else
                           {

                              File pfile = propertyValue.getFile();
                              if (pfile != null)
                              {
                                 vdata =
                                    new TransientValueData(currentProperty.getValues().size(), null, new SpoolFile(
                                       PrivilegedFileHelper.getAbsolutePath(pfile)), spoolConfig);
                              }
                              else
                              {
                                 vdata = new TransientValueData(currentProperty.getValues().size(), new byte[]{});
                              }
                           }
                        }
                        else
                        {
                           // other like String
                           vdata = new TransientValueData(currentProperty.getValues().size(), propertyValue.getText());
                        }

                        currentProperty.getValues().add(vdata);

                        // reset value context
                        propertyValue = null;
                     }
                  }
                  break;
               }
            }
         }

         return changes;
      }
      finally
      {
         input.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWorkspaceInitialized() throws RepositoryException
   {
      try
      {
         // If someone invoke isWorkspaceInitialized() during restore action then NullNodeData for root node will be pushed
         // into the cache and will be there even restore is finished and data will be placed into DB.
         return !isRestoreInProgress && dataManager.getItemData(Constants.ROOT_UUID) != null;
      }
      catch (RepositoryException e)
      {
         throw new RepositoryException("Cannot check if the workspace '" + workspaceName
            + "' has already been initialized", e);
      }
   }
}
