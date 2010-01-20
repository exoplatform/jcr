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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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

   private static Log log = ExoLogger.getLogger("jcr.LocalWorkspaceDataManagerStub");

   public LocalWorkspaceDataManagerStub(CacheableWorkspaceDataManager persistentManager)
   {
      super(persistentManager);
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
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException
   {
      return Collections.unmodifiableList(super.getChildPropertiesData(parent));
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
