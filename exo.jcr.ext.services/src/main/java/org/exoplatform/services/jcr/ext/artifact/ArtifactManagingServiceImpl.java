/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.artifact;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RegistryEntry;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS .<br>
 * Service responsible for Administration Maven repository the served JCR structure inside
 * workspaceName is: rootPath (maven-root)/ ---part-of-group-folder1/ (nt:folder + exo:groupId)
 * ---part-of-group-foldern/ ------artifact-root-folder/(nt:folder + exo:artifactId)
 * ---------maven-metadata.xml(nt:file) ---------maven-metadata.xml.sha1(nt:file)
 * ---------artifact-version-folder/(nt:folder + exo:versionId) ------------artifactId-version.jar
 * (nt:file + exo:mavenjar / nt:resource) ------------artifactId-version.jar.sha1 (nt:file +
 * exo:mavensha1 / nt:resource ) ------------artifactId-version.pom (nt:file + exo:mavenpom /
 * nt:resource) ------------artifactId-version.pom.sha1 (nt:file + exo:mavensha1/ (nt:resource)
 * ------------maven-metadata.xml (nt:file +exo:mavenmetadata / (nt:resource )
 * ------------maven-metadata.xml.sha1(nt:file + exo:mavensha1 / (nt:resource)
 * 
 * @author Gennady Azarenkov
 * @author Volodymyr Krasnikov
 * @version $Id: $
 */
public class ArtifactManagingServiceImpl
   implements ArtifactManagingService, Startable
{
   private static final int BUFFER = 4096;

   private static final String STRING_TERMINATOR = "*";

   private static final String NT_FILE = "artifact-nodetypes.xml";

   private static final String SERVICE_NAME = "ArtifactManaging";

   private RepositoryService repositoryService;

   private RegistryService registryService;

   private SessionProvider sessionProvider;

   private InitParams initParams;

   private String repoWorkspaceName;

   private String rootNodePath;

   private static final Log LOG = ExoLogger.getLogger(ArtifactManagingServiceImpl.class);

   private Map<String, String> mimeMap = new Hashtable<String, String>();

   private List<String> listErrorPom = new ArrayList<String>();

   /**
    * @param initParams
    * @param repositoryService
    * @param registryService
    * @throws RepositoryConfigurationException
    */
   public ArtifactManagingServiceImpl(InitParams initParams, RepositoryService repositoryService,
            RegistryService registryService) throws RepositoryConfigurationException
   {
      this.repositoryService = repositoryService;
      this.registryService = registryService;

      if (initParams == null)
      {
         throw new RepositoryConfigurationException("Init parameters expected !!!");
      }

      this.initParams = initParams;

      setDefaultMimes();
   }

   /**
    * without registry service
    * 
    * @param params
    * @param repositoryService
    * @throws RepositoryConfigurationException
    */
   public ArtifactManagingServiceImpl(InitParams params, RepositoryService repositoryService)
            throws RepositoryConfigurationException
   {
      this(params, repositoryService, null);
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#addArtifact
    * (org.exoplatform.services.jcr.ext.common.SessionProvider,
    * org.exoplatform.services.jcr.ext.maven.ArtifactDescriptor, java.io.InputStream)
    */
   public void addArtifact(SessionProvider sp, ArtifactDescriptor artifact, InputStream jarIStream,
            InputStream pomIStream) throws RepositoryException
   {

      Session session = currentSession(sp);
      Node rootNode = (Node) session.getItem(rootNodePath);

      Node groupId_tail = createGroupIdLayout(rootNode, artifact);

      Node artifactId_node = createArtifactIdLayout(groupId_tail, artifact);

      Node version_node = createVersionLayout(artifactId_node, artifact);

      if (version_node != null)
      { // returns if the same node sibling appears
         importResource(version_node, jarIStream, "jar", artifact);
         importResource(version_node, pomIStream, "pom", artifact);
      }

      session.save();

   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#exportArtifacts
    * (org.exoplatform.services.jcr.ext.common.SessionProvider,
    * org.exoplatform.services.jcr.ext.maven.FolderDescriptor, java.io.OutputStream)
    */
   public void exportArtifacts(SessionProvider sp, FolderDescriptor parentFolder, OutputStream out)
            throws RepositoryException, FileNotFoundException
   {

      Session session = currentSession(sp);
      Node parentNode;
      if (rootNodePath.length() > 1)
      {
         parentNode = (Node) session.getItem(rootNodePath + "/" + parentFolder.getAsPath());
      }
      else
      {
         parentNode = (Node) session.getItem("/" + parentFolder.getAsPath());
      }

      LOG.info("Write repository to zipped stream");

      ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(out, BUFFER));
      try
      {
         mapRepositoryToZipStream(parentNode, zout);
         zout.flush();
         zout.close();
      }
      catch (IOException e)
      {
         LOG.error("Cannot write to zip stream", e);
      }

   }

   public void exportArtifacts(SessionProvider sp, FolderDescriptor parentFolder, File destFolder)
            throws RepositoryException, FileNotFoundException
   {

      Session session = currentSession(sp);
      Node parentNode;
      if (rootNodePath.length() > 1)
      {
         parentNode = (Node) session.getItem(rootNodePath + "/" + parentFolder.getAsPath());
      }
      else
      {
         parentNode = (Node) session.getItem("/" + parentFolder.getAsPath());
      }
      mapRepositoryToFilefs(parentNode, destFolder);
   }

   private void mapRepositoryToFilefs(Node parentNode, File parentFolder) throws RepositoryException
   {
      NodeIterator folderIterator = parentNode.getNodes();
      while (folderIterator.hasNext())
      {
         Node folder = folderIterator.nextNode();
         if (folder.isNodeType("exo:artifact"))
         {
            File levelfolder = new File(parentFolder.getAbsoluteFile() + File.separator + folder.getName());
            levelfolder.mkdir();
            mapRepositoryToFilefs(folder, levelfolder); // recursive call
         }
         else if (folder.isNodeType("exo:file"))
         {
            File content = new File(parentFolder + File.separator + folder.getName());

            FileOutputStream fout = null;
            try
            {
               fout = new FileOutputStream(content);
               Node dataNode = folder.getNode("jcr:content");

               Property data = dataNode.getProperty("jcr:data");
               Property lastModified = dataNode.getProperty("jcr:lastModified");
               content.setLastModified(lastModified.getLong());
               IOUtils.copy(data.getStream(), fout);

            }
            catch (FileNotFoundException e)
            {
               LOG.error("!!Can't create content fo file", e);
            }
            catch (IOException e)
            {
               LOG.error("!!Can't write content fo file", e);
            }
            finally
            {
               IOUtils.closeQuietly(fout);
            }
         }
      }
   }

   private void mapRepositoryToZipStream(Node parentNode, ZipOutputStream zout) throws RepositoryException, IOException
   {

      NodeIterator folderIterator = parentNode.getNodes();
      while (folderIterator.hasNext())
      {
         Node folder = folderIterator.nextNode();
         if (folder.isNodeType("exo:artifact"))
         {
            String entryName = parentNode.getPath() + File.separator + folder.getName();
            ZipEntry entry = new ZipEntry(entryName + "/");

            zout.putNextEntry(entry);

            mapRepositoryToZipStream(folder, zout);

         }
         else if (folder.isNodeType("exo:file"))
         {

            String entryName = parentNode.getPath() + File.separator + folder.getName();
            ZipEntry entry = new ZipEntry(entryName);

            if (LOG.isDebugEnabled())
            {
               LOG.debug("Zipping " + entryName);
            }

            Node dataNode = folder.getNode("jcr:content");
            Property data = dataNode.getProperty("jcr:data");
            InputStream in = data.getStream();

            Property lastModified = dataNode.getProperty("jcr:lastModified");

            entry.setTime(lastModified.getLong());
            entry.setSize(in.available());
            zout.putNextEntry(entry);

            int count;
            byte[] buf = new byte[BUFFER];
            while ((count = in.read(buf, 0, BUFFER)) != -1)
            {
               zout.write(buf, 0, count);
            }
            zout.flush();
            in.close();
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#getDescriptors
    * (org.exoplatform.services.jcr.ext.common.SessionProvider,
    * org.exoplatform.services.jcr.ext.maven.FolderDescriptor)
    */
   public List<Descriptor> getDescriptors(SessionProvider sp, FolderDescriptor parentFolder) throws RepositoryException
   {
      LOG.info("Get child nodes to : " + parentFolder.getAsString());

      Session session = currentSession(sp);

      session.refresh(true);

      Node rootNode = (Node) session.getItem(rootNodePath);
      String strPath = parentFolder.getAsString();

      Node targetNode;
      if (strPath.equals("/"))
      {
         targetNode = rootNode;
      }
      else
      {
         targetNode = rootNode.getNode(strPath.substring(1));
      }
      LOG.info(targetNode.getPath());
      NodeType[] a = targetNode.getMixinNodeTypes();
      StringBuilder mixins = new StringBuilder();
      for (NodeType type : a)
      {
         mixins.append(type.getName()).append(" ");
      }
      LOG.info("**** Mixins : " + mixins.toString());

      LOG.info("**** Workspace : " + session.getWorkspace().getName());

      List<Descriptor> childNodes = new ArrayList<Descriptor>();
      for (NodeIterator iterator = targetNode.getNodes(); iterator.hasNext();)
      {
         // descriptor holds names of all child nodes than makes up artifact
         // coordinates.
         Node node = iterator.nextNode();

         if (node.isNodeType("nt:folder") || node.isNodeType("nt:file"))
         {
            Descriptor descriptor = new FolderDescriptor(node.getName());
            childNodes.add(descriptor);
         }
      }

      return (childNodes.size() == 0) ? null : childNodes;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#importArtifacts
    * (org.exoplatform.services.jcr.ext.common.SessionProvider, java.io.InputStream)
    */
   public void importArtifacts(SessionProvider sp, InputStream in) throws RepositoryException, FileNotFoundException
   {
      LOG.info("Extract repository to temporary folder");
      String path = System.getProperty("java.io.tmpdir") + File.separator + "maven2";
      File temporaryFolder = new File(getUniqueFilename(path));
      if (!temporaryFolder.mkdir())
      {
         throw new FileNotFoundException("Cannot create temporary folder");
      }
      ZipEntry entry;
      ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
      try
      {
         while ((entry = zipIn.getNextEntry()) != null)
         {

            if (!(entry.isDirectory() && new File(temporaryFolder + File.separator + entry.getName()).mkdir()))
            {
               int count;
               byte data[] = new byte[BUFFER];
               File file = new File(temporaryFolder + File.separator + entry.getName());

               FileUtils.touch(file);

               FileOutputStream fos = new FileOutputStream(file);
               BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
               while ((count = zipIn.read(data, 0, BUFFER)) != -1)
               {
                  dest.write(data, 0, count);
               }
               dest.flush();
               dest.close();
            }

         }
      }
      catch (IOException e)
      {
         LOG.error("Cannot get zip entry from stream", e);
      }
      finally
      {
         IOUtils.closeQuietly(zipIn);
         IOUtils.closeQuietly(in);
      }

      // main part - copy to JCR from temp folder
      // use uploading artifacts from local folder.

      importArtifacts(sp, temporaryFolder);
      try
      {
         FileUtils.deleteDirectory(temporaryFolder);
      }
      catch (IOException e)
      {
         LOG.error("Cannot remove temporary folder", e);
      }

   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#importArtifacts
    * (org.exoplatform.services.jcr.ext.common.SessionProvider, java.io.File)
    */
   public void importArtifacts(SessionProvider sp, File folder) throws RepositoryException, FileNotFoundException
   {
      if (!folder.exists())
         throw new FileNotFoundException("Source folder expected");
      try
      {
         this.listErrorPom.clear();
         importFilesToJCR(sp, folder);
      }
      catch (Exception e)
      {
         LOG.error("Exception during uploading local folder to JCR", e);
      }
   }

   /*
    * This method provides adding to JCR artifacts. this means that jar-files and appropriate pom
    * files would be added. Main logic: scan all files & if it is a pair jar/pom -add it.
    */
   private void importFilesToJCR(SessionProvider sp, File folder) throws Exception
   {

      for (File file : folder.listFiles(new DefaultFileFilter()))
      {

         if (file.isDirectory())
         {
            importFilesToJCR(sp, file);
         }

         String ext = FilenameUtils.getExtension(file.getAbsolutePath());
         if (ext.equals("pom"))
         {
            String basename = FilenameUtils.removeExtension(file.getAbsolutePath());
            File jarfile = new File(basename.concat(".jar"));

            if (jarfile.exists())
            {
               // get descripting from pom file
               try
               {
                  ArtifactDescriptor artifact = ArtifactDescriptor.createFromPomfile(file);
                  InputStream jarIStream = new FileInputStream(jarfile);
                  InputStream pomIStream = new FileInputStream(file);

                  addArtifact(sp, artifact, jarIStream, pomIStream);
               }
               catch (org.xml.sax.SAXParseException e)
               {
                  // throw new ArtifactDescriptorException(FilenameUtils.getName(file.getAbsolutePath()));
                  this.listErrorPom.add(FilenameUtils.getName(file.getAbsolutePath()));
                  continue;
               }
            }
         }
      }
   }

   /*
    * According JCR structure, version Node holds all actual data: jar, pom and ckecksums Removing
    * that node is removing all content and artifact indeed!
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#removeArtifact
    * (org.exoplatform.services.jcr.ext.common.SessionProvider,
    * org.exoplatform.services.jcr.ext.maven.ArtifactDescriptor)
    */
   public void removeArtifact(SessionProvider sp, Descriptor artifact) throws RepositoryException
   {
      Session session = currentSession(sp);
      Node root = (Node) session.getItem(rootNodePath);

      String pathToRemove = "";

      if (rootNodePath.length() > 1)
      {
         if (rootNodePath.endsWith("/"))
            pathToRemove = rootNodePath + artifact.getAsPath();
         else
            pathToRemove = rootNodePath + "/" + artifact.getAsPath();
      }
      else
      {
         pathToRemove = "/" + artifact.getAsPath();
      }

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Remove node: " + pathToRemove);
      }

      Node rmNode = (Node) session.getItem(pathToRemove);

      // while (rmNode != root) {
      // Node parent = rmNode.getParent();
      rmNode.remove();
      // if (!parent.hasNodes())
      // rmNode = parent;
      // else
      // break;
      // }
      session.save();
   }

   public List getPermission(SessionProvider sp, Descriptor artifact) throws RepositoryException
   {

      Session session = currentSession(sp);
      String pathToCheck = "";

      if (rootNodePath.length() > 1)
      { // artifact root is some real node
         if (rootNodePath.endsWith("/"))
            pathToCheck = rootNodePath + artifact.getAsPath();
         else
            pathToCheck = rootNodePath + "/" + artifact.getAsPath();
      }
      else
      {
         pathToCheck = "/" + artifact.getAsPath(); // "/" - is root path
      }

      LOG.debug("Checking Permission on node: " + pathToCheck);
      ExtendedNode rmNode = (ExtendedNode) session.getItem(pathToCheck);
      List<AccessControlEntry> list = rmNode.getACL().getPermissionEntries();
      return list;
   }

   public void changePermission(SessionProvider sp, Descriptor artifact, String identity, String[] permissions,
            boolean delete) throws RepositoryException
   {

      Session session = currentSession(sp);
      String pathToChange = "";

      if (rootNodePath.length() > 1)
      { // artifact root is some real node
         if (rootNodePath.endsWith("/"))
            pathToChange = rootNodePath + artifact.getAsPath();
         else
            pathToChange = rootNodePath + "/" + artifact.getAsPath();
      }
      else
      {
         pathToChange = "/" + artifact.getAsPath(); // "/" - is root path
      }

      LOG.debug("Changing Permission on node: " + pathToChange);

      ExtendedNode chNode = (ExtendedNode) session.getItem(pathToChange);
      if (!chNode.isNodeType("exo:privilegeable"))
      {
         if (chNode.canAddMixin("exo:privilegeable"))
            chNode.addMixin("exo:privilegeable");
         else
            throw new RepositoryException("Can't add mixin");
      }
      // log.info("PERMS-SIZE:" + permissions.length);
      try
      {
         if (!delete)
            chNode.setPermission(identity, permissions);
         else
         {
            if (permissions.length > 0)
            {
               for (int i = 0; i < permissions.length; i++)
                  chNode.removePermission(identity, permissions[i]);
            }
            else
            {
               chNode.removePermission(identity);
            }
         }
      }
      catch (RepositoryException e)
      {
         LOG.error("Cannot change permissions", e);
      }

      session.save();
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.maven.ArtifactManagingService#searchArtifacts
    * (org.exoplatform.services.jcr.ext.common.SessionProvider,
    * org.exoplatform.services.jcr.ext.maven.SearchCriteria)
    */
   public List<Descriptor> searchArtifacts(SessionProvider sp, SearchCriteria criteria) throws RepositoryException
   {
      Session session = currentSession(sp);

      session.refresh(true);
      session.save();

      String param = criteria.getContainsExpr();
      String pathConstraint = "";
      if (rootNodePath.length() > 1)
      { // artifact root is some real node
         if (rootNodePath.endsWith("/"))
            pathConstraint = rootNodePath + "%/" + param + "[%]";
         else
            pathConstraint = rootNodePath + "/%/" + param + "[%]";
      }
      else
      {
         pathConstraint = "/%/" + param + "[%]"; // artifact root is workspace root
      }
      // node !!

      String sqlQuery = String.format("SELECT * FROM nt:folder WHERE jcr:path LIKE '%s' ", pathConstraint);

      LOG.info(sqlQuery);
      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);

      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();

      List<Descriptor> preciseNode = new ArrayList<Descriptor>();
      while (iterator.hasNext())
      {

         Node candidate = iterator.nextNode();

         Descriptor descriptor = new FolderDescriptor(candidate.getPath());
         preciseNode.add(descriptor);

      }

      return preciseNode;
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      // responsible for:
      // 1. reading parameters (such as repoWorkspaceName, repoPath) from
      // registryService (if present) or init params
      // 2. initializing artifact service Registry Entry if registryService is
      // present
      // if Entry is not initialized yet (first launch)
      // 3. initializing maven root if not initialized

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Starting ArtifactManagingService ...");
      }

      sessionProvider = SessionProvider.createSystemProvider();

      try
      {
         InputStream xml = getClass().getResourceAsStream(NT_FILE);
         ManageableRepository rep = repositoryService.getCurrentRepository();
         rep.getNodeTypeManager().registerNodeTypes(xml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS,
            NodeTypeDataManager.TEXT_XML);
         readParamsFromRegistryService(sessionProvider);
         prepareRootNode(sessionProvider, rootNodePath);
      }
      catch (PathNotFoundException e)
      {
         try
         {
            readParamsFromFile();
            writeParamsToRegistryService(sessionProvider);

            prepareRootNode(sessionProvider, rootNodePath);
         }
         catch (RepositoryException exc)
         {
            LOG.error("Cannot write init configuration to RegistryService", exc);
         }
         catch (IOException exc)
         {
            LOG.error("Cannot write init configuration to RegistryService", exc);
         }
         catch (SAXException exc)
         {
            LOG.error("Cannot write init configuration to RegistryService", exc);
         }
         catch (ParserConfigurationException exc)
         {
            LOG.error("Cannot write init configuration to RegistryService", exc);
         }
      }
      catch (RepositoryException e)
      {
         LOG.error("Error while register nodetypes/checking existance", e);
      }
      finally
      {
         sessionProvider.close();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
   }

   private Session currentSession(SessionProvider sp) throws RepositoryException
   {
      return sp.getSession(repoWorkspaceName, repositoryService.getCurrentRepository());
   }

   private void prepareRootNode(SessionProvider sp, String path) throws RepositoryException
   {
      Session session = currentSession(sp);
      Node current_root = session.getRootNode();
      for (String folder : path.split("/"))
      {
         if (!current_root.hasNode(folder))
         {
            current_root.addNode(folder);
         }
         current_root = current_root.getNode(folder);
      }
      session.save();
      if (!session.getItem(path).isNode())
         throw new RepositoryException("Maven root node is not been initialized");
   }

   // this function creates hierarchy in JCR storage acording to groupID
   // parameter : com.google.code...
   private Node createGroupIdLayout(Node rootNode, ArtifactDescriptor artifact) throws RepositoryException
   {
      FolderDescriptor groupId = artifact.getGroupId();
      List<String> struct_groupId = new ArrayList<String>();

      String[] items = groupId.getAsPath().split("/");

      for (String subString : items)
      {
         struct_groupId.add(subString);
      }
      Node groupIdTail = rootNode;

      for (Iterator<String> iterator = struct_groupId.iterator(); iterator.hasNext();)
      {
         String name = iterator.next();
         Node levelNode;
         // Node do not has such child nodes
         if (!groupIdTail.hasNode(name))
         {
            levelNode = groupIdTail.addNode(name, "nt:folder");
            levelNode.addMixin("exo:groupId");
         }
         else
         {
            levelNode = groupIdTail.getNode(name);
         }
         groupIdTail = levelNode;
      }

      return groupIdTail;
   }

   private Node createArtifactIdLayout(Node groupId_NodeTail, ArtifactDescriptor artifact) throws RepositoryException
   {
      String artifactId = artifact.getArtifactId();
      Node artifactIdNode;
      if (!groupId_NodeTail.hasNode(artifactId))
      {
         artifactIdNode = groupId_NodeTail.addNode(artifactId, "nt:folder");
         artifactIdNode.addMixin("exo:artifactId");
         artifactIdNode.setProperty("exo:versionList", new String[]
         {ArtifactManagingServiceImpl.STRING_TERMINATOR, ArtifactManagingServiceImpl.STRING_TERMINATOR});
      }
      else
      {
         artifactIdNode = groupId_NodeTail.getNode(artifactId);
      }
      return artifactIdNode;
   }

   private Node createVersionLayout(Node artifactId, ArtifactDescriptor artifact) throws RepositoryException
   {
      String version = artifact.getVersionId();

      if (artifactId.hasNode(version))
         return null;

      Node currentVersion = artifactId.addNode(version, "nt:folder");
      currentVersion.addMixin("exo:versionId");

      return currentVersion;
   }

   // this method used for writing to repo jars, poms and their checksums
   private void importResource(Node parentNode, InputStream file_in, String resourceType, ArtifactDescriptor artifact)
            throws RepositoryException
   {
      // Note that artifactBean been initialized within constructor
      // resourceType can be jar, pom, metadata

      String filename;
      if (resourceType.equals("metadata"))
      {
         filename = "maven-metadata.xml";
      }
      else
      {
         filename = String.format("%s-%s.%s", artifact.getArtifactId(), artifact.getVersionId(), resourceType);
      }

      OutputStream fout = null;
      File tmp_file = null;
      try
      {
         String tmpFilename = getUniqueFilename(filename);
         tmp_file = File.createTempFile(tmpFilename, null);
         fout = new FileOutputStream(tmp_file);
         IOUtils.copy(file_in, fout);
         fout.flush();
      }
      catch (FileNotFoundException e)
      {
         LOG.error("Cannot create .tmp file for storing artifact", e);
      }
      catch (IOException e)
      {
         LOG.error("IO exception on .tmp file for storing artifact", e);
      }
      finally
      {
         IOUtils.closeQuietly(file_in);
         IOUtils.closeQuietly(fout);
      }

      writePrimaryContent(parentNode, filename, resourceType, tmp_file);
      writeChecksum(parentNode, filename, tmp_file, "SHA1");

      try
      {
         // and collect all garbage : temporary files
         FileUtils.forceDelete(tmp_file);
      }
      catch (IOException e)
      {
         LOG.error("Cannot delete tmp file", e);
      }

   }

   private void writePrimaryContent(Node parentNode, String filename, String resourceType, File srcFile)
            throws RepositoryException
   {

      String mimeType = getRelativeMimeType(resourceType);
      Node nodeResourceFile = parentNode.addNode(filename, "nt:file");

      String mixinType = "exo:maven".concat(resourceType);
      if (nodeResourceFile.canAddMixin(mixinType))
         nodeResourceFile.addMixin(mixinType);
      try
      {
         InputStream file_is = new FileInputStream(srcFile);

         Node content = nodeResourceFile.addNode("jcr:content", "nt:resource");
         content.setProperty("jcr:mimeType", mimeType);
         content.setProperty("jcr:lastModified", Calendar.getInstance());
         content.setProperty("jcr:data", file_is);

         IOUtils.closeQuietly(file_is);

      }
      catch (FileNotFoundException e)
      {
         LOG.error("Cannot read from .tmp resource file", e);
      }
   }

   private void writeChecksum(Node parentNode, String filename, File srcFile, String algorithm)
            throws RepositoryException
   {
      Node nodeChecksumFile = parentNode.addNode(filename.concat("." + algorithm.toLowerCase()), "nt:file");

      String mixinType = "exo:maven".concat(algorithm.toLowerCase());
      if (nodeChecksumFile.canAddMixin(mixinType))
      {
         nodeChecksumFile.addMixin(mixinType);
      }
      try
      {

         FileInputStream fileInputStream = new FileInputStream(srcFile);
         String checksum = CRCGenerator.getChecksum(fileInputStream, algorithm);

         InputStream checksum_is = new ByteArrayInputStream(checksum.getBytes());
         String mimeType = "text/plain";

         Node content = nodeChecksumFile.addNode("jcr:content", "nt:resource");
         content.setProperty("jcr:mimeType", mimeType);
         content.setProperty("jcr:lastModified", Calendar.getInstance());
         content.setProperty("jcr:data", checksum_is);

         IOUtils.closeQuietly(checksum_is);
      }
      catch (FileNotFoundException e)
      {
         LOG.error("Cannot read from .tmp resource file", e);
      }
      catch (IOException e)
      {
         LOG.error("Cannot read from .tmp resource file", e);
      }
      catch (NoSuchAlgorithmException e)
      {
         LOG.error("No such algorithm for generating checksums", e);
      }
   }

   protected File createSingleMetadata(String groupId, String artifactId, String version) throws FileNotFoundException
   {
      File temp = null;
      try
      {
         String filename = getUniqueFilename("maven-metadata.xml");
         temp = File.createTempFile(filename, null);

         OutputStream os = new FileOutputStream(temp);
         XMLOutputFactory factory = XMLOutputFactory.newInstance();
         XMLStreamWriter writer = factory.createXMLStreamWriter(os);
         try
         {
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("metadata");

            writer.writeStartElement("groupId");
            writer.writeCharacters(groupId);
            writer.writeEndElement();

            writer.writeStartElement("artifactId");
            writer.writeCharacters(artifactId);
            writer.writeEndElement();

            writer.writeStartElement("version");
            writer.writeCharacters(version);
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndDocument();
         }
         finally
         {
            writer.flush();
            writer.close();
            os.close();
         }
      }
      catch (XMLStreamException e)
      {
         LOG.error("Error on creating metadata - XML", e);
      }
      catch (IOException e)
      {
         LOG.error("Error on creating metadata - FILE", e);
      }
      return (temp != null && temp.exists()) ? temp : null;
   }

   protected File createMultiMetadata(String groupId, String artifactId, String current_version, List<String> v_list)
            throws FileNotFoundException
   {
      File temp = null;
      try
      {
         String filename = getUniqueFilename("maven-metadata.xml");
         temp = File.createTempFile(filename, null);

         OutputStream os = new FileOutputStream(temp);
         XMLOutputFactory factory = XMLOutputFactory.newInstance();
         XMLStreamWriter writer = factory.createXMLStreamWriter(os);
         try
         {
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("metadata");

            writer.writeStartElement("groupId");
            writer.writeCharacters(groupId);
            writer.writeEndElement();

            writer.writeStartElement("artifactId");
            writer.writeCharacters(artifactId);
            writer.writeEndElement();

            String elderVersion;
            if (v_list.size() > 0)
            {
               Collections.sort(v_list); // sort list
               elderVersion = v_list.get(0); // get first element
            }
            else
               elderVersion = current_version;
            v_list.add(current_version);

            writer.writeStartElement("version");
            writer.writeCharacters(elderVersion);
            writer.writeEndElement();

            writer.writeStartElement("versions");
            writer.writeStartElement("versioning");

            for (Iterator<String> iterator = v_list.iterator(); iterator.hasNext();)
            {
               writer.writeStartElement("version");
               writer.writeCharacters(iterator.next());
               writer.writeEndElement();
            }

            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndDocument();
         }
         finally
         {
            writer.flush();
            writer.close();
            os.close();
         }
      }
      catch (XMLStreamException e)
      {
         LOG.error("Error on creating metadata - XML", e);
      }
      catch (IOException e)
      {
         LOG.error("Error on creating metadata - FILE", e);
      }
      return (temp != null && temp.exists()) ? temp : null;
   }

   private String getRelativeMimeType(String key)
   {
      return (String) mimeMap.get(key);
   }

   private void setDefaultMimes()
   {
      mimeMap.put("jar", "application/java-archive");
      mimeMap.put("pom", "text/xml");
      // mimeMap.put("metadata", "text/xml");
   }

   private String getUniqueFilename(String basename)
   {
      String suffix = ((Double) Math.random()).toString().substring(2, 7);
      return basename + "." + suffix;
   }

   /**
    * Read parameters from RegistryService.
    * 
    * @param sessionProvider
    *          The SessionProvider
    * @throws RepositoryException
    * @throws PathNotFoundException
    */
   private void readParamsFromRegistryService(SessionProvider sessionProvider) throws PathNotFoundException,
            RepositoryException
   {
      String entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + "workspace";
      RegistryEntry registryEntry = registryService.getEntry(sessionProvider, entryPath);
      Document doc = registryEntry.getDocument();
      Element element = doc.getDocumentElement();
      repoWorkspaceName = getAttributeSmart(element, "value");

      entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + "rootNode";
      registryEntry = registryService.getEntry(sessionProvider, entryPath);
      doc = registryEntry.getDocument();
      element = doc.getDocumentElement();
      rootNodePath = getAttributeSmart(element, "value");

      LOG.info("Workspace from RegistryService: " + repoWorkspaceName);
      LOG.info("RootNode from RegistryService: " + rootNodePath);
   }

   /**
    * Write parameters to RegistryService.
    * 
    * @param sessionProvider
    *          The SessionProvider
    * @throws ParserConfigurationException
    * @throws SAXException
    * @throws IOException
    * @throws RepositoryException
    */
   private void writeParamsToRegistryService(SessionProvider sessionProvider) throws IOException, SAXException,
            ParserConfigurationException, RepositoryException
   {

      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement(SERVICE_NAME);
      doc.appendChild(root);

      Element element = doc.createElement("workspace");
      setAttributeSmart(element, "value", repoWorkspaceName);
      root.appendChild(element);

      element = doc.createElement("rootNode");
      setAttributeSmart(element, "value", rootNodePath);
      root.appendChild(element);

      RegistryEntry serviceEntry = new RegistryEntry(doc);
      registryService.createEntry(sessionProvider, RegistryService.EXO_SERVICES, serviceEntry);
   }

   /**
    * Get attribute value.
    * 
    * @param element
    *          The element to get attribute value
    * @param attr
    *          The attribute name
    * @return Value of attribute if present and null in other case
    */
   private String getAttributeSmart(Element element, String attr)
   {
      return element.hasAttribute(attr) ? element.getAttribute(attr) : null;
   }

   /**
    * Set attribute value. If value is null the attribute will be removed.
    * 
    * @param element
    *          The element to set attribute value
    * @param attr
    *          The attribute name
    * @param value
    *          The value of attribute
    */
   private void setAttributeSmart(Element element, String attr, String value)
   {
      if (value == null)
      {
         element.removeAttribute(attr);
      }
      else
      {
         element.setAttribute(attr, value);
      }
   }

   /**
    * Get parameters which passed from the configuration file.
    * 
    * @throws RepositoryConfigurationException
    * 
    * @throws RepositoryConfigurationException
    */
   private void readParamsFromFile()
   {
      PropertiesParam props = initParams.getPropertiesParam("artifact.workspace");

      if (props == null)
      {
         throw new IllegalArgumentException("Property parameters 'locations' expected");
      }

      repoWorkspaceName = props.getProperty("workspace");
      rootNodePath = props.getProperty("rootNode");

      LOG.info("Workspace from configuration file: " + repoWorkspaceName);
      LOG.info("RootNode from configuration file: " + rootNodePath);
   }

   public List getListErrors()
   {
      return listErrorPom;
   }

}
