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
package org.exoplatform.services.jcr.ext.repository;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * Created by The eXo Platform SAS.
 *
 * RestRepositoryService is RESTfull service provides the access to repository.
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: NamesList.java 111 2008-11-11 11:11:11Z rainf0x $
 */

@Path("/jcr-service")
public class RestRepositoryService implements ResourceContainer
{

   /**
    * Definition the constants.
    */
   public static final class Constants
   {

      /**
       * The base path to this service.
       */
      public static final String BASE_URL = "/jcr-service";

      /**
       * Definition the operation types.
       */
      public static final class OperationType
      {
         /**
          * Repository service configuration operation.
          */
         public static final String REPOSITORY_SERVICE_CONFIGURATION = "/repository-service-configuration";

         /**
          * Default workspace configuration operations.
          */
         public static final String DEFAULT_WS_CONFIG = "/default-ws-config";

         /**
          * Create new repository operations.
          */
         public static final String CREATE_REPOSITORY = "/create-repository";

         /**
          * Create new workspace.
          */
         public static final String CREATE_WORKSPACE = "/create-workspace";

         /**
          * Remove repository operation.
          */
         public static final String REMOVE_REPOSITORY = "/remove-repository";

         /**
          * Remove workspace operation.
          */
         public static final String REMOVE_WORKSPACE = "/remove-workspace";

         /**
          * The list of repositories name operations.
          */
         public static final String REPOSITORIES_LIST = "/repositories";

         /**
          * The list of workspaces name operation.
          */
         public static final String WORKSPACES_LIST = "/workspaces";

         /**
          * Update workspace configuration operation.
          */
         public static final String UPDATE_WORKSPACE_CONFIG = "/update-workspace-config";

         /**
          * OperationType private constructor.
          */
         private OperationType()
         {
         }
      }

      /**
       * Constants private constructor.
       */
      private Constants()
      {
      }
   }

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.ext.RestRepositoryService");

   /**
    * 
    */
   private final RepositoryService repositoryService;

   /**
    * To disable cache control.
    */
   private static final CacheControl NO_CACHE = new CacheControl();

   static
   {
      // noCache = new CacheControl();
      NO_CACHE.setNoCache(true);
      NO_CACHE.setNoStore(true);
   }

   /**
    * @param repositoryService
    */
   public RestRepositoryService(RepositoryService repositoryService)
   {
      this.repositoryService = repositoryService;
   }

   /**
    * Gives the repository service configuration which is composed
    * of the configuration of all the repositories and workspaces
    * @return Response
    *           return the Response with RepositoryServiceConf
    * @LevelAPI Experimental
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/repository-service-configuration")
   public Response getRepositoryServiceConfiguration()
   {
      RepositoryServiceConfiguration configuration = repositoryService.getConfig();
      RepositoryServiceConf conf =
         new RepositoryServiceConf(configuration.getRepositoryConfigurations(), configuration
            .getDefaultRepositoryName());
      return Response.ok(conf, MediaType.APPLICATION_JSON_TYPE).cacheControl(NO_CACHE).build();
   }

   /**
    * Gives the configuration of the default workspace of the given repository
    * @param repositoryName the name of the repository
    * @return Response
    *           return the Response with WorkspaceEntry
    * @LevelAPI Experimental
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/default-ws-config/{repositoryName}")
   public Response getDefaultWorkspaceConfig(@PathParam("repositoryName") String repositoryName)
   {
      String errorMessage = new String();
      Status status;

      try
      {
         String defaultWorkspaceName =
            repositoryService.getRepository(repositoryName).getConfiguration().getDefaultWorkspaceName();

         for (WorkspaceEntry wEntry : repositoryService.getRepository(repositoryName).getConfiguration()
            .getWorkspaceEntries())
         {
            if (defaultWorkspaceName.equals(wEntry.getName()))
            {
               return Response.ok(wEntry).cacheControl(NO_CACHE).build();
            }
         }

         return Response.status(Response.Status.NOT_FOUND).entity("Can not get default workspace configuration.").type(
            MediaType.TEXT_PLAIN).cacheControl(NO_CACHE).build();
      }
      catch (RepositoryException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.NOT_FOUND;
      }
      catch (Throwable e) //NOSONAR
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.INTERNAL_SERVER_ERROR;
      }

      return Response.status(status).entity(errorMessage).type(MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE)
         .build();
   }

   /**
    * Creates a repository based on the given repository configuration
    * 
    * @param uriInfo the uri location 
    * @param newRepository the configuration of the new repository
    * @return Response return the Response
    * @throws URISyntaxException will be generated the URISyntaxException  
    * @LevelAPI Experimental
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/create-repository")
   public Response createRepository(@Context UriInfo uriInfo, RepositoryEntry newRepository) throws URISyntaxException
   {
      String errorMessage = new String();
      Status status;
      try
      {
         repositoryService.createRepository(newRepository);
         repositoryService.getConfig().retain(); // save configuration to persistence (file or persister)
         return Response.ok().cacheControl(NO_CACHE).build();
      }
      catch (RepositoryException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.BAD_REQUEST;
      }
      catch (RepositoryConfigurationException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.BAD_REQUEST;
      }
      catch (Throwable e) //NOSONAR
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.INTERNAL_SERVER_ERROR;
      }

      return Response.status(status).entity(errorMessage).type(MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE)
         .build();
   }

   /**
    * Adds a new workspace based on the given configuration to the given repository.
    * 
    * @param uriInfo the uri location
    * @param repositoryName the name of the repository
    * @param newWorkspace the configuration of the new workspace
    * @return Response return the Response
    * @throws URISyntaxException will be generated the URISyntaxException
    * @LevelAPI Experimental
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/create-workspace/{repositoryName}")
   public Response createWorkspace(@Context UriInfo uriInfo, @PathParam("repositoryName") String repositoryName,
      WorkspaceEntry newWorkspace) throws URISyntaxException
   {
      String errorMessage = new String();
      Status status;
      try
      {
         RepositoryImpl repository = (RepositoryImpl)repositoryService.getRepository(repositoryName);
         repository.configWorkspace(newWorkspace);
         repository.createWorkspace(newWorkspace.getName());
         repositoryService.getConfig().retain(); // save configuration to persistence (file or persister)
         return Response.ok().cacheControl(NO_CACHE).build();
      }
      catch (RepositoryException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.NOT_FOUND;
      }
      catch (RepositoryConfigurationException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.BAD_REQUEST;
      }
      catch (Throwable e) //NOSONAR
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.INTERNAL_SERVER_ERROR;
      }
      return Response.status(status).entity(errorMessage).type(MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE)
         .build();
   }

   /**
    * Removes an existing repository
    * 
    * @param uriInfo the uri location
    * @param repositoryName the name of the repository
    * @param forseSessionClose a flag indicating whether or not the current sessions must be closed
    * @return Response return the Response
    * @LevelAPI Experimental
    */
   @GET
   @RolesAllowed("administrators")
   @Path("/remove-repository/{repositoryName}/{forseSessionClose}")
   public Response removeRepository(@Context UriInfo uriInfo, @PathParam("repositoryName") String repositoryName,
      @PathParam("forseSessionClose") Boolean forseSessionClose)
   {
      String errorMessage = new String();
      Status status;

      try
      {
         if (forseSessionClose)
         {
            for (WorkspaceEntry wsEntry : repositoryService.getConfig().getRepositoryConfiguration(repositoryName)
               .getWorkspaceEntries())
            {
               forceCloseSession(repositoryName, wsEntry.getName());
            }
         }

         if (repositoryService.canRemoveRepository(repositoryName))
         {
            repositoryService.removeRepository(repositoryName);
            repositoryService.getConfig().retain(); // save configuration to persistence (file or persister) 
            return Response.ok().build();
         }
         return Response.status(Status.CONFLICT).entity("Can't remove repository " + repositoryName)
            .cacheControl(NO_CACHE).build();
      }
      catch (RepositoryException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.NOT_FOUND;
      }
      catch (Throwable e) //NOSONAR
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.INTERNAL_SERVER_ERROR;
      }

      return Response.status(status).entity(errorMessage).type(MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE)
         .build();
   }

   /**
    * Removes an existing workspace from a given repository. 
    * 
    * @param uriInfo the uri location
    * @param repositoryName the name of the repository
    * @param workspaceName the name of the workspace
    * @param forseSessionClose a flag indicating whether or not the current sessions must be closed
    * @return Response return the Response
    * @LevelAPI Experimental
    */
   @POST
   @RolesAllowed("administrators")
   @Path("/remove-workspace/{repositoryName}/{workspaceName}/{forseSessionClose}/")
   public Response removeWorkspace(@Context UriInfo uriInfo, @PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("forseSessionClose") Boolean forseSessionClose)
   {
      String errorMessage = new String();
      Status status;

      try
      {
         ManageableRepository repository = repositoryService.getRepository(repositoryName);

         if (forseSessionClose)
         {
            forceCloseSession(repositoryName, workspaceName);
         }

         if (repository.canRemoveWorkspace(workspaceName))
         {
            repository.removeWorkspace(workspaceName);
            repositoryService.getConfig().retain(); // save configuration to persistence (file or persister)
            return Response.ok().build();
         }
         return Response.status(Status.CONFLICT).entity(
            "Can't remove workspace " + workspaceName + " in repository " + repositoryName).cacheControl(NO_CACHE)
            .build();
      }
      catch (RepositoryException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.NOT_FOUND;
      }
      catch (RepositoryConfigurationException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.NOT_FOUND;
      }
      catch (Throwable e) //NOSONAR
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.INTERNAL_SERVER_ERROR;
      }

      return Response.status(status).entity(errorMessage).type(MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE)
         .build();
   }

   /**
    * Gives the name of all the existing repositories.
    * 
    * @return Response return the Response with list of repository names
    * @LevelAPI Experimental
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/repositories")
   public Response getRepositoryNames()
   {
      List<String> repositories = new ArrayList<String>();

      for (RepositoryEntry rEntry : repositoryService.getConfig().getRepositoryConfigurations())
      {
         repositories.add(rEntry.getName());
      }

      return Response.ok(new NamesList(repositories)).cacheControl(NO_CACHE).build();
   }

   /**
    * Gives the name of all the existing workspaces for a given repository.
    * 
    * @param repositoryName the name of the repository
    * @return Response return the Response with list of workspace names
    * @LevelAPI Experimental
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/workspaces/{repositoryName}")
   public Response getWorkspaceNames(@PathParam("repositoryName") String repositoryName)
   {
      String errorMessage = new String();
      Status status;

      try
      {
         List<String> workspaces = new ArrayList<String>();

         for (WorkspaceEntry wEntry : repositoryService.getRepository(repositoryName).getConfiguration()
            .getWorkspaceEntries())
         {
            workspaces.add(wEntry.getName());
         }

         return Response.ok(new NamesList(workspaces)).cacheControl(NO_CACHE).build();
      }
      catch (RepositoryException e)
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.NOT_FOUND;
      }
      catch (Throwable e) //NOSONAR
      {
         if (log.isDebugEnabled())
         {
            log.error(e.getMessage(), e);
         }
         errorMessage = e.getMessage();
         status = Status.INTERNAL_SERVER_ERROR;
      }

      return Response.status(status).entity(errorMessage).type(MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE)
         .build();
   }

   /**
    * Updates the configuration of a given workspace.
    * 
    * @param repositoryName the name of the repository
    * @param workspaceName the name of the workspace
    * @param workspaceEntry the configuration of the workspace 
    * @return Response return the Response
    * @LevelAPI Unsupported
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/update-workspace-config/{repositoryName}/{workspaceName}")
   public Response updateWorkspaceConfiguration(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, WorkspaceEntry workspaceEntry)
   {
      return Response.status(Status.OK).entity("The method /update-workspace-config not implemented.").type(
         MediaType.TEXT_PLAIN_TYPE).cacheControl(NO_CACHE).build();
   }

   /**
    * forceCloseSession. Close sessions on specific workspace.
    * 
    * @param repositoryName
    *          repository name
    * @param workspaceName
    *          workspace name
    * @return int return the how many sessions was closed
    * @throws RepositoryConfigurationException
    *           will be generate RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generate RepositoryException
    */
   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }
}
