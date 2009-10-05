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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: CommandControllerServlet.java 14756 2008-05-26 14:47:15Z pnedonosko $
 */

@SuppressWarnings("serial")
public class CommandControllerServlet extends HttpServlet
{

   private static Log log = ExoLogger.getLogger("jcr:CommandControllerServlet");
   
   @Override
   protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
      IOException
   {

      ServletContext servletCtx = getServletContext();
      ExoContainer container = (ExoContainer)servletCtx.getAttribute(WebConstants.EXO_CONTAINER);
      final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
      boolean hasChanged = false;
      ClassLoader pcClassLoader = null;
      if (container == null)
      {
         container = PortalContainer.getCurrentInstance(servletCtx);
         if (container instanceof PortalContainer)
         {
            PortalContainer pc = (PortalContainer)container;
            pcClassLoader = pc.getPortalClassLoader();
            servletCtx = pc.getPortalContext();
         }
      }

      SessionProviderService sessionProviderService =
         (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);

      RepositoryService repositoryService =
         (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      GenericWebAppContext ctx;
      try
      {
         ctx =
            new GenericWebAppContext(servletCtx, request, response, sessionProviderService
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

      CommandService commandService = (CommandService)container.getComponentInstanceOfType(CommandService.class);
      String catalogName = (String)ctx.get(WebConstants.CATALOG_NAME);

      // command from context
      String commandName = (String)ctx.get("Command");
      if (commandName == null)
         throw new ServletException("No Command found at the Context");
      Command cmd;
      if (catalogName == null)
         cmd = commandService.getCatalog().getCommand(commandName);
      else
         cmd = commandService.getCatalog(catalogName).getCommand(commandName);

      if (cmd == null)
         throw new ServletException("No Command found " + commandName);
      try
      {
         if (pcClassLoader != null)
         {
            // Set the portal classloader to get all the resources defined within the portal context
            Thread.currentThread().setContextClassLoader(pcClassLoader);
            hasChanged = true;
         }         
         cmd.execute(ctx);
      }
      catch (Exception e)
      {
         log.error("An error occurs while executing the command " + commandName, e);
         throw new ServletException(e);
      }
      finally
      {
         if (hasChanged)
         {
            // Re-set the old classloader
            Thread.currentThread().setContextClassLoader(currentClassLoader);
         }
      }
   }

}
