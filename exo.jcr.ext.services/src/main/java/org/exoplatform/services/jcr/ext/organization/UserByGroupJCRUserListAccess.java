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

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.organization.UserStatus;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: UserByGroupJCRUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class UserByGroupJCRUserListAccess extends JCRUserListAccess
{

   /**
    * The parent node where all nodes with users's names related to current group are persisted.
    */
   private final ExtendedNode refUsers;

   /**
    * UserByGroupJCRUserListAccess constructor.
    */
   public UserByGroupJCRUserListAccess(JCROrganizationServiceImpl service, String groupId, UserStatus status) throws RepositoryException
   {
      super(service, status);
      refUsers = getRefUsersNode(groupId);
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      if (refUsers == null)
         return 0;
      long result = refUsers.getNodesCount();
      if (status != UserStatus.ANY)
      {
         StringBuilder statement = new StringBuilder("SELECT * FROM nt:base WHERE");
         statement.append(" jcr:path LIKE '").append(refUsers.getPath()).append("/%'");
         statement.append(" AND NOT jcr:path LIKE '").append(refUsers.getPath()).append("/%/%'");
         statement.append(" AND ").append(JCROrganizationServiceImpl.JOS_DISABLED);
         if (status == UserStatus.ENABLED)
            statement.append(" IS NOT NULL");
         else
            statement.append(" IS NULL");

         // We remove the total amount of disabled users
         result -= session.getWorkspace().getQueryManager()
            .createQuery(statement.toString(), Query.SQL).execute().getNodes().getSize();
      }
      return (int)result;
   }

   /**
    * {@inheritDoc}
    */
   protected Object readObject(Node node) throws Exception
   {
      Node userNode = utils.getUserNode(node.getSession(), node.getName());
      return uHandler.readUser(userNode);
   }

   /**
    * {@inheritDoc}
    */
   protected NodeIterator createIterator(Session session) throws RepositoryException
   {
      return refUsers == null ? new EmptyNodeIterator() : refUsers.getNodesLazily(DEFAULT_PAGE_SIZE);
   }

   private ExtendedNode getRefUsersNode(String groupId) throws PathNotFoundException, RepositoryException
   {
      Session session = service.getStorageSession();
      try
      {
         Node groupNode = utils.getGroupNode(service.getStorageSession(), groupId);
         return (ExtendedNode)groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP);
      }
      catch (PathNotFoundException e)
      {
         return null;
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Empty {@link NodeIterator}.
    */
   private class EmptyNodeIterator implements NodeIterator
   {

      public void skip(long skipNum)
      {
      }

      public long getSize()
      {
         return 0;
      }

      public long getPosition()
      {
         return 0;
      }

      public boolean hasNext()
      {
         return false;
      }

      public Object next()
      {
         throw new NoSuchElementException("Iteration has no more elements");
      }

      public void remove()
      {
         throw new UnsupportedOperationException("Operation is not supported");
      }

      public Node nextNode()
      {
         throw new NoSuchElementException("Iteration has no more elements");
      }
   }
}
