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
package org.exoplatform.frameworks.jcr.web.fckeditor;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.frameworks.jcr.web.WebConstants;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JCRContentFCKeditor.java 6944 2006-07-11 08:06:04Z peterit $
 */

public class JCRContentFCKeditor extends FCKeditor
{

   private final static Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.JCRContentFCKeditor");

   private final String filePath;

   private final SessionProviderService sessionProviderService;

   private final ManageableRepository repo;

   public JCRContentFCKeditor(HttpServletRequest req, String parInstanceName, String workspaceName, String filePath,
      String newNodeType) throws RepositoryException
   {
      super(req, parInstanceName);

      this.filePath = filePath;

      ExoContainer container =
         (ExoContainer)req.getSession().getServletContext().getAttribute(WebConstants.EXO_CONTAINER);
      if (container == null)
      {
         container = PortalContainer.getCurrentInstance(req.getSession().getServletContext());
      }

      sessionProviderService =
         (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);

      RepositoryService repositoryService =
         (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      repo = repositoryService.getCurrentRepository();
      Session session =
         sessionProviderService.getSessionProvider(null).getSession(repo.getConfiguration().getDefaultWorkspaceName(),
            repo);

      Node file;
      try
      {
         file = (Node)session.getItem(filePath);
      }
      catch (PathNotFoundException e1)
      {
         file = session.getRootNode().addNode(filePath.substring(1), newNodeType);
      }
      
      if (!file.isNodeType("nt:file"))
      {
         throw new RepositoryException("The Node should be nt:file type");
      }

      try
      {
         Property content = (Property)session.getItem(filePath + "/jcr:content/jcr:data");
         this.setValue(content.getString());
      }
      catch (RepositoryException e)
      {
         LOG.error("Repository error " + e, e);
      }
   }

   public void saveValue(String value) throws RepositoryException
   {
      // file.setProperty("jcr:content/jcr:data", value);
      // [VO] "jcr:content/jcr:data" - impossible according to spec
      Session session =
         sessionProviderService.getSessionProvider(null).getSession(repo.getConfiguration().getDefaultWorkspaceName(),
            repo);

      try
      {
         Node file = (Node)session.getItem(filePath);

         file.getNode("jcr:content").setProperty("jcr:data", value);
         setValue(value);
         file.getSession().save();
      }
      finally
      {
         session.logout();
      }
   }
}
