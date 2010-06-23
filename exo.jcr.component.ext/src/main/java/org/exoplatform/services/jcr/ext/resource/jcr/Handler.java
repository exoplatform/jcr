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
package org.exoplatform.services.jcr.ext.resource.jcr;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.resource.JcrURLConnection;
import org.exoplatform.services.jcr.ext.resource.NodeRepresentationService;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;
import org.exoplatform.services.security.ConversationState;
import org.picocontainer.Startable;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.jcr.Session;

/**
 * URLStreamHandler for protocol <tt>jcr://</tt>.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class Handler extends URLStreamHandler implements Startable
{

   /*
    * This is implements as Startable to be independent from other services. It
    * should be guaranty created, and set special system property.
    * Static fields repositoryService, nodeRepresentationService should be
    * initialized once when Handler created by container.
    */

   /**
    * It specifies the package prefix name with should be added in System
    * property java.protocol.handler.pkgs. Protocol handlers will be in a class
    * called <tt>jcr</tt>.Handler.
    */
   private static final String protocolPathPkg = "org.exoplatform.services.jcr.ext.resource";

   /**
    * Repository service.
    */
   private static RepositoryService repositoryService;

   /**
    * See {@link NodeRepresentationService}.
    */
   private static NodeRepresentationService nodeRepresentationService;

   private static ThreadLocalSessionProviderService threadLocalSessionProviderService;

   public Handler(RepositoryService rs, NodeRepresentationService nrs, ThreadLocalSessionProviderService tsps)
   {
      repositoryService = rs;
      nodeRepresentationService = nrs;
      threadLocalSessionProviderService = tsps;
   }

   /*
    * This constructor will be used by java.net.URL .
    * NOTE Currently it is not possible use next under servlet container:
    * URL url = new URL("jcr://repo/workspace#/path");
    * even system property 'java.protocol.handler.pkgs' contains correct
    * package name for handler java.net.URL use SystemClassLoader, and it
    * does work when handler is lib/.jar file or in .war file.
    * This bug is described here:
    * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4648098
    */
   public Handler()
   {
   }

   // URLStreamHandler

   /**
    * {@inheritDoc}
    */
   @Override
   protected URLConnection openConnection(URL url) throws IOException
   {
      try
      {
         UnifiedNodeReference nodeReference = new UnifiedNodeReference(url);

         // First try use user specified session provider, e.g.
         // ThreadLocalSessionProvider or System SessionProvider
         SessionProvider sessionProvider = threadLocalSessionProviderService.getSessionProvider(null);

         if (sessionProvider == null && ConversationState.getCurrent() != null)
            sessionProvider =
               (SessionProvider)ConversationState.getCurrent().getAttribute(SessionProvider.SESSION_PROVIDER);

         ManageableRepository repository;
         String repositoryName = nodeReference.getRepository();
         if (repositoryName == null || repositoryName.length() == 0)
            repository = sessionProvider.getCurrentRepository();
         else
            repository = repositoryService.getRepository(repositoryName);

         String workspaceName = nodeReference.getWorkspace();
         if (workspaceName == null || workspaceName.length() == 0)
            workspaceName = sessionProvider.getCurrentWorkspace();

         Session ses = sessionProvider.getSession(workspaceName, repository);
         JcrURLConnection conn = new JcrURLConnection(nodeReference, ses, nodeRepresentationService);
         return conn;

      }
      catch (Exception e)
      {
         //e.printStackTrace();
         throw new IOException("Open connection to URL '" + url.toString() + "' failed!");
      }
   }

   // Startable

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      String existingProtocolPathPkgs = System.getProperty("java.protocol.handler.pkgs");
      if (existingProtocolPathPkgs == null)
         System.setProperty("java.protocol.handler.pkgs", protocolPathPkg);
      else if (existingProtocolPathPkgs.indexOf(protocolPathPkg) == -1)
         System.setProperty("java.protocol.handler.pkgs", existingProtocolPathPkgs + "|" + protocolPathPkg);
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      // nothing to do!
   }

}
