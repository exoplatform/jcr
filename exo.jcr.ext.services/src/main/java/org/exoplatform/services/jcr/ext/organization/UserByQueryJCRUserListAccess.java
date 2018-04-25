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

import org.exoplatform.services.jcr.ext.organization.UserHandlerImpl.UserProperties;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.organization.UserStatus;

import java.sql.Timestamp;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: UserByQueryJCRUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class UserByQueryJCRUserListAccess extends JCRUserListAccess
{

   /**
    * The query.
    */
   private org.exoplatform.services.organization.Query query;

   /**
    * UserByQueryJCRUserListAccess constructor.
    */
   public UserByQueryJCRUserListAccess(JCROrganizationServiceImpl service,
      org.exoplatform.services.organization.Query query, UserStatus status) throws RepositoryException
   {
      super(service, status);
      this.query = query;
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      iterator = createIterator(session);
      return (int)iterator.getSize();
   }

   /**
    * Removes asterisk from beginning and from end of statement.
    */
   private String removeAsterisk(String str)
   {
      if (str.startsWith("*"))
      {
         str = str.substring(1);
      }

      if (str.endsWith("*"))
      {
         str = str.substring(0, str.length() - 1);
      }

      return str;
   }

   /**
    * Transforms {@link org.exoplatform.services.organization.Query} into {@link Query}.
    */
   private QueryImpl makeQuery(Session session) throws InvalidQueryException, RepositoryException
   {
      StatementContext context = new StatementContext();
      context.statement = new StringBuilder("SELECT * FROM ");
      context.statement.append(JCROrganizationServiceImpl.JOS_USERS_NODETYPE);

      if (query.getUserName() != null)
      {
         addStringStatement(context, UserProperties.JOS_USER_NAME, query.getUserName());
      }

      if (query.getFirstName() != null)
      {
         addStringStatement(context, UserProperties.JOS_FIRST_NAME, query.getFirstName());
      }

      if (query.getLastName() != null)
      {
         addStringStatement(context, UserProperties.JOS_LAST_NAME, query.getLastName());
      }

      if (query.getEmail() != null)
      {
         addStringStatement(context, UserProperties.JOS_EMAIL, query.getEmail());
      }

      if (query.getFromLoginDate() != null)
      {
         addDateStatement(context, UserProperties.JOS_LAST_LOGIN_TIME, ">=", query.getFromLoginDate());
      }

      if (query.getToLoginDate() != null)
      {
         addDateStatement(context, UserProperties.JOS_LAST_LOGIN_TIME, "<=", query.getToLoginDate());
      }

      if (status != UserStatus.ANY)
      {
         context.statement.append(" AND ").append(JCROrganizationServiceImpl.JOS_DISABLED);
         if (status == UserStatus.ENABLED)
            context.statement.append(" IS NULL");
         else
            context.statement.append(" IS NOT NULL");
      }

      return (QueryImpl)session.getWorkspace().getQueryManager().createQuery(context.statement.toString(), Query.SQL);
   }

   private void addStringStatement(StatementContext context, String field, String value)
   {
      addStatement(context, "UPPER(" + field + ")", "like", "'%" + removeAsterisk(value).toUpperCase() + "%'");
   }

   private void addDateStatement(StatementContext context, String field, String operand, Date value)
   {
      String timeStamp = new Timestamp(value.getTime()).toString();
      addStatement(context, field, operand, "TIMESTAMP '" + timeStamp + "'");
   }

   private void addStatement(StatementContext context, String field, String operand, String value)
   {
      if (context.hasWhere)
      {
         context.statement.append(" AND");
      }
      else
      {
         context.hasWhere = true;
         context.statement.append(" WHERE");
      }

      context.statement.append(" " + field + " " + operand + " " + value);
   }

   /**
    * {@inheritDoc}
    * 
    * It is not possible to reuse because session is closed in query.
    */
   protected boolean canReuseIterator()
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected NodeIterator createIterator(Session session) throws RepositoryException
   {
      QueryImpl query = makeQuery(session);
      QueryResult result = query.execute();

      return result.getNodes();
   }

   /**
    * {@inheritDoc}
    */
   protected Object readObject(Node node) throws Exception
   {
      return uHandler.readUser(node);
   }

   private class StatementContext
   {
      private StringBuilder statement;

      private boolean hasWhere;
   }
}
