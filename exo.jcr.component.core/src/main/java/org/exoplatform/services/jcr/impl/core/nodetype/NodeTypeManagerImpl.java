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
package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.util.EntityCollection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: NodeTypeManagerImpl.java 13986 2008-05-08 10:48:43Z pnedonosko
 *          $
 */
public class NodeTypeManagerImpl implements ExtendedNodeTypeManager
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeManagerImpl");

   public static final String NODETYPES_ROOT = "/jcr:system/jcr:nodetypes";

   protected final ValueFactory valueFactory;

   protected final LocationFactory locationFactory;

   protected final NodeTypeDataManager typesManager;

   public NodeTypeManagerImpl(LocationFactory locationFactory, ValueFactoryImpl valueFactory,
      NodeTypeDataManager typesManager)
   {
      this.valueFactory = valueFactory;
      this.locationFactory = locationFactory;
      this.typesManager = typesManager;

   }

   // JSR-170 stuff ================================

   public NodeType findNodeType(InternalQName nodeTypeName) throws NoSuchNodeTypeException, RepositoryException
   {

      NodeTypeData ntdata = typesManager.getNodeType(nodeTypeName);
      if (ntdata != null)
         return new NodeTypeImpl(ntdata, typesManager, this, locationFactory, valueFactory);

      throw new NoSuchNodeTypeException("Nodetype not found " + nodeTypeName.getAsString());
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeIterator getAllNodeTypes()
   {
      EntityCollection ec = new EntityCollection();
      List<NodeTypeData> allNts = typesManager.getAllNodeTypes();

      for (NodeTypeData ntdata : allNts)
         ec.add(new NodeTypeImpl(ntdata, typesManager, this, locationFactory, valueFactory));

      return ec;
   }

   /**
    * Returns an iterator over all available mixin node types.
    * 
    * @return An <code>NodeTypeIterator</code>.
    * @throws RepositoryException if an error occurs.
    */
   public NodeTypeIterator getMixinNodeTypes() throws RepositoryException
   {
      List<NodeTypeData> allNodeTypes = typesManager.getAllNodeTypes();

      Collections.sort(allNodeTypes, new NodeTypeDataComparator());

      EntityCollection ec = new EntityCollection();
      for (NodeTypeData nodeTypeData : allNodeTypes)
      {
         if (nodeTypeData.isMixin())
            ec.add(new NodeTypeImpl(nodeTypeData, typesManager, this, locationFactory, valueFactory));

      }
      return ec;
   }

   /**
    * {@inheritDoc}
    */
   public NodeType getNodeType(final String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException
   {
      NodeTypeData ntdata = typesManager.getNodeType(locationFactory.parseJCRName(nodeTypeName).getInternalName());
      if (ntdata != null)
         return new NodeTypeImpl(ntdata, typesManager, this, locationFactory, valueFactory);

      throw new NoSuchNodeTypeException("Nodetype not found " + nodeTypeName);
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeDataManager getNodeTypesHolder() throws RepositoryException
   {
      return typesManager;
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeValue getNodeTypeValue(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException
   {
      NodeTypeData ntdata = typesManager.getNodeType(locationFactory.parseJCRName(nodeTypeName).getInternalName());
      if (ntdata != null)
      {
         NodeTypeValue nodeTypeValue = new NodeTypeValue();
         nodeTypeValue.setMixin(ntdata.isMixin());
         nodeTypeValue.setName(locationFactory.createJCRName(ntdata.getName()).getAsString());
         nodeTypeValue.setOrderableChild(ntdata.hasOrderableChildNodes());
         if (ntdata.getPrimaryItemName() == null)
         {
            nodeTypeValue.setPrimaryItemName("");
         }
         else
         {
            nodeTypeValue.setPrimaryItemName(locationFactory.createJCRName(ntdata.getPrimaryItemName()).getAsString());
         }
         List<String> declaredSupertypeNames = new ArrayList<String>();
         for (int i = 0; i < ntdata.getDeclaredSupertypeNames().length; i++)
         {
            declaredSupertypeNames.add(locationFactory.createJCRName(ntdata.getDeclaredSupertypeNames()[i])
               .getAsString());
         }
         List<PropertyDefinitionValue> declaredPropertyDefinitionValues = new ArrayList<PropertyDefinitionValue>();

         for (int i = 0; i < ntdata.getDeclaredPropertyDefinitions().length; i++)
         {
            declaredPropertyDefinitionValues.add(convert(ntdata.getDeclaredPropertyDefinitions()[i]));
         }

         List<NodeDefinitionValue> declaredChildNodeDefinitionValues = new ArrayList<NodeDefinitionValue>();

         for (int i = 0; i < ntdata.getDeclaredChildNodeDefinitions().length; i++)
         {
            declaredChildNodeDefinitionValues.add(convert(ntdata.getDeclaredChildNodeDefinitions()[i]));
         }

         nodeTypeValue.setDeclaredSupertypeNames(declaredSupertypeNames);
         nodeTypeValue.setDeclaredPropertyDefinitionValues(declaredPropertyDefinitionValues);
         nodeTypeValue.setDeclaredChildNodeDefinitionValues(declaredChildNodeDefinitionValues);
         return nodeTypeValue;
      }

      throw new NoSuchNodeTypeException("Nodetype not found " + nodeTypeName);

   }

   /**
    * Returns an iterator over all available primary node types.
    * 
    * @return An <code>NodeTypeIterator</code>.
    * @throws RepositoryException if an error occurs.
    */
   public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException
   {
      EntityCollection ec = new EntityCollection();
      NodeTypeIterator allTypes = getAllNodeTypes();
      while (allTypes.hasNext())
      {
         NodeType type = allTypes.nextNodeType();
         if (!type.isMixin())
            ec.add(type);
      }
      return ec;
   }

   public boolean hasNodeType(String name) throws RepositoryException
   {

      return typesManager.getNodeType(locationFactory.parseJCRName(name).getInternalName()) != null;
   }

   /**
    * {@inheritDoc}
    */
   public void registerNodeType(NodeType nodeType, int alreadyExistsBehaviour) throws RepositoryException
   {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    * 
    * @return
    */
   public NodeType registerNodeType(NodeTypeValue nodeTypeValue, int alreadyExistsBehaviour) throws RepositoryException
   {
      List<NodeTypeValue> list = new ArrayList<NodeTypeValue>();
      list.add(nodeTypeValue);
      NodeTypeIterator it = registerNodeTypes(list, alreadyExistsBehaviour);
      return it.nextNodeType();
   }

   public NodeTypeIterator registerNodeTypes(List<NodeTypeValue> values, int alreadyExistsBehaviour)
      throws UnsupportedRepositoryOperationException, RepositoryException
   {

      Collection<NodeTypeData> nts = typesManager.registerNodeTypes(values, alreadyExistsBehaviour);
      EntityCollection types = new EntityCollection();
      for (NodeTypeData ntdata : nts)
         types.add(new NodeTypeImpl(ntdata, typesManager, this, locationFactory, valueFactory));

      return types;
   }

   /**
    * {@inheritDoc}
    * 
    * @return
    */
   public NodeTypeIterator registerNodeTypes(InputStream xml, int alreadyExistsBehaviour, String contentType)
      throws RepositoryException
   {

      Collection<NodeTypeData> nts = typesManager.registerNodeTypes(xml, alreadyExistsBehaviour, contentType);
      EntityCollection types = new EntityCollection();
      for (NodeTypeData ntdata : nts)
         types.add(new NodeTypeImpl(ntdata, typesManager, this, locationFactory, valueFactory));

      return types;
   }

   /**
    * {@inheritDoc}
    * 
    * @return
    * @deprecated use   registerNodeTypes(InputStream xml, int alreadyExistsBehaviour, String contentType)
    */
   @Deprecated
   public NodeTypeIterator registerNodeTypes(InputStream xml, int alreadyExistsBehaviour) throws RepositoryException
   {

      Collection<NodeTypeData> nts =
         typesManager.registerNodeTypes(xml, alreadyExistsBehaviour, NodeTypeDataManager.TEXT_XML);
      EntityCollection types = new EntityCollection();
      for (NodeTypeData ntdata : nts)
         types.add(new NodeTypeImpl(ntdata, typesManager, this, locationFactory, valueFactory));

      return types;
   }

   /**
    * {@inheritDoc}
    */
   public void unregisterNodeType(String name) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException,
      RepositoryException
   {
      InternalQName nodeTypeName = locationFactory.parseJCRName(name).getInternalName();
      if (typesManager.getNodeType(nodeTypeName) == null)
         throw new NoSuchNodeTypeException(name);
      typesManager.unregisterNodeType(nodeTypeName);
   }

   /**
    * {@inheritDoc}
    */
   public void unregisterNodeTypes(String[] names) throws UnsupportedRepositoryOperationException,
      NoSuchNodeTypeException, RepositoryException
   {
      for (int i = 0; i < names.length; i++)
      {
         unregisterNodeType(names[i]);
      }

   }

   private NodeDefinitionValue convert(NodeDefinitionData data) throws RepositoryException
   {
      NodeDefinitionValue value = new NodeDefinitionValue();

      value.setName((locationFactory.createJCRName(data.getName()).getAsString()));

      value.setAutoCreate(data.isAutoCreated());
      value.setMandatory(data.isMandatory());
      value.setOnVersion(data.getOnParentVersion());
      value.setReadOnly(data.isProtected());

      value.setSameNameSiblings(data.isAllowsSameNameSiblings());
      value.setDefaultNodeTypeName(locationFactory.createJCRName(data.getDeclaringNodeType()).getAsString());

      List<String> requiredNodeTypeNames = new ArrayList<String>();

      for (int i = 0; i < data.getRequiredPrimaryTypes().length; i++)
      {
         requiredNodeTypeNames.add(locationFactory.createJCRName(data.getRequiredPrimaryTypes()[i]).getAsString());
      }
      value.setRequiredNodeTypeNames(requiredNodeTypeNames);

      return value;
   }

   private PropertyDefinitionValue convert(PropertyDefinitionData data) throws RepositoryException
   {
      PropertyDefinitionValue value = new PropertyDefinitionValue();

      value.setName((locationFactory.createJCRName(data.getName()).getAsString()));

      value.setAutoCreate(data.isAutoCreated());
      value.setMandatory(data.isMandatory());
      value.setOnVersion(data.getOnParentVersion());
      value.setReadOnly(data.isProtected());

      value.setRequiredType(data.getRequiredType());
      value.setMultiple(data.isMultiple());

      value.setDefaultValueStrings(Arrays.asList(data.getDefaultValues()));
      value.setValueConstraints(Arrays.asList(data.getValueConstraints()));

      return value;
   }

   /**
    * Comparator
    * 
    * @author sj
    */
   private class NodeTypeDataComparator implements Comparator<NodeTypeData>
   {

      private static final int NT = 4;

      private static final int MIX = 3;

      private static final int JCR = 2;

      private static final int EXO = 1;

      private static final int OTHER = 0;

      /**
       * @param o1
       * @param o2
       * @return
       */
      public int compare(NodeTypeData o1, NodeTypeData o2)
      {

         return getIndex(o2) - getIndex(o1);
      }

      private int getIndex(NodeTypeData data)
      {
         InternalQName name;
         int result = OTHER;
         name = data.getName();
         String nameSpace = name.getNamespace();
         if (Constants.NS_NT_URI.equals(nameSpace))
            result = NT;
         else if (Constants.NS_MIX_URI.equals(nameSpace))
            result = MIX;
         else if (Constants.NS_JCR_URI.equals(nameSpace))
            result = JCR;
         else if (Constants.NS_EXO_URI.equals(nameSpace))
            result = EXO;
         return result;
      }
   }
}
