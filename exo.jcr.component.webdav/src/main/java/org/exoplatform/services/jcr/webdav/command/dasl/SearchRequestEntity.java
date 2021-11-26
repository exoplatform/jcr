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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.webdav.command.dasl;

import org.exoplatform.common.util.HierarchicalProperty;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */
public class SearchRequestEntity
{

   /**
    * Request body.
    */
   private HierarchicalProperty body;

   /**
    * Constructor.
    * 
    * @param body request body.
    */
   public SearchRequestEntity(HierarchicalProperty body)
   {
      this.body = body;
   }

   /**
    * Get query language.
    * 
    * @return query language
    * @throws UnsupportedQueryException {@link UnsupportedQueryException}
    */
   public String getQueryLanguage() throws UnsupportedQueryException
   {
      if (body.getChild(0).getName().getNamespaceURI().equals("DAV:")
         && body.getChild(0).getName().getLocalPart().equals("sql"))
      {
         return "sql";
      }
      else if (body.getChild(0).getName().getNamespaceURI().equals("DAV:")
         && body.getChild(0).getName().getLocalPart().equals("xpath"))
      {
         return "xpath";
      }

      throw new UnsupportedOperationException();
   }

   /**
    * Get query.
    * 
    * @return query qury body.
    * @throws UnsupportedQueryException {@link UnsupportedQueryException}
    */
   public String getQuery() throws UnsupportedQueryException
   {
      if (body.getChild(0).getName().getNamespaceURI().equals("DAV:")
         && body.getChild(0).getName().getLocalPart().equals("sql"))
      {
         return body.getChild(0).getValue();
      }
      else if (body.getChild(0).getName().getNamespaceURI().equals("DAV:")
         && body.getChild(0).getName().getLocalPart().equals("xpath"))
      {
         return body.getChild(0).getValue();
      }

      throw new UnsupportedQueryException();
   }

}
