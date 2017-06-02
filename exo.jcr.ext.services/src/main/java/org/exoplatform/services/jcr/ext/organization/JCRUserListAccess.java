/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JCRListAccess.java 111 2008-11-11 11:11:11Z $
 */
public abstract class JCRUserListAccess implements ListAccess<User>
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo-jcr-services.JCRUserListAccess");

   /**
    * Default value of page size. 
    */
   public static final int DEFAULT_PAGE_SIZE = 20;

   /**
    * JCR implementation of {@link OrganizationService}.
    */
   protected final JCROrganizationServiceImpl service;

   /**
    * JCR implementation of {@link UserHandler}.
    */
   protected final UserHandlerImpl uHandler;

   /**
    * Utility class.
    */
   protected final Utils utils;

   /**
    * Iterator by children items. 
    */
   protected NodeIterator iterator;

   /**
    * Indicates the expected status of the users
    */
   protected final UserStatus status;

   /**
    * JCRListAccess constructor.
    */
   public JCRUserListAccess(JCROrganizationServiceImpl service, UserStatus status) throws RepositoryException
   {
      this.service = service;
      this.uHandler = (UserHandlerImpl)service.getUserHandler();
      this.utils = new Utils(service);
      this.status = status;
   }

   /**
    * {@inheritDoc}
    */
   public User[] load(int index, int length) throws Exception, IllegalArgumentException
   {
      validateParameters(index, length);

      if (length == 0)
      {
         return new User[0];
      }

      Session session = service.getStorageSession();
      try
      {
         reuseIterator(session, index);

         User[] entities = new User[length];
         int processed = 0;

         while (iterator.hasNext() && processed != length)
         {
            Node n = iterator.nextNode();
            if (!accept(n))
               continue;
            entities[processed++] = (User)readObject(n);
         }

         if (processed != length)
         {
            throw new IllegalArgumentException(
               "Illegal index or length: sum of the index and the length cannot be greater than the list size");
         }

         return entities;
      }
      finally
      {
         session.logout();
      }
   }

   private void validateParameters(int index, int length) throws IllegalArgumentException
   {
      if (index < 0)
      {
         throw new IllegalArgumentException("Illegal index: index must be a positive number");
      }

      if (length < 0)
      {
         throw new IllegalArgumentException("Illegal length: length must be a positive number");
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getSize() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return getSize(session);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Check if possible to reuse current iterator (which means possibility to fetch next nodes in row).
    * Otherwise the new one will be created.
    */
   protected void reuseIterator(Session session, int newPosition) throws RepositoryException
   {
      if (!(canReuseIterator() && iterator != null && iterator.getPosition() == newPosition))
      {
         iterator = createIterator(session);
         iterator.skip(newPosition);
      }
   }

   /**
    * Indicates if we able to reuse current iterator.
    */
   protected boolean canReuseIterator()
   {
      return true;
   }

   /**
    * Reads entity from node.
    */
   protected abstract Object readObject(Node node) throws Exception;

   /**
    * Returns iterator over entities.
    */
   protected abstract NodeIterator createIterator(Session session) throws RepositoryException;

   /**
    * Determines the count of available users.
    */
   protected abstract int getSize(Session session) throws Exception;

   /**
     * Tests whether or not the specified node should be
     * included in the node list.
     *
     * @param node  The node to be tested
     * @return <code>true</code> if and only if <code>node</code>
     *          should be included
    */
   protected boolean accept(Node node)
   {
      try
      {
         return status == UserStatus.ANY || status.matches(node.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED));
      }
      catch (RepositoryException e)
      {
         if (LOG.isDebugEnabled())
         {
            String path = "unknown";
            try
            {
               path = node.getPath();
            }
            catch (RepositoryException e1)
            {
               LOG.debug("Could not get the node of the node: " + node, e1);
            }
            LOG.debug("Could not know if the mixin type " + JCROrganizationServiceImpl.JOS_DISABLED
               + " has been added to the node " + path, e);
         }
         return true;
      }
   }
}
