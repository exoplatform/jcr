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
package org.exoplatform.services.jcr.webdav.xml;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.resource.Resource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SARL.<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class PropstatGroupedRepresentation
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger(PropstatGroupedRepresentation.class);

   /**
    * properties statuses.
    */
   protected final Map<String, Set<HierarchicalProperty>> propStats;

   /**
    * properties names
    */
   protected Set<QName> propNames = null;

   /**
    * Boolean flag. Shows if only properties names are required (true) or
    * properties and values (false).
    */
   protected final boolean namesOnly;

   /**
    * resource.
    */
   protected final Resource resource;

   /**
    * @param resource resource
    * @param propNames properties names
    * @param namesOnly Boolean flag. Shows if only properties names are required
    *          (true) or properties and values (false).
    * @throws RepositoryException {@link RepositoryException}
    */
   public PropstatGroupedRepresentation(final Resource resource, final Set<QName> propNames, boolean namesOnly)
      throws RepositoryException
   {
      this.namesOnly = namesOnly;
      this.resource = resource;
      this.propStats = new HashMap<String, Set<HierarchicalProperty>>();

      this.propNames = propNames;

      if (propNames != null)
      {
         this.propNames = new HashSet();
         Iterator<QName> propertyNameIter = propNames.iterator();
         while (propertyNameIter.hasNext())
         {
            QName property = propertyNameIter.next();
            this.propNames.add(property);
         }
      }
   }

   /**
    * Returns properties statuses.
    * 
    * @return properties statuses
    * @throws RepositoryException {@link RepositoryException}
    */
   public final Map<String, Set<HierarchicalProperty>> getPropStats() throws RepositoryException
   {
      String statname = WebDavConst.getStatusDescription(HTTPStatus.OK);
      if (propNames == null)
      {
         propStats.put(statname, resource.getProperties(namesOnly));
      }
      else
      {

         for (QName propName : propNames)
         {
            HierarchicalProperty prop = new HierarchicalProperty(propName);
            try
            {
               prop = resource.getProperty(propName);
               statname = WebDavConst.getStatusDescription(HTTPStatus.OK);

            }
            catch (AccessDeniedException exc)
            {
               statname = WebDavConst.getStatusDescription(HTTPStatus.FORBIDDEN);
               log.error(exc.getMessage(), exc);
            }
            catch (ItemNotFoundException exc)
            {
               statname = WebDavConst.getStatusDescription(HTTPStatus.NOT_FOUND);

            }
            catch (PathNotFoundException e)
            {
               statname = WebDavConst.getStatusDescription(HTTPStatus.NOT_FOUND);

            }
            catch (RepositoryException e)
            {
               statname = WebDavConst.getStatusDescription(HTTPStatus.INTERNAL_ERROR);
            }

            if (!propStats.containsKey(statname))
            {
               propStats.put(statname, new HashSet<HierarchicalProperty>());
            }

            Set<HierarchicalProperty> propSet = propStats.get(statname);
            propSet.add(prop);
         }
      }
      return propStats;
   }

}
