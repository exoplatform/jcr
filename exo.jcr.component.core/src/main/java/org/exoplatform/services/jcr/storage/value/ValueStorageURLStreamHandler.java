/*
 * Copyright (C) 2014 eXo Platform SAS.
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
package org.exoplatform.services.jcr.storage.value;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This implementation of an {@link URLConnection} allows to get a content from
 * a value storage
 * 
 * <p>The syntax of an value storage URL is:
 * 
 * <pre>
 * vs:/{repository}/{workspace}/{value-storage-id}/{resource-id}
 * </pre>
 *
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class ValueStorageURLStreamHandler extends URLStreamHandler
{
   /**
    * The syntax of the expected path
    */
   private static Pattern PATH_SYNTAX = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/(.*)");

   /**
    * The protocol to access to a value storage
    */
   public static final String PROTOCOL = "vs";

   /**
    * A reusable instance of {@link ValueStorageURLStreamHandler}
    */
   public static final ValueStorageURLStreamHandler INSTANCE = new ValueStorageURLStreamHandler();

   /**
    * @see java.net.URLStreamHandler#openConnection(java.net.URL)
    */
   @Override
   protected URLConnection openConnection(URL u) throws IOException
   {
      if (!u.getProtocol().equals(PROTOCOL))
         throw new MalformedURLException("Only the protocol " + PROTOCOL + " is supported");
      if (u.getHost() != null)
         throw new MalformedURLException("The host will be automatically defined, so it is not expected");
      String file = u.getFile();
      Matcher m = PATH_SYNTAX.matcher(file);
      if (!m.matches())
         throw new MalformedURLException("The syntax of the path of the value storage URL doesn't match with"
            + " the expected syntax which is '/{repository}/{workspace}/{value-storage-id}/{resource-id}'");

      ValueStorageURLConnection connection = createURLConnection(u, m.group(1), m.group(2), m.group(3));
      connection.setIdResource(m.group(4));
      return connection;
   }

   /**
    * <p>Creates a new instance of {@link ValueStorageURLConnection} from the
    * value storage that belongs to the provided <code>repository</code> and <code>workspace</code>
    * and whose id is the provided <code>valueStorageId</code>.</p>
    * <p><b>NB:</b> <i>For performance reason, this method should be overridden by
    * sub classes. Indeed the default implementation will get {@link ValueStoragePluginProvider} 
    * from the current context but in practice, sub classes should already have it.
    * The default implementation only makes sense, when it will be called directly by 
    * {@link ValueStorageURLStreamHandler#openConnection(URL)} which happens when
    * we try to read the content of the target resource outside the context of the
    * value storage.</i>
    * </p>
    * @param u the {@link URL} of the resource
    * @param repository the name of the repository that owns the value storage
    * @param workspace the name of the workspace that owns the value storage
    * @param valueStorageId the id of the value storage
    * @return the {@link ValueStorageURLConnection} corresponding to the resource
    * @throws IOException if the {@link ValueStorageURLConnection} could not be created
    */
   protected ValueStorageURLConnection createURLConnection(URL u, String repository, String workspace,
      String valueStorageId) throws IOException
   {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      RepositoryService repositoryService =
         (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      if (repositoryService == null)
         throw new IOException("Could not find the repository service");
      try
      {
         ManageableRepository repo = repositoryService.getRepository(repository);
         WorkspaceContainerFacade workspaceContainer = repo.getWorkspaceContainer(workspace);
         ValueStoragePluginProvider provider =
            (ValueStoragePluginProvider)workspaceContainer.getComponent(ValueStoragePluginProvider.class);
         if (provider == null)
            throw new IOException("Could not find the ValueStoragePluginProvider for " + repository + "/" + workspace);
         return provider.createURLConnection(valueStorageId, u);
      }
      catch (IOException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new IOException("Could not get ValueStorageURLConnection from the value storage " + repository + "/"
            + workspace + "/" + valueStorageId, e);
      }
   }

   /**
    * @see java.net.URLStreamHandler#parseURL(java.net.URL, java.lang.String, int, int)
    */
   @Override
   protected void parseURL(URL u, String spec, int start, int limit)
   {
      setURL(u, PROTOCOL, null, -1, null, null, spec.substring(start, limit), null, null);
   }
}
