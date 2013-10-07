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
package org.exoplatform.services.jcr.webdav;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.webdav.command.AclCommand;
import org.exoplatform.services.jcr.webdav.command.CopyCommand;
import org.exoplatform.services.jcr.webdav.command.DeleteCommand;
import org.exoplatform.services.jcr.webdav.command.GetCommand;
import org.exoplatform.services.jcr.webdav.command.HeadCommand;
import org.exoplatform.services.jcr.webdav.command.LockCommand;
import org.exoplatform.services.jcr.webdav.command.MkColCommand;
import org.exoplatform.services.jcr.webdav.command.MoveCommand;
import org.exoplatform.services.jcr.webdav.command.OrderPatchCommand;
import org.exoplatform.services.jcr.webdav.command.PropFindCommand;
import org.exoplatform.services.jcr.webdav.command.PropPatchCommand;
import org.exoplatform.services.jcr.webdav.command.PutCommand;
import org.exoplatform.services.jcr.webdav.command.SearchCommand;
import org.exoplatform.services.jcr.webdav.command.UnLockCommand;
import org.exoplatform.services.jcr.webdav.command.deltav.CheckInCommand;
import org.exoplatform.services.jcr.webdav.command.deltav.CheckOutCommand;
import org.exoplatform.services.jcr.webdav.command.deltav.ReportCommand;
import org.exoplatform.services.jcr.webdav.command.deltav.UnCheckOutCommand;
import org.exoplatform.services.jcr.webdav.command.deltav.VersionControlCommand;
import org.exoplatform.services.jcr.webdav.lock.NullResourceLocksHolder;
import org.exoplatform.services.jcr.webdav.util.InitParamsDefaults;
import org.exoplatform.services.jcr.webdav.util.NodeTypeUtil;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.ext.webdav.method.ACL;
import org.exoplatform.services.rest.ext.webdav.method.CHECKIN;
import org.exoplatform.services.rest.ext.webdav.method.CHECKOUT;
import org.exoplatform.services.rest.ext.webdav.method.COPY;
import org.exoplatform.services.rest.ext.webdav.method.LOCK;
import org.exoplatform.services.rest.ext.webdav.method.MKCOL;
import org.exoplatform.services.rest.ext.webdav.method.MOVE;
import org.exoplatform.services.rest.ext.webdav.method.OPTIONS;
import org.exoplatform.services.rest.ext.webdav.method.ORDERPATCH;
import org.exoplatform.services.rest.ext.webdav.method.PROPFIND;
import org.exoplatform.services.rest.ext.webdav.method.PROPPATCH;
import org.exoplatform.services.rest.ext.webdav.method.REPORT;
import org.exoplatform.services.rest.ext.webdav.method.SEARCH;
import org.exoplatform.services.rest.ext.webdav.method.UNCHECKOUT;
import org.exoplatform.services.rest.ext.webdav.method.UNLOCK;
import org.exoplatform.services.rest.ext.webdav.method.VERSIONCONTROL;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created by The eXo Platform SAS.
 * WebDavServiceImpl is the WebDav connector on top of eXo JCR.
 * It allows to execute CRUD operations on the JCR thanks to the Webdav protocol.
 *
 * @author Gennady Azarenkov
 */

@Path("/jcr")
public class WebDavServiceImpl implements WebDavService, ResourceContainer
{
   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.WebDavServiceImpl");
   /**
    * Local Thread SessionProvider.
    */
   private final SessionProviderService sessionProviderService;

   /**
    * Repository service.
    */
   private final RepositoryService repositoryService;

   /**
    * NullResourceLocksHolder.
    */
   private final NullResourceLocksHolder nullResourceLocks;

   /**
    * Encapsulates WebDAV service initial parameters.
    */
   private WebDavServiceInitParams webDavServiceInitParams;

   /**
    * The list of allowed methods.
    */
   private static final String ALLOW;

   private final MimeTypeResolver mimeTypeResolver;

   static
   {
      StringBuffer sb = new StringBuffer();
      for (Method m : WebDavServiceImpl.class.getMethods())
      {
         for (Annotation a : m.getAnnotations())
         {
            javax.ws.rs.HttpMethod ma = null;
            if ((ma = a.annotationType().getAnnotation(javax.ws.rs.HttpMethod.class)) != null)
            {
               if (sb.length() > 0)
                  sb.append(", ");
               sb.append(ma.value());
            }
         }
      }

      ALLOW = sb.toString();

   }

   /**
    * Constructor.
    * 
    * @param params Initialization parameters
    * @param repositoryService repository service
    * @param sessionProviderService session provider service
    * @throws Exception {@link Exception}
    */
   public WebDavServiceImpl(InitParams params, RepositoryService repositoryService,
      SessionProviderService sessionProviderService) throws Exception
   {
      this(repositoryService, sessionProviderService);
      this.webDavServiceInitParams = new WebDavServiceInitParams(params);
   }

   /**
    * Constructor.
    * 
    * @param params Initialization params
    * @param repositoryService repository service
    * @param sessionProviderService session provider service
    */
   protected WebDavServiceImpl(Map<String, String> params, RepositoryService repositoryService,
      SessionProviderService sessionProviderService) throws Exception
   {
      this(repositoryService, sessionProviderService);
      this.webDavServiceInitParams = new WebDavServiceInitParams(params);
   }

   /**
    * Constructor.
    * 
    * @param repositoryService repository service
    * @param sessionProviderService session provider service
    */
   protected WebDavServiceImpl(RepositoryService repositoryService,
      SessionProviderService sessionProviderService)
   {
      this.sessionProviderService = sessionProviderService;
      this.repositoryService = repositoryService;
      this.nullResourceLocks = new NullResourceLocksHolder();
      this.mimeTypeResolver = new MimeTypeResolver();
      this.mimeTypeResolver.setDefaultMimeType(InitParamsDefaults.FILE_MIME_TYPE);
      this.webDavServiceInitParams = new WebDavServiceInitParams();
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @CHECKIN
   @Path("/{repoName}/{repoPath:.*}/")
   public Response checkin(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader)
   {

      if (log.isDebugEnabled())
      {
         log.debug("CHECKIN " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      Session session;
      try
      {
         session = session(repoName, workspaceName(repoPath), lockTokens(lockTokenHeader, ifHeader));
         return new CheckInCommand().checkIn(session, path(repoPath));
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @CHECKOUT
   @Path("/{repoName}/{repoPath:.*}/")
   public Response checkout(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader)
   {

      if (log.isDebugEnabled())
      {
         log.debug("CHECKOUT " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      Session session;
      try
      {
         session = session(repoName, workspaceName(repoPath), lockTokens(lockTokenHeader, ifHeader));
         return new CheckOutCommand().checkout(session, path(repoPath));
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param destinationHeader Destination HTTP Header
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param depthHeader Depth HTTP header
    * @param overwriteHeader Overwrite HTTP header
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @COPY
   @Path("/{repoName}/{repoPath:.*}/")
   public Response copy(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.DESTINATION) String destinationHeader,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
      @HeaderParam(ExtHttpHeaders.OVERWRITE) String overwriteHeader, @Context UriInfo uriInfo, HierarchicalProperty body)
   {
      // to trace if an item on destination path exists
      boolean itemExisted = false;

      if (log.isDebugEnabled())
      {
         log.debug("COPY " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         String serverURI = uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).build().toString();

         // destinationHeader could begins from workspace name (passed from cms 
         // WebDAVServiceImpl) and doesn't contain neither host no repository name 
         URI dest = buildURI(destinationHeader);
         URI base = buildURI(serverURI);

         String destPath = dest.getPath();
         int repoIndex = destPath.indexOf(repoName);

         // check if destination corresponds to base uri
         // if the destination is on another server
         // or destination header is malformed
         // we return BAD_GATEWAY(502) HTTP status
         // more info here http://www.webdav.org/specs/rfc2518.html#METHOD_COPY
         if (dest.getHost() != null && !base.getHost().equals(dest.getHost()))
         {
            return Response.status(HTTPStatus.BAD_GATEWAY).entity("Bad Gateway").build();
         }

         destPath = normalizePath(repoIndex == -1 ? destPath : destPath.substring(repoIndex + repoName.length() + 1));

         String srcWorkspace = workspaceName(repoPath);
         String srcNodePath = path(repoPath);

         String destWorkspace = workspaceName(destPath);
         String destNodePath = path(destPath, false);

         List<String> lockTokens = lockTokens(lockTokenHeader, ifHeader);

         Depth depth = new Depth(depthHeader);

         boolean overwrite = overwriteHeader != null && overwriteHeader.equalsIgnoreCase("T");
         repoName = getRepositoryName(repoName);

         if (overwrite)
         {
            Response delResponse = delete(repoName, destPath, lockTokenHeader, ifHeader);
            itemExisted = (delResponse.getStatus() == HTTPStatus.NO_CONTENT);
         }
         else
         {
            Session session = session(repoName, srcWorkspace, null);

            if (session.getRootNode().hasNode(TextUtil.relativizePath(path(destPath))))
            {
               return Response.status(HTTPStatus.PRECON_FAILED)
                  .entity("Item exists on destination path, while overwriting is forbidden").build();
            }
         }

         if (depth.getStringValue().equalsIgnoreCase("infinity"))
         {

            if (srcWorkspace.equals(destWorkspace))
            {
               Session session = session(repoName, destWorkspace, lockTokens);
               return new CopyCommand(uriInfo.getBaseUriBuilder().path(getClass()).path(repoName), itemExisted).copy(
                  session, srcNodePath, destNodePath);
            }

            Session destSession = session(repoName, destWorkspace, lockTokens);
            return new CopyCommand(uriInfo.getBaseUriBuilder().path(getClass()).path(repoName), itemExisted).copy(
               destSession, srcWorkspace, srcNodePath, destNodePath);

         }
         else if (depth.getIntValue() == 0)
         {

            int nodeNameStart = srcNodePath.lastIndexOf('/') + 1;
            String nodeName = srcNodePath.substring(nodeNameStart);
            int indexStart = nodeName.indexOf('[');
            if (indexStart != -1)
            {
               nodeName = nodeName.substring(0, indexStart);
            }

            Session session = session(repoName, destWorkspace, lockTokens);

            return new MkColCommand(nullResourceLocks, uriInfo.getBaseUriBuilder().path(getClass()).path(repoName))
               .mkCol(session, destNodePath + "/" + nodeName, webDavServiceInitParams.getDefaultFolderNodeType(), null,
                  lockTokens);

         }
         else
         {
            return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad Request").build();
         }

      }
      catch (PreconditionException exc)
      {
         return Response.status(HTTPStatus.BAD_REQUEST).entity(exc.getMessage()).build();
      }
      catch (NoSuchWorkspaceException e)
      {
         log.error("NoSuchWorkspaceException " + e.getMessage(), e);
         return Response.status(HTTPStatus.CONFLICT).entity(e.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If HTTP Header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @DELETE
   @Path("/{repoName}/{repoPath:.*}/")
   public Response delete(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader)
   {

      if (log.isDebugEnabled())
      {
         log.debug("DELETE " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         Session session = session(repoName, workspaceName(repoPath), lockTokens(lockTokenHeader, ifHeader));
         if (lockTokenHeader != null)
         {
            lockTokenHeader = lockTokenHeader.substring(1, lockTokenHeader.length() - 1);
            if (lockTokenHeader.contains(WebDavConst.Lock.OPAQUE_LOCK_TOKEN))
            {
               lockTokenHeader = lockTokenHeader.split(":")[1];
            }
         }
         return new DeleteCommand().delete(session, path(repoPath), lockTokenHeader);
      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * WedDAV "GET" method. See <a href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP
    * methods for distributed authoring sec. 8.4 "GET, HEAD for Collections"</a>.
    * 
    * @param repoName repository name
    * @param repoPath path in repository
    * @param rangeHeader Range HTTP header
    * @param version version name
    * @param ifModifiedSince if-modified-since header
    * @param ifNoneMatch if-none-match header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @GET
   @Path("/{repoName}/{repoPath:.*}/")
   public Response get(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.RANGE) String rangeHeader,
      @HeaderParam(ExtHttpHeaders.IF_MODIFIED_SINCE) String ifModifiedSince,
      @HeaderParam(ExtHttpHeaders.IF_NONE_MATCH) String ifNoneMatch, @QueryParam("version") String version,
      @Context UriInfo uriInfo)
   {
      if (log.isDebugEnabled())
      {
         log.debug("GET " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         Session session = session(repoName, workspaceName(repoPath), null);

         ArrayList<Range> ranges = new ArrayList<Range>();

         if (rangeHeader != null)
         {
            if (log.isDebugEnabled())
            {
               log.debug(rangeHeader);
            }

            if (rangeHeader.startsWith("bytes="))
            {
               String rangeString = rangeHeader.substring(rangeHeader.indexOf("=") + 1);

               String[] tokens = rangeString.split(",");
               for (String token : tokens)
               {
                  Range range = new Range();
                  token = token.trim();
                  int dash = token.indexOf("-");
                  if (dash == -1)
                  {
                     return Response.status(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .entity("Requested Range Not Satisfiable").build();
                  }
                  else if (dash == 0)
                  {
                     range.setStart(Long.parseLong(token));
                     range.setEnd(-1L);
                  }
                  else if (dash > 0)
                  {
                     range.setStart(Long.parseLong(token.substring(0, dash)));
                     if (dash < token.length() - 1)
                        range.setEnd(Long.parseLong(token.substring(dash + 1, token.length())));
                     else
                        range.setEnd(-1L);
                  }
                  ranges.add(range);
               }
            }
         }
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         return new GetCommand(webDavServiceInitParams.getXsltParams()).get(session, path(repoPath), version, uri,
            ranges, ifModifiedSince, ifNoneMatch, webDavServiceInitParams.getCacheControlMap());

      }
      catch (PathNotFoundException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * WedDAV "HEAD" method. see <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP methods for distributed
    * authoring sec. 8.4 "GET, HEAD for Collections"</a>.
    * 
    * @param repoName repository name
    * @param repoPath path in repository
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @HEAD
   @Path("/{repoName}/{repoPath:.*}/")
   public Response head(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @Context UriInfo uriInfo)
   {

      if (log.isDebugEnabled())
      {
         log.debug("HEAD " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         Session session = session(repoName, workspaceName(repoPath), null);
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         return new HeadCommand().head(session, path(repoPath), uri);
      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param depthHeader Depth HTTP header
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @LOCK
   @Path("/{repoName}/{repoPath:.*}/")
   public Response lock(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader, HierarchicalProperty body)
   {

      if (log.isDebugEnabled())
      {
         log.debug("LOCK " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         Session session = session(repoName, workspaceName(repoPath), lockTokens(lockTokenHeader, ifHeader));
         return new LockCommand(nullResourceLocks).lock(session, path(repoPath), body, new Depth(depthHeader), "86400");

      }
      catch (PreconditionException exc)
      {
         log.error("PreconditionException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.PRECON_FAILED).entity(exc.getMessage()).build();

      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();

      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @UNLOCK
   @Path("/{repoName}/{repoPath:.*}/")
   public Response unlock(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader)
   {

      if (log.isDebugEnabled())
      {
         log.debug("UNLOCK " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      Session session;
      List<String> tokens = lockTokens(lockTokenHeader, ifHeader);
      try
      {
         session = session(repoName, workspaceName(repoPath), tokens);
         return new UnLockCommand(nullResourceLocks).unLock(session, path(repoPath), tokens);

      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param folderNodeTypeHeader JCR Node-Type header
    * @param mixinTypesHeader JCR Mixin-Types header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @MKCOL
   @Path("/{repoName}/{repoPath:.*}/")
   public Response mkcol(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @HeaderParam(ExtHttpHeaders.FOLDER_NODETYPE) String folderNodeTypeHeader,
      @HeaderParam(ExtHttpHeaders.CONTENT_MIXINTYPES) String mixinTypesHeader, @Context UriInfo uriInfo)
   {
      if (log.isDebugEnabled())
      {
         log.debug("MKCOL " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         List<String> tokens = lockTokens(lockTokenHeader, ifHeader);
         Session session = session(repoName, workspaceName(repoPath), tokens);
         String folderNodeType =
            NodeTypeUtil.getNodeType(folderNodeTypeHeader, webDavServiceInitParams.getDefaultFolderNodeType(),
               webDavServiceInitParams.getAllowedFolderNodeTypes());

         return new MkColCommand(nullResourceLocks, uriInfo.getBaseUriBuilder().path(getClass()).path(repoName)).mkCol(
            session, path(repoPath, false), folderNodeType, NodeTypeUtil.getMixinTypes(mixinTypesHeader), tokens);
      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();
      }
      catch (NoSuchNodeTypeException exc)
      {
         log.error("NoSuchNodeTypeException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.UNSUPPORTED_TYPE).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param destinationHeader Destination HTTP header
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param depthHeader Depth HTTP header
    * @param overwriteHeader Overwrite HTTP header
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @MOVE
   @Path("/{repoName}/{repoPath:.*}/")
   public Response move(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.DESTINATION) String destinationHeader,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
      @HeaderParam(ExtHttpHeaders.OVERWRITE) String overwriteHeader, @Context UriInfo uriInfo, HierarchicalProperty body)
   {
      // to trace if an item on destination path exists
      boolean itemExisted = false;

      if (log.isDebugEnabled())
      {
         log.debug("MOVE " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         String serverURI = uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).build().toString();

         // destinationHeader could begins from workspace name (passed from cms
         // WebDAVServiceImpl) and doesn't contain neither host no repository name
         URI dest = buildURI(destinationHeader);
         URI base = buildURI(serverURI);

         String destPath = dest.getPath();
         int repoIndex = destPath.indexOf(repoName);

         // check if destination corresponds to base uri
         // if the destination is on another server
         // or destination header is malformed
         // we return BAD_GATEWAY(502) HTTP status
         // more info here http://www.webdav.org/specs/rfc2518.html#METHOD_MOVE
         if (dest.getHost() != null && !base.getHost().equals(dest.getHost()))
         {
            return Response.status(HTTPStatus.BAD_GATEWAY).entity("Bad Gateway").build();
         }

         destPath = normalizePath(repoIndex == -1 ? destPath : destPath.substring(repoIndex + repoName.length() + 1));

         String destWorkspace = workspaceName(destPath);
         String destNodePath = path(destPath, false);

         String srcWorkspace = workspaceName(repoPath);
         String srcNodePath = path(repoPath);

         List<String> lockTokens = lockTokens(lockTokenHeader, ifHeader);

         Depth depth = new Depth(depthHeader);

         boolean overwrite = overwriteHeader != null && overwriteHeader.equalsIgnoreCase("T");
         repoName = getRepositoryName(repoName);

         if (overwrite)
         {
            Response delResponse = delete(repoName, destPath, lockTokenHeader, ifHeader);
            itemExisted = (delResponse.getStatus() == HTTPStatus.NO_CONTENT);
         }
         else
         {
            Session session = session(repoName, srcWorkspace, null);
            String uri =
               uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(srcWorkspace).build().toString();
            Response prpfind = new PropFindCommand().propfind(session, path(destPath), body, depth.getIntValue(), uri);
            if (prpfind.getStatus() != HTTPStatus.NOT_FOUND)
            {
               return Response.status(HTTPStatus.PRECON_FAILED)
                  .entity("Item exists on destination path, while overwriting is forbidden").build();
            }
         }

         if (depth.getStringValue().equalsIgnoreCase("Infinity"))
         {
            if (srcWorkspace.equals(destWorkspace))
            {
               Session session = session(repoName, srcWorkspace, lockTokens);
               return new MoveCommand(uriInfo.getBaseUriBuilder().path(getClass()).path(repoName), itemExisted).move(
                  session, srcNodePath, destNodePath);
            }

            Session srcSession = session(repoName, srcWorkspace, lockTokens);
            Session destSession = session(repoName, destWorkspace, lockTokens);
            return new MoveCommand(uriInfo.getBaseUriBuilder().path(getClass()).path(repoName), itemExisted).move(
               srcSession, destSession, srcNodePath, destNodePath);
         }
         else
         {
            return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad Request").build();
         }

      }
      catch (NoSuchWorkspaceException e)
      {
         log.error("NoSuchWorkspaceException " + e.getMessage(), e);
         return Response.status(HTTPStatus.CONFLICT).entity(e.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }

   }

   /**
    * @param repoName repository name
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @OPTIONS
   @Path("/{repoName}/{path:.*}/")
   public Response options(@PathParam("path") String path)
   {

      if (log.isDebugEnabled())
      {
         log.debug("OPTIONS " + path);
      }

      String DASL_VALUE =
         "<DAV:basicsearch>" + "<exo:sql xmlns:exo=\"http://exoplatform.com/jcr\"/>"
            + "<exo:xpath xmlns:exo=\"http://exoplatform.com/jcr\"/>";

      return Response.ok().header(ExtHttpHeaders.ALLOW, /* allowCommands */ALLOW)
         .header(ExtHttpHeaders.DAV, "1, 2, ordered-collections, access-control")
         .header(ExtHttpHeaders.DASL, DASL_VALUE).header(ExtHttpHeaders.MSAUTHORVIA, "DAV").build();
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @ORDERPATCH
   @Path("/{repoName}/{repoPath:.*}/")
   public Response order(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @Context UriInfo uriInfo, HierarchicalProperty body)
   {

      if (log.isDebugEnabled())
      {
         log.debug("ORDERPATCH " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         List<String> lockTokens = lockTokens(lockTokenHeader, ifHeader);
         Session session = session(repoName, workspaceName(repoPath), lockTokens);
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         return new OrderPatchCommand().orderPatch(session, path(repoPath), body, uri);
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param depthHeader Depth HTTP header
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response HTTP response
    * @LevelAPI Platform
    */
   @PROPFIND
   @Path("/{repoName}/{repoPath:.*}/")
   public Response propfind(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader, @Context UriInfo uriInfo, HierarchicalProperty body)
   {
      if (log.isDebugEnabled())
      {
         log.debug("PROPFIND " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         Session session = session(repoName, workspaceName(repoPath), null);
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         Depth depth = new Depth(depthHeader);
         return new PropFindCommand().propfind(session, path(repoPath), body, depth.getIntValue(), uri);
      }
      catch (NoSuchWorkspaceException exc)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();
      }
      catch (PreconditionException exc)
      {
         return Response.status(HTTPStatus.BAD_REQUEST).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response HTTP response
    * @LevelAPI Platform
    */
   @PROPPATCH
   @Path("/{repoName}/{repoPath:.*}/")
   public Response proppatch(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @Context UriInfo uriInfo, HierarchicalProperty body)
   {
      if (log.isDebugEnabled())
      {
         log.debug("PROPPATCH " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         List<String> lockTokens = lockTokens(lockTokenHeader, ifHeader);
         Session session = session(repoName, workspaceName(repoPath), lockTokens);
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         return new PropPatchCommand(nullResourceLocks).propPatch(session, path(repoPath), body, lockTokens, uri);
      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspace. " + exc.getMessage());
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * WedDAV "PUT" method. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP methods for distributed
    * authoring sec. 8.7 "PUT"</a>.
    * 
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If HTTP Header
    * @param fileNodeTypeHeader JCR NodeType header
    * @param contentNodeTypeHeader JCR Content-NodeType header
    * @param mixinTypes JCR Mixin types header
    * @param mediaType Content-Type HTTP header
    * @param userAgent User-Agent HTTP header
    * @param inputStream stream that contain incoming data
    * @param uriInfo URI info
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @PUT
   @Path("/{repoName}/{repoPath:.*}/")
   public Response put(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      @HeaderParam(ExtHttpHeaders.FILE_NODETYPE) String fileNodeTypeHeader,
      @HeaderParam(ExtHttpHeaders.CONTENT_NODETYPE) String contentNodeTypeHeader,
      @HeaderParam(ExtHttpHeaders.CONTENT_MIXINTYPES) String mixinTypes,
      @HeaderParam(ExtHttpHeaders.CONTENT_TYPE) MediaType mediaType,
      @HeaderParam(ExtHttpHeaders.USER_AGENT) String userAgent, InputStream inputStream, @Context UriInfo uriInfo)
   {
      if (log.isDebugEnabled())
      {
         log.debug("PUT " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);
      MimeTypeRecognizer mimeTypeRecognizer =
         new MimeTypeRecognizer(TextUtil.nameOnly(repoPath), mimeTypeResolver, mediaType, webDavServiceInitParams
            .getUntrustedUserAgents().contains(userAgent));

      try
      {
         repoName = getRepositoryName(repoName);
         List<String> tokens = lockTokens(lockTokenHeader, ifHeader);
         Session session = session(repoName, workspaceName(repoPath), tokens);

         String fileNodeType =
            NodeTypeUtil.getNodeType(fileNodeTypeHeader, webDavServiceInitParams.getDefaultFileNodeType(),
               webDavServiceInitParams.getAllowedFileNodeTypes());

         String contentNodeType = NodeTypeUtil.getContentNodeType(contentNodeTypeHeader);
         NodeTypeManager ntm = session.getWorkspace().getNodeTypeManager();
         NodeType nodeType = ntm.getNodeType(contentNodeType);
         NodeTypeUtil.checkContentResourceType(nodeType);

         return new PutCommand(nullResourceLocks, uriInfo.getBaseUriBuilder().path(getClass()).path(repoName),
            mimeTypeRecognizer).put(session, path(repoPath), inputStream, fileNodeType, contentNodeType,
            NodeTypeUtil.getMixinTypes(mixinTypes), webDavServiceInitParams.getDefaultUpdatePolicyType(),
            webDavServiceInitParams.getDefaultAutoVersionType(), tokens);

      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();

      }
      catch (NoSuchNodeTypeException exc)
      {
         log.error("NoSuchNodeTypeException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.BAD_REQUEST).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param depthHeader Depth HTTP header
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @REPORT
   @Path("/{repoName}/{repoPath:.*}/")
   public Response report(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader, @Context UriInfo uriInfo, HierarchicalProperty body)
   {

      if (log.isDebugEnabled())
      {
         log.debug("REPORT " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         Depth depth = new Depth(depthHeader);
         Session session = session(repoName, workspaceName(repoPath), null);
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         return new ReportCommand().report(session, path(repoPath), body, depth, uri);
      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param uriInfo base URI info
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @SEARCH
   @Path("/{repoName}/{repoPath:.*}/")
   public Response search(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @Context UriInfo uriInfo, HierarchicalProperty body)
   {

      if (log.isDebugEnabled())
      {
         log.debug("SEARCH " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         repoName = getRepositoryName(repoName);
         Session session = session(repoName, workspaceName(repoPath), null);
         String uri =
            uriInfo.getBaseUriBuilder().path(getClass()).path(repoName).path(workspaceName(repoPath)).build()
               .toString();
         return new SearchCommand().search(session, body, uri);

      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @UNCHECKOUT
   @Path("/{repoName}/{repoPath:.*}/")
   public Response uncheckout(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader)
   {

      if (log.isDebugEnabled())
      {
         log.debug("UNCHECKOUT " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         Session session = session(repoName, workspaceName(repoPath), lockTokens(lockTokenHeader, ifHeader));
         return new UnCheckOutCommand().uncheckout(session, path(repoPath));

      }
      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspaceException " + exc.getMessage(), exc);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();

      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Platform
    */
   @VERSIONCONTROL
   @Path("/{repoName}/{repoPath:.*}/")
   public Response versionControl(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader)
   {

      if (log.isDebugEnabled())
      {
         log.debug("VERSION-CONTROL " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      Session session;
      try
      {
         session = session(repoName, workspaceName(repoPath), lockTokens(lockTokenHeader, ifHeader));
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
      return new VersionControlCommand().versionControl(session, path(repoPath));
   }

   /**
    * WebDAV ACL method according to protocol extension - Access Control Protocol: RFC3744
    * More details here: <a href='http://www.webdav.org/specs/rfc3744.html'>Web Distributed 
    * Authoring and Versioning (WebDAV) Access Control Protocol</a>
    * @param repoName repository name
    * @param repoPath path in repository
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If- HTTP Header
    * @param body Request body
    * @return the instance of javax.ws.rs.core.Response
    * @LevelAPI Provisional
    */
   @ACL
   @Path("/{repoName}/{repoPath:.*}/")
   public Response acl(@PathParam("repoName") String repoName, @PathParam("repoPath") String repoPath,
      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader, @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
      HierarchicalProperty body)
   {
      if (log.isDebugEnabled())
      {
         log.debug("ACL " + repoName + "/" + repoPath);
      }

      repoPath = normalizePath(repoPath);

      try
      {
         List<String> lockTokens = lockTokens(lockTokenHeader, ifHeader);
         Session session = session(repoName, workspaceName(repoPath), lockTokens);
         return new AclCommand().acl(session, path(repoPath), body);
      }

      catch (NoSuchWorkspaceException exc)
      {
         log.error("NoSuchWorkspace. " + exc.getMessage());
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.status(HTTPStatus.INTERNAL_ERROR).entity(exc.getMessage()).build();
      }
   }

   /**
    * Gives access to the current session.
    * 
    * @param repoName repository name
    * @param wsName workspace name
    * @param lockTokens Lock tokens
    * @return current session
    * @throws Exception {@link Exception}
    */
   protected Session session(String repoName, String wsName, List<String> lockTokens) throws Exception,
      NoSuchWorkspaceException
   {
      // To be cloud compliant we need now to ignore the provided repository name (more details in JCR-2138)
      ManageableRepository repo = repositoryService.getCurrentRepository();
      if (PropertyManager.isDevelopping() && log.isWarnEnabled())
      {
         String currentRepositoryName = repo.getConfiguration().getName();
         if (!currentRepositoryName.equals(repoName))
         {
            log.warn("The expected repository was '" + repoName
               + "' but we will use the current repository instead which is '" + currentRepositoryName + "'");
         }
      }
      SessionProvider sp = sessionProviderService.getSessionProvider(null);
      if (sp == null)
         throw new RepositoryException("SessionProvider is not properly set. Make the application calls"
            + "SessionProviderService.setSessionProvider(..) somewhere before ("
            + "for instance in Servlet Filter for WEB application)");

      Session session = sp.getSession(wsName, repo);
      if (lockTokens != null)
      {
         String[] presentLockTokens = session.getLockTokens();
         ArrayList<String> presentLockTokensList = new ArrayList<String>();
         for (int i = 0; i < presentLockTokens.length; i++)
         {
            presentLockTokensList.add(presentLockTokens[i]);
         }

         for (int i = 0; i < lockTokens.size(); i++)
         {
            String lockToken = lockTokens.get(i);
            if (!presentLockTokensList.contains(lockToken))
            {
               session.addLockToken(lockToken);
            }
         }
      }
      return session;
   }

   
   /**
    * Gives the name of the repository to access. 
    * @param repoName the name of the expected repository.
    * @return the name of the repository to access.
    */
   protected String getRepositoryName(String repoName) throws RepositoryException
   {
      // To be cloud compliant we need now to ignore the provided repository name (more details in JCR-2138)
      ManageableRepository repo = repositoryService.getCurrentRepository();
      String currentRepositoryName = repo.getConfiguration().getName();
      if (PropertyManager.isDevelopping() && log.isWarnEnabled())
      {
         if (!currentRepositoryName.equals(repoName))
         {
            log.warn("The expected repository was '" + repoName
               + "' but we will use the current repository instead which is '" + currentRepositoryName + "'");
         }
      }
      return currentRepositoryName;
   }

   /**
    * Extracts workspace name from repository path.
    * 
    * @param repoPath repository path
    * @return workspace name
    */
   protected String workspaceName(String repoPath)
   {
      return repoPath.split("/")[0];
   }

   /**
    * Normalizes path.
    * 
    * @param repoPath repository path
    * @return normalized path.
    */
   protected String normalizePath(String repoPath)
   {
      if (repoPath.length() > 0 && repoPath.endsWith("/"))
      {
         return repoPath.substring(0, repoPath.length() - 1);
      }

      return repoPath;
   }

   /**
    * Extracts path from repository path.
    * 
    * @param repoPath repository path
    * @return path
    */
   protected String path(String repoPath)
   {
      return path(repoPath, true);
   }

   /**
    * Extracts path from repository path.
    * 
    * @param repoPath repository path
    * @param withIndex indicates whether the index must be removed or not
    * @return path
    */
   protected String path(String repoPath, boolean withIndex)
   {
      String path = repoPath.substring(workspaceName(repoPath).length());

      if (path.length() > 0)
      {
         if (!withIndex)
         {
            return TextUtil.removeIndexFromPath(path);
         }
         return path;
      }

      return "/";
   }

   /**
    * Creates the list of Lock tokens from Lock-Token and If headers.
    * 
    * @param lockTokenHeader Lock-Token HTTP header
    * @param ifHeader If HTTP header
    * @return the list of lock tokens
    */
   protected List<String> lockTokens(String lockTokenHeader, String ifHeader)
   {
      ArrayList<String> lockTokens = new ArrayList<String>();

      if (lockTokenHeader != null)
      {
         if (lockTokenHeader.startsWith("<"))
         {
            lockTokenHeader = lockTokenHeader.substring(1, lockTokenHeader.length() - 1);
         }

         if (lockTokenHeader.contains(WebDavConst.Lock.OPAQUE_LOCK_TOKEN))
         {
            lockTokenHeader = lockTokenHeader.split(":")[1];
         }

         lockTokens.add(lockTokenHeader);
      }

      if (ifHeader != null)
      {
         String headerLockToken = ifHeader.substring(ifHeader.indexOf("("));
         headerLockToken = headerLockToken.substring(2, headerLockToken.length() - 2);
         if (headerLockToken.contains(WebDavConst.Lock.OPAQUE_LOCK_TOKEN))
         {
            headerLockToken = headerLockToken.split(":")[1];
         }
         lockTokens.add(headerLockToken);
      }

      return lockTokens;
   }

   /** 
    * Build URI from string. 
    */
   private URI buildURI(String path) throws URISyntaxException
   {
      try
      {
         return new URI(path);
      }
      catch (URISyntaxException e)
      {
         return new URI(TextUtil.escape(path, '%', true));
      }
   }

}
