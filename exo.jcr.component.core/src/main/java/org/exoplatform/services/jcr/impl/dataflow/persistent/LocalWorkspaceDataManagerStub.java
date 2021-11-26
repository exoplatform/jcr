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

package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;

import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class LocalWorkspaceDataManagerStub extends VersionableWorkspaceDataManager
{

   public LocalWorkspaceDataManagerStub(CacheableWorkspaceDataManager persistentManager,
      TransactionableResourceManager txResourceManager)
   {
      super(persistentManager, txResourceManager);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(NodeData nodeData) throws RepositoryException
   {
      return Collections.unmodifiableList(super.getChildNodesData(nodeData));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(NodeData nodeData, List<QPathEntryFilter> patternFilters) throws RepositoryException
   {
      return Collections.unmodifiableList(super.getChildNodesData(nodeData, patternFilters));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException
   {
      return Collections.unmodifiableList(super.getChildPropertiesData(parent));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException
   {
      return Collections.unmodifiableList(super.getChildPropertiesData(parent, itemDataFilters));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> listChildPropertiesData(final NodeData parent) throws RepositoryException
   {
      return Collections.unmodifiableList(super.listChildPropertiesData(parent));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      return Collections.unmodifiableList(super.getReferencesData(identifier, skipVersionStorage));
   }
}
