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
package org.exoplatform.services.jcr.ext.script.groovy;

import org.apache.commons.fileupload.FileItem;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RegistryEntry;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.script.groovy.GroovyScriptInstantiator;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: GroovyScript2RestLoader.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
@Path("script/groovy")
public class GroovyScript2RestLoader implements Startable
{

   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.GroovyScript2RestLoader");

   /** Default node types for Groovy scripts. */
   private static final String DEFAULT_NODETYPE = "exo:groovyResourceContainer";

   /** Service name. */
   private static final String SERVICE_NAME = "GroovyScript2RestLoader";

   /** See {@link InitParams}. */
   private InitParams initParams;

   /** See {@link ResourceBinder}. */
   private ResourceBinder binder;

   /** See {@link GroovyScriptInstantiator}. */
   private GroovyScriptInstantiator groovyScriptInstantiator;

   /** See {@link RepositoryService}. */
   private RepositoryService repositoryService;

   /** See {@link ConfigurationManager}. */
   private ConfigurationManager configurationManager;

   /** See {@link RegistryService}. */
   private RegistryService registryService;

   /**
    * See {@link SessionProviderService},
    * {@link ThreadLocalSessionProviderService}.
    */
   private ThreadLocalSessionProviderService sessionProviderService;

   /** Keeps configuration for observation listener. */
   private ObservationListenerConfiguration observationListenerConfiguration;

   /** Node type for Groovy scripts. */
   private String nodeType;

   /** Mapping scripts URL (or other key) to classes. */
   private Map<ScriptKey, Class<?>> scriptsURL2ClassMap = new HashMap<ScriptKey, Class<?>>();

   /**
    * @param binder binder for RESTful services
    * @param groovyScriptInstantiator instantiate groovy scripts
    * @param repositoryService See {@link RepositoryService}
    * @param sessionProviderService See {@link SessionProviderService}
    * @param configurationManager for solve resource loading issue in common way
    * @param params initialized parameters
    */
   public GroovyScript2RestLoader(ResourceBinder binder, GroovyScriptInstantiator groovyScriptInstantiator,
      RepositoryService repositoryService, ThreadLocalSessionProviderService sessionProviderService,
      ConfigurationManager configurationManager, InitParams params)
   {
      this(binder, groovyScriptInstantiator, repositoryService, sessionProviderService, configurationManager, null,
         params);
   }

   /**
    * @param binder binder for RESTful services
    * @param groovyScriptInstantiator instantiates Groovy scripts
    * @param repositoryService See {@link RepositoryService}
    * @param sessionProviderService See {@link SessionProviderService}
    * @param configurationManager for solve resource loading issue in common way
    * @param registryService See {@link RegistryService}
    * @param params initialized parameters
    */
   public GroovyScript2RestLoader(ResourceBinder binder, GroovyScriptInstantiator groovyScriptInstantiator,
      RepositoryService repositoryService, ThreadLocalSessionProviderService sessionProviderService,
      ConfigurationManager configurationManager, RegistryService registryService, InitParams params)
   {

      this.binder = binder;
      this.groovyScriptInstantiator = groovyScriptInstantiator;
      this.repositoryService = repositoryService;
      this.configurationManager = configurationManager;
      this.registryService = registryService;
      this.sessionProviderService = sessionProviderService;
      this.initParams = params;
   }

   /**
    * Remove script with specified URL from ResourceBinder.
    * 
    * @param url the URL. The <code>url.toString()</code> must be corresponded to
    *          script class.
    * @see GroovyScriptRestLoader#loadScript(URL).
    */
   public void unloadScript(URL url)
   {
      unloadScript(new URLScriptKey(url));
   }

   /**
    * Remove script by specified key from ResourceBinder.
    * 
    * @param key the key with which script was created.
    * @see GroovyScript2RestLoader#loadScript(String, InputStream)
    * @see GroovyScript2RestLoader#loadScript(String, String, InputStream)
    * @deprecated
    */
   public boolean unloadScript(String key)
   {
      return unloadScript(new SimpleScriptKey(key));
   }

   public boolean unloadScript(ScriptKey key)
   {
      if (scriptsURL2ClassMap.containsKey(key))
      {
         if (binder.unbind(scriptsURL2ClassMap.get(key)))
         {
            scriptsURL2ClassMap.remove(key);
            return true;
         }
         else
         {
            LOG.warn("Can't remove groovy script, key " + key);
         }
      }
      else
      {
         LOG.warn("Specified key '" + key + "' does not corresponds to any class name.");
      }
      return false;
   }

   /**
    * @param key script's key
    * @return true if script loaded false otherwise
    * @deprecated
    */
   public boolean isLoaded(String key)
   {
      return isLoaded(new SimpleScriptKey(key));
   }

   /**
    * @param url script's URL
    * @return true if script loaded false otherwise
    */
   public boolean isLoaded(URL url)
   {
      return isLoaded(new URLScriptKey(url));
   }

   /**
    * @param key script's key. With this key script was created.
    * @return true if script loaded false otherwise
    */
   public boolean isLoaded(ScriptKey key)
   {
      return scriptsURL2ClassMap.containsKey(key);
   }

   /**
    * Get node type for store scripts, may throw {@link IllegalStateException} if
    * <tt>nodeType</tt> not initialized yet.
    * 
    * @return return node type
    */
   public String getNodeType()
   {
      if (nodeType == null)
         throw new IllegalStateException("Node type not initialized, yet. ");
      return nodeType;
   }

   /**
    * @param url the URL for loading script.
    * @throws IOException it script can't be loaded.
    */
   public boolean loadScript(URL url) throws IOException
   {
      Object resource = groovyScriptInstantiator.instantiateScript(url);
      if (binder.bind(resource))
      {
         // add mapping script URL to name of class.
         scriptsURL2ClassMap.put(new URLScriptKey(url), resource.getClass());
         return true;
      }
      else
      {
         LOG.warn("Groovy script was not binded, URL: " + url);
         return false;
      }
   }

   /**
    * Load script from given stream.
    * 
    * @param key the key which must be corresponded to object class name.
    * @param stream the stream which represents groovy script.
    * @return if script loaded false otherwise
    * @throws IOException if script can't be loaded or parsed.
    * @see ResourceBinder#bind(ResourceContainer)
    * @deprecated
    */
   public boolean loadScript(String key, InputStream stream) throws IOException
   {
      return loadScript(key, null, stream);
   }

   /**
    * Load script from given stream.
    * 
    * @param key the key which must be corresponded to object class name.
    * @param name this name will be passed to compiler to get understandable if
    *          compilation failed
    * @param stream the stream which represents groovy script.
    * @return if script loaded false otherwise
    * @throws IOException if script can't be loaded or parsed.
    * @see ResourceBinder#bind(ResourceContainer)
    * @deprecated
    */
   public boolean loadScript(String key, String name, InputStream stream) throws IOException
   {
      return loadScript(new SimpleScriptKey(key), name, stream);
   }

   /**
    * @param key the key which must be corresponded to object class name
    * @param name this name will be passed to compiler to get understandable if
    *          compilation failed. This parameter may be set as null then groovy
    *          compiler will user automatically generated name for script.
    *          (script000001.groovy)
    * @param stream the stream which represents groovy script.
    * @return if script loaded false otherwise
    * @throws IOException if script can't be loaded or parsed
    */
   public boolean loadScript(ScriptKey key, String name, InputStream stream) throws IOException
   {
      Object resource = groovyScriptInstantiator.instantiateScript(stream, name);
      if (binder.bind(resource))
      {
         scriptsURL2ClassMap.put(key, resource.getClass());
         return true;
      }
      else
      {
         LOG.warn("Groovy script was not binded, key: " + key);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      if (registryService != null && !registryService.getForceXMLConfigurationValue(initParams))
      {
         SessionProvider sessionProvider = SessionProvider.createSystemProvider();
         try
         {
            readParamsFromRegistryService(sessionProvider);
         }
         catch (Exception e)
         {
            readParamsFromFile();
            try
            {
               writeParamsToRegistryService(sessionProvider);
            }
            catch (Exception exc)
            {
               LOG.error("Cannot write init configuration to RegistryService.", exc);
            }
         }
         finally
         {
            sessionProvider.close();
         }
      }
      else
      {
         readParamsFromFile();
      }

      // Add script from configuration files to JCR.
      addScripts();

      try
      {

         // Deploy auto-load scripts and start Observation Listeners.
         String repositoryName = observationListenerConfiguration.getRepository();
         List<String> workspaceNames = observationListenerConfiguration.getWorkspaces();

         ManageableRepository repository = repositoryService.getRepository(repositoryName);

         for (String workspaceName : workspaceNames)
         {
            Session session = repository.getSystemSession(workspaceName);

            String xpath = "//element(*, " + getNodeType() + ")[@exo:autoload='true']";
            Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);

            QueryResult result = query.execute();
            NodeIterator nodeIterator = result.getNodes();
            while (nodeIterator.hasNext())
            {
               Node node = nodeIterator.nextNode();

               if (node.getPath().startsWith("/jcr:system"))
                  continue;

               loadScript(new NodeScriptKey(repositoryName, workspaceName, node), node.getPath(), node.getProperty(
                  "jcr:data").getStream());
            }

            session.getWorkspace().getObservationManager().addEventListener(
               new GroovyScript2RestUpdateListener(repositoryName, workspaceName, this, session),
               Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, "/", true, null,
               new String[]{getNodeType()}, false);
         }
      }
      catch (Exception e)
      {
         LOG.error("Error occurs ", e);
      }
      // Finally bind this object as RESTful service.
      // NOTE this service does not implement ResourceContainer, as usually
      // done for this type of services. It can't be binded in common way cause
      // to dependencies problem. And in other side not possible to use third
      // part which can be injected by GroovyScript2RestLoader.
      binder.bind(this);
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      // nothing to do!
   }

   /**
    * See {@link GroovyScript2RestLoaderPlugin}.
    */
   private List<GroovyScript2RestLoaderPlugin> loadPlugins;

   /**
    * @param cp See {@link ComponentPlugin}
    */
   public void addPlugin(ComponentPlugin cp)
   {
      if (cp instanceof GroovyScript2RestLoaderPlugin)
      {
         if (loadPlugins == null)
            loadPlugins = new ArrayList<GroovyScript2RestLoaderPlugin>();
         loadPlugins.add((GroovyScript2RestLoaderPlugin)cp);
      }
   }

   /**
    * Add scripts that specified in configuration.
    */
   private void addScripts()
   {
      if (loadPlugins == null || loadPlugins.size() == 0)
         return;
      for (GroovyScript2RestLoaderPlugin loadPlugin : loadPlugins)
      {
         // If no one script configured then skip this item,
         // there is no reason to do anything.
         if (loadPlugin.getXMLConfigs().size() == 0)
            continue;

         Session session = null;
         try
         {
            ManageableRepository repository = repositoryService.getRepository(loadPlugin.getRepository());
            String workspace = loadPlugin.getWorkspace();
            session = repository.getSystemSession(workspace);
            String nodeName = loadPlugin.getNode();
            Node node = null;
            try
            {
               node = (Node)session.getItem(nodeName);
            }
            catch (PathNotFoundException e)
            {
               StringTokenizer tokens = new StringTokenizer(nodeName, "/");
               node = session.getRootNode();
               while (tokens.hasMoreTokens())
               {
                  String t = tokens.nextToken();
                  if (node.hasNode(t))
                     node = node.getNode(t);
                  else
                     node = node.addNode(t, "nt:folder");
               }
            }

            for (XMLGroovyScript2Rest xg : loadPlugin.getXMLConfigs())
            {
               String scriptName = xg.getName();
               if (node.hasNode(scriptName))
               {
                  LOG.warn("Node '" + node.getPath() + "/" + scriptName + "' already exists. ");
                  continue;
               }

               createScript(node, scriptName, xg.isAutoload(), configurationManager.getInputStream(xg.getPath()));
            }
            session.save();
         }
         catch (Exception e)
         {
            LOG.error("Failed add scripts. ", e);
         }
         finally
         {
            if (session != null)
               session.logout();
         }
      }
   }

   /**
    * Create JCR node.
    * 
    * @param parent parent node
    * @param name name of node to be created
    * @param stream data stream for property jcr:data
    * @return newly created node
    * @throws Exception if any errors occurs
    */
   private Node createScript(Node parent, String name, boolean autoload, InputStream stream) throws Exception
   {
      Node scriptFile = parent.addNode(name, "nt:file");
      Node script = scriptFile.addNode("jcr:content", getNodeType());
      script.setProperty("exo:autoload", autoload);
      script.setProperty("jcr:mimeType", "script/groovy");
      script.setProperty("jcr:lastModified", Calendar.getInstance());
      script.setProperty("jcr:data", stream);
      return scriptFile;
   }

   /**
    * Read parameters from RegistryService.
    * 
    * @param sessionProvider the SessionProvider
    * @throws RepositoryException
    * @throws PathNotFoundException
    */
   private void readParamsFromRegistryService(SessionProvider sessionProvider) throws PathNotFoundException,
      RepositoryException
   {

      if (LOG.isDebugEnabled())
         LOG.debug("<<< Read init parametrs from registry service.");

      observationListenerConfiguration = new ObservationListenerConfiguration();

      String entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + "nodeType";
      RegistryEntry registryEntry = registryService.getEntry(sessionProvider, entryPath);
      Document doc = registryEntry.getDocument();
      Element element = doc.getDocumentElement();
      nodeType = getAttributeSmart(element, "value");

      entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + "repository";
      registryEntry = registryService.getEntry(sessionProvider, entryPath);
      doc = registryEntry.getDocument();
      element = doc.getDocumentElement();
      observationListenerConfiguration.setRepository(getAttributeSmart(element, "value"));

      entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + "workspaces";
      registryEntry = registryService.getEntry(sessionProvider, entryPath);
      doc = registryEntry.getDocument();
      element = doc.getDocumentElement();
      String workspaces = getAttributeSmart(element, "value");

      String ws[] = workspaces.split(";");
      List<String> wsList = new ArrayList<String>();
      for (String w : ws)
      {
         wsList.add(w);
      }

      observationListenerConfiguration.setWorkspaces(wsList);

      LOG.info("NodeType from RegistryService: " + getNodeType());
      LOG.info("Repository from RegistryService: " + observationListenerConfiguration.getRepository());
      LOG.info("Workspaces node from RegistryService: " + observationListenerConfiguration.getWorkspaces());
   }

   /**
    * Write parameters to RegistryService.
    * 
    * @param sessionProvider the SessionProvider
    * @throws ParserConfigurationException
    * @throws SAXException
    * @throws IOException
    * @throws RepositoryException
    */
   private void writeParamsToRegistryService(SessionProvider sessionProvider) throws IOException, SAXException,
      ParserConfigurationException, RepositoryException
   {
      if (LOG.isDebugEnabled())
         LOG.debug(">>> Save init parametrs in registry service.");

      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement(SERVICE_NAME);
      doc.appendChild(root);

      Element element = doc.createElement("nodeType");
      setAttributeSmart(element, "value", getNodeType());
      root.appendChild(element);

      StringBuffer sb = new StringBuffer();
      for (String workspace : observationListenerConfiguration.getWorkspaces())
      {
         if (sb.length() > 0)
            sb.append(';');
         sb.append(workspace);
      }
      element = doc.createElement("workspaces");
      setAttributeSmart(element, "value", sb.toString());
      root.appendChild(element);

      element = doc.createElement("repository");
      setAttributeSmart(element, "value", observationListenerConfiguration.getRepository());
      root.appendChild(element);

      RegistryEntry serviceEntry = new RegistryEntry(doc);
      registryService.createEntry(sessionProvider, RegistryService.EXO_SERVICES, serviceEntry);
   }

   /**
    * Get attribute value.
    * 
    * @param element The element to get attribute value
    * @param attr The attribute name
    * @return Value of attribute if present and null in other case
    */
   private String getAttributeSmart(Element element, String attr)
   {
      return element.hasAttribute(attr) ? element.getAttribute(attr) : null;
   }

   /**
    * Set attribute value. If value is null the attribute will be removed.
    * 
    * @param element The element to set attribute value
    * @param attr The attribute name
    * @param value The value of attribute
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
    * Read parameters from file.
    */
   private void readParamsFromFile()
   {
      if (initParams != null)
      {
         nodeType =
            initParams.getValuesParam("nodetype") != null ? initParams.getValueParam("nodetype").getValue()
               : DEFAULT_NODETYPE;

         ObjectParameter param = initParams.getObjectParam("observation.config");
         observationListenerConfiguration = (ObservationListenerConfiguration)param.getObject();
      }

      LOG.info("NodeType from configuration file: " + getNodeType());
      LOG.info("Repository from configuration file: " + observationListenerConfiguration.getRepository());
      LOG.info("Workspaces node from configuration file: " + observationListenerConfiguration.getWorkspaces());
   }

   // ///////////////////

   // FIXME
   // For following resource methods use POST instead GET even there is no
   // entity in request to prevent browsers cache when use ajax. Using no cache
   // may not have sense currently when use gadgets.io cause apache shindig
   // implementation ignores all headers except Set-Cookie and Location
   // (see https://issues.apache.org/jira/browse/SHINDIG-882)

   /**
    * This method is useful for clients that can send script in request body
    * without form-data. At required to set specific Content-type header
    * 'script/groovy'.
    * 
    * @param stream the stream that contains groovy source code
    * @param uriInfo see {@link UriInfo}
    * @param repository repository name
    * @param workspace workspace name
    * @param path path to resource to be created
    * @return Response with status 'created'
    */
   @POST
   @Consumes({"script/groovy"})
   @Path("add/{repository}/{workspace}/{path:.*}")
   public Response addScript(InputStream stream, @Context UriInfo uriInfo, @PathParam("repository") String repository,
      @PathParam("workspace") String workspace, @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node node = (Node)ses.getItem(getPath(path));
         createScript(node, getName(path), false, stream);
         ses.save();
         URI location = uriInfo.getBaseUriBuilder().path(getClass(), "getScript").build(repository, workspace, path);
         return Response.created(location).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * @param name script name
    * @param stream script for validation
    */
   @POST
   @Consumes({"script/groovy"})
   @Path("validate{name:.*}")
   public Response validateScript(@PathParam("name") String name, InputStream script)
   {

      try
      {
         if (name != null && name.startsWith("/"))
            name = name.substring(1);
         if (name == null || name.length() == 0)
            groovyScriptInstantiator.instantiateScript(script);
         else
            groovyScriptInstantiator.instantiateScript(script, name);
         return Response.status(Response.Status.OK).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.BAD_REQUEST).entity("Unexpected error. " + e.getMessage()).type(
            MediaType.TEXT_PLAIN).build();
      }

   }

   /**
    * This method is useful for clients that can send script in request body
    * without form-data. At required to set specific Content-type header
    * 'script/groovy'.
    * 
    * @param stream the stream that contains groovy source code
    * @param uriInfo see {@link UriInfo}
    * @param repository repository name
    * @param workspace workspace name
    * @param path path to resource to be created
    * @return Response with status 'created'
    */
   @POST
   @Consumes({"script/groovy"})
   @Path("update/{repository}/{workspace}/{path:.*}")
   public Response updateScript(InputStream stream, @Context UriInfo uriInfo,
      @PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node node = (Node)ses.getItem("/" + path);
         node.getNode("jcr:content").setProperty("jcr:data", stream);
         ses.save();
         URI location = uriInfo.getBaseUriBuilder().path(getClass(), "getScript").build(repository, workspace, path);
         return Response.created(location).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * This method is useful for clients that send scripts as file in
    * 'multipart/*' request body. <br/> NOTE even we use iterator item should be
    * only one, rule one address - one script. This method is created just for
    * comfort loading script from HTML form. NOT use this script for uploading
    * few files in body of 'multipart/form-data' or other type of multipart.
    * 
    * @param items iterator {@link FileItem}
    * @param uriInfo see {@link UriInfo}
    * @param repository repository name
    * @param workspace workspace name
    * @param path path to resource to be created
    * @return Response with status 'created'
    */
   @POST
   @Consumes({"multipart/*"})
   @Path("add/{repository}/{workspace}/{path:.*}")
   public Response addScript(Iterator<FileItem> items, @Context UriInfo uriInfo,
      @PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node node = (Node)ses.getItem(getPath(path));
         InputStream stream = null;
         boolean autoload = false;
         while (items.hasNext())
         {
            FileItem fitem = items.next();
            if (fitem.isFormField() && fitem.getFieldName() != null
               && fitem.getFieldName().equalsIgnoreCase("autoload"))
               autoload = Boolean.valueOf(fitem.getString());
            else if (!fitem.isFormField()) // accept files
               stream = fitem.getInputStream();
         }

         createScript(node, getName(path), autoload, stream);
         ses.save();
         URI location = uriInfo.getBaseUriBuilder().path(getClass(), "getScript").build(repository, workspace, path);
         return Response.created(location).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * This method is useful for clients that send scripts as file in
    * 'multipart/*' request body. <br/> NOTE even we use iterator item should be
    * only one, rule one address - one script. This method is created just for
    * comfort loading script from HTML form. NOT use this script for uploading
    * few files in body of 'multipart/form-data' or other type of multipart.
    * 
    * @param items iterator {@link FileItem}
    * @param uriInfo see {@link UriInfo}
    * @param repository repository name
    * @param workspace workspace name
    * @param path path to resource to be created
    * @return Response with status 'created'
    */
   @POST
   @Consumes({"multipart/*"})
   @Path("update/{repository}/{workspace}/{path:.*}")
   public Response updateScripts(Iterator<FileItem> items, @Context UriInfo uriInfo,
      @PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         FileItem fitem = items.next();
         InputStream stream = null;
         if (!fitem.isFormField()) // if file
            stream = fitem.getInputStream();
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node node = (Node)ses.getItem("/" + path);
         node.getNode("jcr:content").setProperty("jcr:data", stream);
         ses.save();
         URI location = uriInfo.getBaseUriBuilder().path(getClass(), "getScript").build(repository, workspace, path);
         return Response.created(location).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Get source code of groovy script.
    * 
    * @param repository repository name
    * @param workspace workspace name
    * @param path JCR path to node that contains script
    * @return groovy script as stream
    */
   @POST
   @Produces({"script/groovy"})
   @Path("src/{repository}/{workspace}/{path:.*}")
   public Response getScript(@PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node scriptFile = (Node)ses.getItem("/" + path);
         return Response.status(Response.Status.OK).entity(
            scriptFile.getNode("jcr:content").getProperty("jcr:data").getStream()).type("script/groovy").build();

      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Get groovy script's meta-information.
    * 
    * @param repository repository name
    * @param workspace workspace name
    * @param path JCR path to node that contains script
    * @return groovy script's meta-information
    */
   @POST
   @Produces({MediaType.APPLICATION_JSON})
   @Path("meta/{repository}/{workspace}/{path:.*}")
   public Response getScriptMetadata(@PathParam("repository") String repository,
      @PathParam("workspace") String workspace, @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node script = ((Node)ses.getItem("/" + path)).getNode("jcr:content");

         ScriptMetadata meta =
            new ScriptMetadata(script.getProperty("exo:autoload").getBoolean(), isLoaded(new NodeScriptKey(repository,
               workspace, script)), script.getProperty("jcr:mimeType").getString(), script.getProperty(
               "jcr:lastModified").getDate().getTimeInMillis());
         return Response.status(Response.Status.OK).entity(meta).type(MediaType.APPLICATION_JSON).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Remove node that contains groovy script.
    * 
    * @param repository repository name
    * @param workspace workspace name
    * @param path JCR path to node that contains script
    */
   @POST
   @Path("delete/{repository}/{workspace}/{path:.*}")
   public Response deleteScript(@PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         ses.getItem("/" + path).remove();
         ses.save();
         return Response.status(Response.Status.NO_CONTENT).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Change exo:autoload property. If this property is 'true' script will be
    * deployed automatically when JCR repository startup and automatically
    * re-deployed when script source code changed.
    * 
    * @param repository repository name
    * @param workspace workspace name
    * @param path JCR path to node that contains script
    * @param state value for property exo:autoload, if it is not specified then
    *          'true' will be used as default. <br /> Example:
    *          .../scripts/groovy/test1.groovy/load is the same to
    *          .../scripts/groovy/test1.groovy/load?state=true
    */
   @POST
   @Path("autoload/{repository}/{workspace}/{path:.*}")
   public Response autoload(@PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path, @DefaultValue("true") @QueryParam("state") boolean state)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node script = ((Node)ses.getItem("/" + path)).getNode("jcr:content");
         script.setProperty("exo:autoload", state);
         ses.save();
         return Response.status(Response.Status.NO_CONTENT).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Deploy groovy script as REST service. If this property set to 'true' then
    * script will be deployed as REST service if 'false' the script will be
    * undeployed. NOTE is script already deployed and <tt>state</tt> is
    * <tt>true</tt> script will be re-deployed.
    * 
    * @param repository repository name
    * @param workspace workspace name
    * @param path the path to JCR node that contains groovy script to be deployed
    */
   @POST
   @Path("load/{repository}/{workspace}/{path:.*}")
   public Response load(@PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @PathParam("path") String path, @DefaultValue("true") @QueryParam("state") boolean state)
   {
      Session ses = null;
      try
      {
         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));
         Node script = ((Node)ses.getItem("/" + path)).getNode("jcr:content");
         ScriptKey key = new NodeScriptKey(repository, workspace, script);
         if (state)
         {
            if (isLoaded(key))
               unloadScript(key);
            if (!loadScript(key, path, script.getProperty("jcr:data").getStream()))
            {
               String message =
                  "Can't bind script " + path
                     + ", it is not root resource or root resource with the same URI pattern already registered";
               return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
            }
         }
         else
         {
            if (!unloadScript(key))
            {
               String message =
                  "Can't unbind script " + path + ", not bound or has wrong mapping to the resource class ";
               return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
            }
         }

         return Response.status(Response.Status.NO_CONTENT).build();
      }
      catch (PathNotFoundException e)
      {
         LOG.error("Path " + path + " does not exists", e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Returns the list of all groovy-scripts found in workspace.
    * 
    * @param repository Repository name.
    * @param workspace Workspace name.
    * @param name Additional search parameter. If not emtpy method returns the
    *          list of script names matching wildcard else returns all the
    *          scripts found in workspace.
    * @return
    */
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @Path("list/{repository}/{workspace}")
   public Response list(@PathParam("repository") String repository, @PathParam("workspace") String workspace,
      @QueryParam("name") String name)
   {

      Session ses = null;
      try
      {

         ses =
            sessionProviderService.getSessionProvider(null).getSession(workspace,
               repositoryService.getRepository(repository));

         String xpath = "//element(*, exo:groovyResourceContainer)";

         Query query = ses.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
         QueryResult result = query.execute();
         NodeIterator nodeIterator = result.getNodes();

         ArrayList<String> scriptList = new ArrayList<String>();

         if (name == null || "".equals(name))
         {
            while (nodeIterator.hasNext())
            {
               Node node = nodeIterator.nextNode();
               scriptList.add(node.getParent().getPath());
            }
         }
         else
         {
            StringBuffer p = new StringBuffer();
            // add '.*' pattern at the start
            p.append(".*");
            for (int i = 0; i < name.length(); i++)
            {
               char c = name.charAt(i);
               if (c == '*' || c == '?')
                  p.append('.');
               if (".()[]^$|".indexOf(c) != -1)
                  p.append('\\');
               p.append(c);
            }
            // add '.*' pattern at he end
            p.append(".*");

            Pattern pattern = Pattern.compile(p.toString(), Pattern.CASE_INSENSITIVE);
            while (nodeIterator.hasNext())
            {
               Node node = nodeIterator.nextNode();
               String scriptName = node.getParent().getPath();

               if (pattern.matcher(scriptName).matches())
               {
                  scriptList.add(scriptName);
               }
            }
         }
         Collections.sort(scriptList);
         return Response.status(Response.Status.OK).entity(new ScriptList(scriptList)).type(MediaType.APPLICATION_JSON)
            .build();

      }
      catch (Exception e)
      {
         LOG.error("Unexpected error occurs ", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }
   }

   /**
    * Extract path to node's parent from full path.
    * 
    * @param fullPath full path to node
    * @return node's parent path
    */
   private static String getPath(String fullPath)
   {
      int sl = fullPath.lastIndexOf('/');
      return sl > 0 ? "/" + fullPath.substring(0, sl) : "/";
   }

   /**
    * Extract node's name from full node path.
    * 
    * @param fullPath full path to node
    * @return node's name
    */
   private static String getName(String fullPath)
   {
      int sl = fullPath.lastIndexOf('/');
      return sl >= 0 ? fullPath.substring(sl + 1) : fullPath;
   }

   /**
    * Script metadata, used for pass script metada as JSON.
    */
   public static class ScriptMetadata
   {

      /**
       * Is script autoload.
       */
      private final boolean autoload;

      /**
       * Is script loaded.
       */
      private final boolean load;

      /**
       * Script media type (script/groovy).
       */
      private final String mediaType;

      /**
       * Last modified date.
       */
      private final long lastModified;

      public ScriptMetadata(boolean autoload, boolean load, String mediaType, long lastModified)
      {
         this.autoload = autoload;
         this.load = load;
         this.mediaType = mediaType;
         this.lastModified = lastModified;
      }

      /**
       * @return {@link #autoload}
       */
      public boolean getAutoload()
      {
         return autoload;
      }

      /**
       * @return {@link #load}
       */
      public boolean getLoad()
      {
         return load;
      }

      /**
       * @return {@link #mediaType}
       */
      public String getMediaType()
      {
         return mediaType;
      }

      /**
       * @return {@link #lastModified}
       */
      public long getLastModified()
      {
         return lastModified;
      }
   }

   /**
    * Script list, used for pass script list as JSON.
    */
   public static class ScriptList
   {

      /**
       * The list of scripts.
       */
      private List<String> list;

      /**
       * Returns the list of scripts.
       * 
       * @return the list of scripts.
       */
      public List<String> getList()
      {
         return list;
      }

      /**
       * ScriptList constructor.
       * 
       * @param the list of scripts
       */
      public ScriptList(List<String> scriptList)
      {
         this.list = scriptList;
      }

   }
}
