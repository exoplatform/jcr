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
package org.exoplatform.services.jcr.ext.registry;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by The eXo Platform SAS . <br/>
 * 
 * Centralized collector for JCR based entities (services, apps, users) It contains info about the
 * whole system, i.e. for all repositories used by system. All operations performed in context of
 * "current" repository, i.e. RepositoryService.getCurrentRepository() Each repository has own
 * Registry storage which is placed in workspace configured in "locations" entry like:
 * <properties-param> <name>locations</name> <description>registry locations</description> <property
 * name="repository1" value="workspace1"/> <property name="repository2" value="workspace2"/> The
 * implementation hides storage details from end user
 * 
 * 
 * @author Gennady Azarenkov
 * @version $Id: RegistryService.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class RegistryService extends Registry implements Startable
{

   private static Log log = ExoLogger.getLogger("jcr.RegistryService");

   protected final static String EXO_REGISTRY_NT = "exo:registry";

   protected final static String EXO_REGISTRYENTRY_NT = "exo:registryEntry";

   protected final static String EXO_REGISTRYGROUP_NT = "exo:registryGroup";

   protected final static String NT_FILE = "registry-nodetypes.xml";

   protected final static String EXO_REGISTRY = "exo:registry";

   protected final static String EXO_REGISTRYENTRY = "exo:registryEntry";

   public final static String EXO_SERVICES = "exo:services";

   public final static String EXO_APPLICATIONS = "exo:applications";

   public final static String EXO_USERS = "exo:users";

   public final static String EXO_GROUPS = "exo:groups";

   protected final Map<String, String> regWorkspaces;

   private HashMap<String, String> appConfigurations = new HashMap<String, String>();

   private String entryLocation;
   
   private final PropertiesParam props;

   protected final RepositoryService repositoryService;

   // TODO temporary flag to have start() run once
   protected boolean started = false;

   /**
    * @param params
    *          accepts "locations" properties param
    * @param repositoryService
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    * @throws FileNotFoundException
    */
   public RegistryService(InitParams params, RepositoryService repositoryService)
      throws RepositoryConfigurationException
   {

      this.repositoryService = repositoryService;
      this.regWorkspaces = new HashMap<String, String>();
      if (params == null)
         throw new RepositoryConfigurationException("Init parameters expected");
      this.props = params.getPropertiesParam("locations");
      if (props == null)
         throw new RepositoryConfigurationException("Property parameters 'locations' expected");
   }

   /**
    * {@inheritDoc}
    */
   public RegistryEntry getEntry(final SessionProvider sessionProvider, final String entryPath)
      throws PathNotFoundException, RepositoryException
   {

      final String fullPath = "/" + EXO_REGISTRY + "/" + entryPath;
      Session session = session(sessionProvider, repositoryService.getCurrentRepository());
      try
      {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         session.exportDocumentView(fullPath, out, true, false);
         return RegistryEntry.parse(out.toByteArray());
      }
      catch (IOException e)
      {
         throw new RepositoryException("Can't export node " + fullPath + " to XML representation " + e);
      }
      catch (ParserConfigurationException e)
      {
         throw new RepositoryException("Can't export node " + fullPath + " to XML representation " + e);
      }
      catch (SAXException e)
      {
         throw new RepositoryException("Can't export node " + fullPath + " to XML representation " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void createEntry(final SessionProvider sessionProvider, final String groupPath, final RegistryEntry entry)
      throws RepositoryException
   {

      final String fullPath = "/" + EXO_REGISTRY + "/" + groupPath;
      try
      {
         checkGroup(sessionProvider, groupPath);
         session(sessionProvider, repositoryService.getCurrentRepository()).getWorkspace().importXML(fullPath,
            entry.getAsInputStream(), IMPORT_UUID_CREATE_NEW);
      }
      catch (IOException ioe)
      {
         throw new RepositoryException("Item " + fullPath + "can't be created " + ioe);
      }
      catch (ItemExistsException iee)
      {
         throw new RepositoryException("Item " + fullPath + "alredy exists " + iee);
      }
      catch (TransformerException te)
      {
         throw new RepositoryException("Can't get XML representation from stream " + te);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeEntry(final SessionProvider sessionProvider, final String entryPath) throws RepositoryException
   {

      Node root = session(sessionProvider, repositoryService.getCurrentRepository()).getRootNode();
      Node node = root.getNode(EXO_REGISTRY + "/" + entryPath);
      Node parent = node.getParent();
      node.remove();
      parent.save();
   }

   /**
    * {@inheritDoc}
    */
   public void recreateEntry(final SessionProvider sessionProvider, final String groupPath, final RegistryEntry entry)
      throws RepositoryException
   {

      final String entryRelPath = EXO_REGISTRY + "/" + groupPath + "/" + entry.getName();
      final String parentFullPath = "/" + EXO_REGISTRY + "/" + groupPath;

      try
      {
         Session session = session(sessionProvider, repositoryService.getCurrentRepository());

         // Don't care about concurrency, Session should be dedicated to the Thread, see JCR-765
         // synchronized (session) {
         Node node = session.getRootNode().getNode(entryRelPath);

         // delete existing entry...
         node.remove();

         // create same entry,
         // [PN] no check we need here, as we have deleted this node before
         // checkGroup(sessionProvider, fullParentPath);
         session.importXML(parentFullPath, entry.getAsInputStream(), IMPORT_UUID_CREATE_NEW);

         // save recreated changes
         session.save();
         // }
      }
      catch (IOException ioe)
      {
         throw new RepositoryException("Item " + parentFullPath + "can't be created " + ioe);
      }
      catch (TransformerException te)
      {
         throw new RepositoryException("Can't get XML representation from stream " + te);
      }
   }

   /**
    * {@inheritDoc}
    */
   public RegistryNode getRegistry(final SessionProvider sessionProvider) throws RepositoryException
   {

      return new RegistryNode(session(sessionProvider, repositoryService.getCurrentRepository()).getRootNode().getNode(
         EXO_REGISTRY));
   }

   /**
    * @param sessionProvider
    * @param repo
    * @return session
    * @throws RepositoryException
    */
   private Session session(final SessionProvider sessionProvider, final ManageableRepository repo)
      throws RepositoryException
   {

      return sessionProvider.getSession(regWorkspaces.get(repo.getConfiguration().getName()), repo);
   }

   public boolean isStarted()
   {
      return started;
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      if (!started)
         try
         {
            for (RepositoryEntry repConfiguration : repConfigurations())
            {
               String repName = repConfiguration.getName();
               String wsName = null;
               if (props != null)
               {
                  wsName = props.getProperty(repName);
                  if (wsName == null)
                     wsName = repConfiguration.getDefaultWorkspaceName();
               }
               else
               {
                  wsName = repConfiguration.getDefaultWorkspaceName();
               }
               addRegistryLocation(repName, wsName);
               InputStream xml = getClass().getResourceAsStream(NT_FILE);
               try
               {
                  repositoryService.getRepository(repName).getNodeTypeManager().registerNodeTypes(xml,
                     ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
               }
               finally
               {
                  try
                  {
                     xml.close();
                  }
                  catch (Exception e)
                  {
                     //ignore me
                  }                  
               }
            }
            initStorage(false);

            started = true;
         }
         catch (RepositoryConfigurationException e)
         {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
         }
         catch (RepositoryException e)
         {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
         }
      else if (log.isDebugEnabled())
         log.warn("Registry service already started");
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   /**
    * @param replace
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public void initStorage(boolean replace) throws RepositoryConfigurationException, RepositoryException
   {
      for (RepositoryEntry repConfiguration : repConfigurations())
      {
         String repName = repConfiguration.getName();
         ManageableRepository rep = repositoryService.getRepository(repName);
         Session sysSession = rep.getSystemSession(regWorkspaces.get(repName));

         if (sysSession.getRootNode().hasNode(EXO_REGISTRY) && replace)
            sysSession.getRootNode().getNode(EXO_REGISTRY).remove();

         if (!sysSession.getRootNode().hasNode(EXO_REGISTRY))
         {
            Node rootNode = sysSession.getRootNode().addNode(EXO_REGISTRY, EXO_REGISTRY_NT);
            rootNode.addNode(EXO_SERVICES, EXO_REGISTRYGROUP_NT);
            rootNode.addNode(EXO_APPLICATIONS, EXO_REGISTRYGROUP_NT);
            rootNode.addNode(EXO_USERS, EXO_REGISTRYGROUP_NT);
            rootNode.addNode(EXO_GROUPS, EXO_REGISTRYGROUP_NT);

            Set<String> appNames = appConfigurations.keySet();
            final String fullPath = "/" + EXO_REGISTRY + "/" + entryLocation;
            for (String appName : appNames)
            {
               String xml = appConfigurations.get(appName);
               try
               {
                  DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                  ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
                  Document document = builder.parse(stream);
                  RegistryEntry entry = new RegistryEntry(document);
                  sysSession.importXML(fullPath, entry.getAsInputStream(), IMPORT_UUID_CREATE_NEW);
               }
               catch (ParserConfigurationException e)
               {
                  e.printStackTrace();
               }
               catch (IOException e)
               {
                  e.printStackTrace();
               }
               catch (SAXException e)
               {
                  e.printStackTrace();
               }
               catch (TransformerException e)
               {
                  e.printStackTrace();
               }

            }
            sysSession.save();
         }
         sysSession.logout();
      }
   }

   /**
    * @param repositoryName
    * @param workspaceName
    */
   public void addRegistryLocation(String repositoryName, String workspaceName)
   {
      regWorkspaces.put(repositoryName, workspaceName);
   }

   /**
    * @param repositoryName
    */
   public void removeRegistryLocation(String repositoryName)
   {
      regWorkspaces.remove(repositoryName);
   }

   /**
    * @param sessionProvider
    * @param groupName
    * @param entryName
    * @param repositoryName
    * @return
    * @throws RepositoryException
    */
   public void initRegistryEntry(String groupName, String entryName) throws RepositoryException,
      RepositoryConfigurationException
   {

      String relPath = EXO_REGISTRY + "/" + groupName + "/" + entryName;
      for (RepositoryEntry repConfiguration : repConfigurations())
      {
         String repName = repConfiguration.getName();
         SessionProvider sysProvider = SessionProvider.createSystemProvider();
         Node root = session(sysProvider, repositoryService.getRepository(repName)).getRootNode();
         if (!root.hasNode(relPath))
         {
            root.addNode(relPath, EXO_REGISTRYENTRY_NT);
            root.save();
         }
         else
         {
            log.info("The RegistryEntry " + relPath + "is already initialized on repository " + repName);
         }
         sysProvider.close();
      }
   }

   /**
    * @return repository service
    */
   public RepositoryService getRepositoryService()
   {
      return repositoryService;
   }

   /**
    * Get value of force-xml-configuration param.
    * 
    * @param initParams
    *          The InitParams
    * @return force-xml-configuration value if present and false in other case
    */
   public boolean getForceXMLConfigurationValue(InitParams initParams)
   {
      ValueParam valueParam = initParams.getValueParam("force-xml-configuration");
      return (valueParam != null ? Boolean.valueOf(valueParam.getValue()) : false);
   }

   /**
    * @return repository entries
    */
   private List<RepositoryEntry> repConfigurations()
   {
      return (List<RepositoryEntry>)repositoryService.getConfig().getRepositoryConfigurations();
   }

   /**
    * check if group exists and creates one if necessary
    * 
    * @param sessionProvider
    * @param groupPath
    * @throws RepositoryException
    */
   private void checkGroup(final SessionProvider sessionProvider, final String groupPath) throws RepositoryException
   {
      String[] groupNames = groupPath.split("/");
      String prefix = "/" + EXO_REGISTRY;
      Session session = session(sessionProvider, repositoryService.getCurrentRepository());
      for (String name : groupNames)
      {
         String path = prefix + "/" + name;
         try
         {
            Node group = (Node)session.getItem(path);
            if (!group.isNodeType(EXO_REGISTRYGROUP_NT))
               throw new RepositoryException("Node at " + path + " should be  " + EXO_REGISTRYGROUP_NT + " type");
         }
         catch (PathNotFoundException e)
         {
            Node parent = (Node)session.getItem(prefix);
            parent.addNode(name, EXO_REGISTRYGROUP_NT);
            parent.save();
         }
         prefix = path;
      }
   }

   public void addPlugin(ComponentPlugin plugin)
   {
      if (RegistryInitializationEntryPlugin.class.isAssignableFrom(plugin.getClass()))
      {
         RegistryInitializationEntryPlugin registryPlugin = (RegistryInitializationEntryPlugin)plugin;
         appConfigurations = registryPlugin.getAppConfiguration();
         entryLocation = registryPlugin.getLocation();
         if (entryLocation == null)
            entryLocation = EXO_APPLICATIONS;
      }
   }

}
