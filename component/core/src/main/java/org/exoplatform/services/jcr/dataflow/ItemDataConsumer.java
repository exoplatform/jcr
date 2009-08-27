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
package org.exoplatform.services.jcr.dataflow;

import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: ItemDataConsumer.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          Basic (Level 1) data flow inmemory operations
 * 
 *          Common Rule for Read : If there is some storage in this manager � try to get the data
 *          from here first, if not found � call super.someMethod
 */
public interface ItemDataConsumer
{

   /**
    * @param parent
    * @param name
    * @return data by parent and name
    * @throws RepositoryException
    */
   ItemData getItemData(NodeData parent, QPathEntry name) throws RepositoryException;

   /**
    * @param identifier
    * @return data by identifier
    */
   ItemData getItemData(String identifier) throws RepositoryException;

   /**
    * @param parentIdentifier
    * @return children data
    */
   List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException;

   /**
    * @param parentIdentifier
    * @return children data
    */
   List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException;

   List<PropertyData> listChildPropertiesData(final NodeData nodeData) throws RepositoryException;

   /**
    * @param identifier
    *          - referenceable id
    * @param skipVersionStorage
    *          - if true references will be returned according the JSR-170 spec, without items from
    *          version storage
    * @return - list of REFERENCE properties
    * @throws RepositoryException
    */
   List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage) throws RepositoryException;
}
