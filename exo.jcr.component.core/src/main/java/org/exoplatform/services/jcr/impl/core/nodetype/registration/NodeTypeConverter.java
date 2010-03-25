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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataImpl;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class NodeTypeConverter
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeConverter");

   private final LocationFactory locationFactory;

   protected final String accessControlPolicy;

   public NodeTypeConverter(LocationFactory locationFactory, String accessControlPolicy)
   {
      super();
      this.locationFactory = locationFactory;
      this.accessControlPolicy = accessControlPolicy;
   }

   public List<NodeTypeData> convertFromValueToData(List<NodeTypeValue> ntvalues) throws RepositoryException
   {
      List<NodeTypeData> nodeTypeDataList = new ArrayList<NodeTypeData>();
      for (NodeTypeValue ntvalue : ntvalues)
      {

         if (accessControlPolicy.equals(AccessControlPolicy.DISABLE))
         {
            List<String> nsupertypes = ntvalue.getDeclaredSupertypeNames();
            if (nsupertypes != null && nsupertypes.contains("exo:privilegeable")
               || ntvalue.getName().equals("exo:privilegeable"))
            {
               // skip this node, so it's not necessary at this runtime
               // + "' -- it's not necessary at this runtime";
               log.warn("Node type " + ntvalue.getName() + " is not register due to DISABLE control policy");
               break;
            }
         }

         // We have to validate node value before registering it
         ntvalue.validateNodeType();
         // throw new RepositoryException("Invalid node type value");

         // declaring NT name
         InternalQName ntName = locationFactory.parseJCRName(ntvalue.getName()).getInternalName();

         List<String> stlist = ntvalue.getDeclaredSupertypeNames();
         InternalQName[] supertypes = new InternalQName[stlist.size()];
         for (int i = 0; i < stlist.size(); i++)
         {
            supertypes[i] = locationFactory.parseJCRName(stlist.get(i)).getInternalName();
         }

         List<PropertyDefinitionValue> pdlist = ntvalue.getDeclaredPropertyDefinitionValues();
         PropertyDefinitionData[] props = new PropertyDefinitionData[pdlist.size()];
         for (int i = 0; i < pdlist.size(); i++)
         {
            PropertyDefinitionValue v = pdlist.get(i);

            PropertyDefinitionData pd;
            pd =
               new PropertyDefinitionData(locationFactory.parseJCRName(v.getName()).getInternalName(), ntName, v
                  .isAutoCreate(), v.isMandatory(), v.getOnVersion(), v.isReadOnly(), v.getRequiredType(),
                  safeListToArray(v.getValueConstraints()), safeListToArray(v.getDefaultValueStrings()), v.isMultiple());

            props[i] = pd;
         }

         List<NodeDefinitionValue> ndlist = ntvalue.getDeclaredChildNodeDefinitionValues();
         NodeDefinitionData[] nodes = new NodeDefinitionData[ndlist.size()];
         for (int i = 0; i < ndlist.size(); i++)
         {
            NodeDefinitionValue v = ndlist.get(i);

            List<String> rnts = v.getRequiredNodeTypeNames();
            InternalQName[] requiredNTs = new InternalQName[rnts.size()];
            for (int ri = 0; ri < rnts.size(); ri++)
            {
               requiredNTs[ri] = locationFactory.parseJCRName(rnts.get(ri)).getInternalName();
            }
            InternalQName defaultNodeName = null;
            if (v.getDefaultNodeTypeName() != null)
            {
               defaultNodeName = locationFactory.parseJCRName(v.getDefaultNodeTypeName()).getInternalName();
            }
            NodeDefinitionData nd =
               new NodeDefinitionData(locationFactory.parseJCRName(v.getName()).getInternalName(), ntName, v
                  .isAutoCreate(), v.isMandatory(), v.getOnVersion(), v.isReadOnly(), requiredNTs, defaultNodeName, v
                  .isSameNameSiblings());
            nodes[i] = nd;
         }

         InternalQName primaryItemName = null;
         if (ntvalue.getPrimaryItemName() != null)
            primaryItemName = locationFactory.parseJCRName(ntvalue.getPrimaryItemName()).getInternalName();

         NodeTypeData nodeTypeData =
            new NodeTypeDataImpl(ntName, primaryItemName, ntvalue.isMixin(), ntvalue.isOrderableChild(), supertypes,
               props, nodes);

         nodeTypeDataList.add(nodeTypeData);
      }
      return nodeTypeDataList;
   }

   public Map<InternalQName, NodeTypeData> convertToMap(List<NodeTypeData> ntvalues)
   {
      Map<InternalQName, NodeTypeData> result = new HashMap<InternalQName, NodeTypeData>();
      for (NodeTypeData nodeTypeData : ntvalues)
      {
         result.put(nodeTypeData.getName(), nodeTypeData);
      }
      return result;
   }

   /**
    * Convert list to array.
    * 
    * @param v list of string
    * @return array of string, empty array if list null.
    */
   private String[] safeListToArray(List<String> v)
   {
      return v != null ? v.toArray(new String[v.size()]) : new String[0];
   }
}
