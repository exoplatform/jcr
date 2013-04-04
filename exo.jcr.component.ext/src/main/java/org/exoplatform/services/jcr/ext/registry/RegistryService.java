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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
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
 * @LevelAPI Platform
 */
public class RegistryService extends Registry implements Startable
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.RegistryService");

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

   protected final List<String> mixinNames;

   protected boolean started = false;

   /**
    * @param params
    *          accepts "locations" properties param
    * @param repositoryService the repository service
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public RegistryService(InitParams params, RepositoryService repositoryService)
      throws RepositoryConfigurationException
   {

      this.repositoryService = repositoryService;
      this.regWorkspaces = new HashMap<String, String>();
      if (params == null)
      {
         throw new RepositoryConfigurationException("Init parameters expected");
      }

      this.props = params.getPropertiesParam("locations");
      if (props == null)
      {
         throw new RepositoryConfigurationException("Property parameters 'locations' expected");
      }

      ValuesParam mixinValues = params.getValuesParam("mixin-names");
      if (mixinValues != null)
      {
         this.mixinNames = params.getValuesParam("mixin-names").getValues();
      }
      else
      {
         this.mixinNames = new ArrayList<String>();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
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
   @Override
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
   @Override
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
   @Override
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
   public void updateEntry(final SessionProvider sessionProvider, final String groupPath, final RegistryEntry entry)
      throws RepositoryException
   {

      final String entryRelPath = EXO_REGISTRY + "/" + groupPath + "/" + entry.getName();
      final String parentFullPath = "/" + EXO_REGISTRY + "/" + groupPath;

      try
      {
         Session session = session(sessionProvider, repositoryService.getCurrentRepository());

         // Don't care about concurrency, Session should be dedicated to the Thread, see JCR-765
         // synchronized (session) {
         try { 
            Node node = session.getRootNode().getNode(entryRelPath);
         // delete existing entry...
            node.remove();
         // create same entry,
            // [PN] no check we need here, as we have deleted this node before
            // checkGroup(sessionProvider, fullParentPath);
            session.importXML(parentFullPath, entry.getAsInputStream(), IMPORT_UUID_CREATE_NEW);
         } catch (PathNotFoundException e) {
            createEntry(sessionProvider, groupPath, entry);
         }
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
   @Override
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
               InputStream xml = SecurityHelper.doPrivilegedAction(new PrivilegedAction<InputStream>()
               {
                  public InputStream run()
                  {
                     return getClass().getResourceAsStream(NT_FILE);
                  }
               });

               try
               {
                  repositoryService.getRepository(repName).getNodeTypeManager()
                     .registerNodeTypes(xml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
               }
               finally
               {
                  try
                  {
                     xml.close();
                  }
                  catch (Exception e)
                  {
                     if (LOG.isTraceEnabled())
                     {
                        LOG.trace("An exception occurred: " + e.getMessage());
                     }
                  }                  
               }
            }
            initStorage(false);

            started = true;
         }
         catch (RepositoryConfigurationException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
         catch (RepositoryException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      else if (LOG.isDebugEnabled())
         LOG.warn("Registry service already started");
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   /**
    * Init the registry storage
    * @param replace <code>true</code> if wants to re-create exo:registry node, <code>false</code> otherwise
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public void initStorage(boolean replace) throws RepositoryConfigurationException, RepositoryException
   {
      for (RepositoryEntry repConfiguration : repConfigurations())
      {
         String repName = repConfiguration.getName();
         ManageableRepository rep = repositoryService.getRepository(repName);
         final Session sysSession = rep.getSystemSession(regWorkspaces.get(repName));

         try
         {
            if (sysSession.getRootNode().hasNode(EXO_REGISTRY) && replace)
            {
               sysSession.getRootNode().getNode(EXO_REGISTRY).remove();
            }

            Node rootNode;
            Node servicesNode;
            Node applicationsNode;
            Node usersNode;
            Node groupsNode;

            if (!sysSession.getRootNode().hasNode(EXO_REGISTRY))
            {
               rootNode = sysSession.getRootNode().addNode(EXO_REGISTRY, EXO_REGISTRY_NT);
               servicesNode = rootNode.addNode(EXO_SERVICES, EXO_REGISTRYGROUP_NT);
               applicationsNode = rootNode.addNode(EXO_APPLICATIONS, EXO_REGISTRYGROUP_NT);
               usersNode = rootNode.addNode(EXO_USERS, EXO_REGISTRYGROUP_NT);
               groupsNode = rootNode.addNode(EXO_GROUPS, EXO_REGISTRYGROUP_NT);

               Set<String> appNames = appConfigurations.keySet();
               final String fullPath = "/" + EXO_REGISTRY + "/" + entryLocation;
               for (String appName : appNames)
               {
                  final String xml = appConfigurations.get(appName);
                  try
                  {
                     SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
                     {
                        public Void run() throws Exception
                        {
                           DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                           ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
                           Document document = builder.parse(stream);
                           RegistryEntry entry = new RegistryEntry(document);
                           sysSession.importXML(fullPath, entry.getAsInputStream(), IMPORT_UUID_CREATE_NEW);
                           return null;
                        }
                     });
                  }
                  catch (PrivilegedActionException pae)
                  {
                     Throwable cause = pae.getCause();
                     if (cause instanceof ParserConfigurationException)
                     {
                        LOG.error(cause.getLocalizedMessage(), cause);
                     }
                     else if (cause instanceof IOException)
                     {
                        LOG.error(cause.getLocalizedMessage(), cause);
                     }
                     else if (cause instanceof SAXException)
                     {
                        LOG.error(cause.getLocalizedMessage(), cause);
                     }
                     else if (cause instanceof TransformerException)
                     {
                        LOG.error(cause.getLocalizedMessage(), cause);
                     }
                     else if (cause instanceof RuntimeException)
                     {
                        throw (RuntimeException)cause;
                     }
                     else
                     {
                        throw new RuntimeException(cause);
                     }
                  }
               }
               sysSession.save();
            }
            else
            {
               rootNode = sysSession.getRootNode().getNode(EXO_REGISTRY);
               servicesNode = rootNode.getNode(EXO_SERVICES);
               applicationsNode = rootNode.getNode(EXO_APPLICATIONS);
               usersNode = rootNode.getNode(EXO_USERS);
               groupsNode = rootNode.getNode(EXO_GROUPS);
            }

            for (String mixin : mixinNames)
            {
               if (rootNode.canAddMixin(mixin))
               {
                  rootNode.addMixin(mixin);
               }

               if (servicesNode.canAddMixin(mixin))
               {
                  servicesNode.addMixin(mixin);
               }

               if (applicationsNode.canAddMixin(mixin))
               {
                  applicationsNode.addMixin(mixin);
               }

               if (usersNode.canAddMixin(mixin))
               {
                  usersNode.addMixin(mixin);
               }

               if (groupsNode.canAddMixin(mixin))
               {
                  groupsNode.addMixin(mixin);
               }
            }

            if (sysSession.hasPendingChanges())
            {
               sysSession.save();
            }
         }
         finally
         {
            sysSession.logout();
         }
      }
   }

   /**
    * Add a new registry entry
    * @param repositoryName the repository name
    * @param workspaceName the workspace name
    */
   public void addRegistryLocation(String repositoryName, String workspaceName)
   {
      regWorkspaces.put(repositoryName, workspaceName);
   }

   /**
    * @param repositoryName the repository name
    */
   public void removeRegistryLocation(String repositoryName)
   {
      regWorkspaces.remove(repositoryName);
   }

   /**
    * Init the registry entry
    * @param groupName the group entry name
    * @param entryName the entry name
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
            LOG.info("The RegistryEntry " + relPath + "is already initialized on repository " + repName);
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
      return repositoryService.getConfig().getRepositoryConfigurations();
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
