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
package org.exoplatform.frameworks.jcr.web;

import org.apache.commons.chain.Command;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.frameworks.jcr.command.web.GenericWebAppContext;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SAS . Shortcut for
 * {@code http://localhost:8080/simple-cms/jcr?workspace=digital-assets&path=/Image.jpg}
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: DisplayJCRContentServlet.java 14756 2008-05-26 14:47:15Z pnedonosko $
 */
public class DisplayJCRContentServlet extends HttpServlet
{

   ServletConfig config;

   @Override
   protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
      IOException
   {

      ExoContainer container = (ExoContainer)getServletContext().getAttribute(WebConstants.EXO_CONTAINER);
      if (container == null)
      {
         container = PortalContainer.getCurrentInstance(getServletContext());
      }

      SessionProviderService sessionProviderService =
         (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);

      RepositoryService repositoryService =
         (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      CommandService commandService = (CommandService)container.getComponentInstanceOfType(CommandService.class);

      GenericWebAppContext ctx;
      try
      {
         ctx =
            new GenericWebAppContext(getServletContext(), request, response, sessionProviderService
               .getSessionProvider(null), // null for
               // ThreadLocalSessionProvider
               repositoryService.getDefaultRepository());
      }
      catch (final RepositoryException e)
      {
         throw new IOException()
         {
            @Override
            public Throwable getCause()
            {
               return e;
            }
         };
      }
      catch (final RepositoryConfigurationException e)
      {
         throw new IOException()
         {
            @Override
            public Throwable getCause()
            {
               return e;
            }
         };
      }

      String catalogName = (String)ctx.get(WebConstants.CATALOG_NAME);

      String wsName = (String)ctx.get("workspace");
      if (wsName != null)
         ctx.setCurrentWorkspace(wsName);

      String currentPath = (String)ctx.get("path");

      if (currentPath == null)
         throw new ServletException("Path undefined " + request.getParameter("path") + " Request: "
            + request.getRequestURI());

      try
      {
         Command cmd;
         if (catalogName == null)
            cmd = commandService.getCatalog().getCommand("displayResource");
         else
            cmd = commandService.getCatalog(catalogName).getCommand("displayResource");

         if (cmd == null)
            throw new Exception("No 'displayResource' command found");
         ctx.put("path", currentPath);
         ctx.put("cache-control-max-age", getServletConfig().getInitParameter("cache-control-max-age"));
         cmd.execute(ctx);
      }
      catch (Exception e)
      {
         e.printStackTrace(); //NOSONAR
         throw new ServletException(e);
      }
   }

}
