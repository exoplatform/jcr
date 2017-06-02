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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: SimpleUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class SimpleJCRUserListAccess extends JCRUserListAccess
{

   /**
    * The parent node where all users nodes are persisted.
    */
   private final ExtendedNode usersStorageNode;

   /**
    * JCRUserListAccess constructor.
    */
   public SimpleJCRUserListAccess(JCROrganizationServiceImpl service, UserStatus status) throws RepositoryException
   {
      super(service, status);
      usersStorageNode = getUsersStorageNode();
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      long result = usersStorageNode.getNodesCount();
      if (status != UserStatus.ANY)
      {
         StringBuilder statement = new StringBuilder("SELECT * FROM ");
         statement.append(JCROrganizationServiceImpl.JOS_USERS_NODETYPE).append(" WHERE");
         statement.append(" jcr:path LIKE '").append(usersStorageNode.getPath()).append("/%'");
         statement.append(" AND NOT jcr:path LIKE '").append(usersStorageNode.getPath()).append("/%/%'");
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
      return uHandler.readUser(node);
   }

   /**
    * {@inheritDoc}
    */
   protected NodeIterator createIterator(Session session) throws RepositoryException
   {
      return usersStorageNode.getNodesLazily(DEFAULT_PAGE_SIZE);
   }

   /**
    * Returns users storage node.
    */
   private ExtendedNode getUsersStorageNode() throws RepositoryException
   {
      Session session = service.getStorageSession();
      try
      {
         return (ExtendedNode)utils.getUsersStorageNode(service.getStorageSession());
      }
      finally
      {
         session.logout();
      }
   }
}
