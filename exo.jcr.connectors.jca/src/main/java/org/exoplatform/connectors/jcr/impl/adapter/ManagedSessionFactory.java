/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.connectors.jcr.impl.adapter;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.PrintWriter;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Credentials;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class ManagedSessionFactory implements ManagedConnectionFactory
{

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = 2298804451713956342L;

   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ManagedSessionFactory");

   /**
    * The default name of the portal container
    */
   private String portalContainer;

   /**
    * The name of the repository
    */
   private String repository;

   /**
    * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory()
    */
   public Object createConnectionFactory() throws ResourceException
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("createConnectionFactory()");
      }
      throw new NotSupportedException("Only managed environments are supported");
   }

   /**
    * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(javax.resource.spi.ConnectionManager)
    */
   public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("createConnectionFactory(" + cxManager + ")");
      }
      return new SessionFactoryImpl(cxManager, this);
   }

   /**
    * {@inheritDoc}
    */
   public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
      throws ResourceException
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("createManagedConnection(" + subject + "," + cxRequestInfo + ")");
      }
      if (!(cxRequestInfo instanceof SessionRequestInfo))
      {
         throw new ResourceException("The connection request info must be an instance of type SessionRequestInfo");
      }
      ExoContainer container = getContainer();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("createManagedConnection: container = " + container.getContext().getName());
      }
      RepositoryService rs = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      if (rs == null)
      {
         throw new ResourceException("No RepositoryService could be found in the container "
            + container.getContext().getName());
      }
      ManageableRepository mRepository = getRepository(rs);
      SessionRequestInfo sri = (SessionRequestInfo)cxRequestInfo;
      String workspaceName = sri.getWorkspace();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("createManagedConnection: workspace = " + (workspaceName == null ? "'default'" : workspaceName));
      }
      Credentials credentials = getCredentials(subject, sri);
      SessionImpl session = null;
      try
      {
         session = (SessionImpl)mRepository.login(credentials, workspaceName);
      }
      catch (Exception e)
      {
         throw new ResourceException("Could not login to the workspace " + 
            (workspaceName == null ? "'default'" : workspaceName), e);
      }
      return new ManagedSessionImpl(session);
   }

   /**
    * Gets the credentials from the subject, if it cannot be found and the user name
    * has been set in the {@link SessionRequestInfo}, we will use the credentials
    * from the {@link SessionRequestInfo}, otherwise we will use the credentials
    * defined in the configuration of the {@link ManagedSessionFactory}.
    */
   private Credentials getCredentials(final Subject subject, SessionRequestInfo sri) throws ResourceException
   {
      CredentialsImpl credentials = null;
      if (subject != null)
      {
         credentials = SecurityHelper.doPrivilegedAction(new PrivilegedAction<CredentialsImpl>()
         {
            public CredentialsImpl run()
            {
               Iterator<Object> i = subject.getPrivateCredentials().iterator();
               while (i.hasNext())
               {
                  Object o = i.next();
                  if (o instanceof PasswordCredential)
                  {
                     PasswordCredential cred = (PasswordCredential)o;
                     if (cred.getManagedConnectionFactory().equals(ManagedSessionFactory.this))
                     {
                        return new CredentialsImpl(cred.getUserName(), cred.getPassword());
                     }
                  }
               }
               return null;
            }
         });
      }
      if (credentials == null && sri.getUserName() != null)
      {
         credentials =
            new CredentialsImpl(sri.getUserName(), sri.getPassword() == null ? null : sri.getPassword()
               .toCharArray());
      }
      if (LOG.isDebugEnabled())
      {
         LOG.debug("getCredentials: login = " + (credentials == null ? "undefined" : credentials.getUserID()));
      }
      return credentials;
   }

   /**
    * Gets the repository corresponding to the given context, if a repository name
    * has been defined in the configuration of the {@link ManagedSessionFactory}
    * it will try to get it otherwise it will get the current repository
    */
   private ManageableRepository getRepository(RepositoryService rs) throws ResourceException
   {
      ManageableRepository mRepository = null;
      try
      {
         mRepository = repository == null ? rs.getCurrentRepository() : rs.getRepository(repository);
      }
      catch (Exception e)
      {
         throw new ResourceException("Could not get the "
            + (repository == null ? "current repository." : "repository '" + repository + "'"), e);
      }
      if (mRepository == null)
      {
         throw new ResourceException("Could not find the "
            + (repository == null ? "current repository." : "repository '" + repository + "'"));
      }
      if (LOG.isDebugEnabled())
      {
         LOG.debug("getRepository: repository = " + (repository == null ? "current." : repository));
      }

      return mRepository;
   }

   /**
    * Gets the container from the current context
    */
   private ExoContainer getContainer() throws ResourceException
   {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      if (container instanceof RootContainer)
      {
         String portalContainerName =
            portalContainer == null ? PortalContainer.DEFAULT_PORTAL_CONTAINER_NAME : portalContainer;
         container = RootContainer.getInstance().getPortalContainer(portalContainerName);
         if (container == null)
         {
            throw new ResourceException("The eXo container is null, because the current container is a RootContainer "
               + "and there is no PortalContainer with the name '" + portalContainerName + "'.");
         }
      }
      else if (container == null)
      {
         throw new ResourceException("The eXo container is null, because the current container is null.");
      }
      return container;
   }

   /**
    * @see javax.resource.spi.ManagedConnectionFactory#getLogWriter()
    */
   public PrintWriter getLogWriter() throws ResourceException
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connectionSet, Subject subject,
      ConnectionRequestInfo cxRequestInfo) throws ResourceException
   {
      throw new NotSupportedException("The JCR sessions are not shareable");
   }

   /**
    * @see javax.resource.spi.ManagedConnectionFactory#setLogWriter(java.io.PrintWriter)
    */
   public void setLogWriter(PrintWriter out) throws ResourceException
   {
   }

   /**
    * @return the portalContainer
    */
   public String getPortalContainer()
   {
      return portalContainer;
   }

   /**
    * @param portalContainer the portalContainer to set
    */
   public void setPortalContainer(String portalContainer)
   {
      this.portalContainer = portalContainer;
   }

   /**
    * @return the repository
    */
   public String getRepository()
   {
      return repository;
   }

   /**
    * @param repository the repository to set
    */
   public void setRepository(String repository)
   {
      this.repository = repository;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((portalContainer == null) ? 0 : portalContainer.hashCode());
      result = prime * result + ((repository == null) ? 0 : repository.hashCode());
      return result;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ManagedSessionFactory other = (ManagedSessionFactory)obj;
      if (portalContainer == null)
      {
         if (other.portalContainer != null)
            return false;
      }
      else if (!portalContainer.equals(other.portalContainer))
         return false;
      if (repository == null)
      {
         if (other.repository != null)
            return false;
      }
      else if (!repository.equals(other.repository))
         return false;
      return true;
   }
}
