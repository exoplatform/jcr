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

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeRepository;
import org.exoplatform.services.jcr.impl.core.value.ValueConstraintsValidator;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: NodeTypeDataValidator.java 2430 2010-05-21 12:21:59Z pnedonosko $
 */
public class NodeTypeDataValidator
{
   /**
    * Class logger.
    */
   private final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeDataValidator");

   protected final NodeTypeRepository hierarchy;

   protected final ValueFactoryImpl valueFactory;
   
   protected class ValidatorValueFactory extends ValueFactoryImpl
   {
      ValidatorValueFactory(LocationFactory locationFactory)
      {
         super(locationFactory);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Value createValue(String value, int type) throws ValueFormatException
      {
         if (type == PropertyType.BINARY)
         {
            LOG.warn("Not supported Value type: BINARY");
            return null;
         }

         return super.createValue(value, type);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Value createValue(InputStream value)
      {
         LOG.warn("Not supported Value type: BINARY");
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Value loadValue(ValueData data, int type) throws RepositoryException
      {
         if (type == PropertyType.BINARY)
         {
            LOG.warn("Not supported Value type: BINARY");
            return null;
         }

         return super.loadValue(data, type);
      }
   }

   public NodeTypeDataValidator(LocationFactory locationFactory, NodeTypeRepository hierarchy)
   {
      this.hierarchy = hierarchy;

      this.valueFactory = new ValidatorValueFactory(locationFactory); 
   }

   public void validateNodeType(List<NodeTypeData> nodeTypeDataList) throws RepositoryException
   {
      for (NodeTypeData nodeTypeData : nodeTypeDataList)
      {
         validateNodeType(nodeTypeData);
      }
      checkCyclicDependencies(nodeTypeDataList);
   }

   private void checkCyclicDependencies(List<NodeTypeData> nodeTypeDataList) throws RepositoryException
   {
      Set<InternalQName> unresolvedDependecies = new HashSet<InternalQName>();
      Set<InternalQName> resolvedDependecies = new HashSet<InternalQName>();
      for (NodeTypeData nodeTypeData : nodeTypeDataList)
      {
         // / add itself
         resolvedDependecies.add(nodeTypeData.getName());
         // remove from unresolved
         unresolvedDependecies.remove(nodeTypeData.getName());
         // check suppers
         for (int i = 0; i < nodeTypeData.getDeclaredSupertypeNames().length; i++)
         {
            InternalQName superName = nodeTypeData.getDeclaredSupertypeNames()[i];
            if (hierarchy.getNodeType(superName) == null && !resolvedDependecies.contains(superName))
            {
               unresolvedDependecies.add(superName);
            }
         }
         // check node definition
         for (int i = 0; i < nodeTypeData.getDeclaredChildNodeDefinitions().length; i++)
         {
            NodeDefinitionData childnodeDefinitionData = nodeTypeData.getDeclaredChildNodeDefinitions()[i];
            for (int j = 0; j < childnodeDefinitionData.getRequiredPrimaryTypes().length; j++)
            {
               InternalQName requiredPrimaryTypeName = childnodeDefinitionData.getRequiredPrimaryTypes()[j];
               if (hierarchy.getNodeType(requiredPrimaryTypeName) == null
                  && !resolvedDependecies.contains(requiredPrimaryTypeName))
               {
                  unresolvedDependecies.add(requiredPrimaryTypeName);
               }
            }
            if (childnodeDefinitionData.getDefaultPrimaryType() != null)
            {
               if (hierarchy.getNodeType(childnodeDefinitionData.getDefaultPrimaryType()) == null
                  && !resolvedDependecies.contains(childnodeDefinitionData.getDefaultPrimaryType()))
               {
                  unresolvedDependecies.add(childnodeDefinitionData.getDefaultPrimaryType());
               }
            }
         }
      }
      if (unresolvedDependecies.size() > 0)
      {
         String msg = "Fail. Unresolved cyclic dependecy for :";
         for (InternalQName internalQName : resolvedDependecies)
         {
            msg += " " + internalQName.getAsString();
         }

         msg += " Unresolved ";
         for (InternalQName internalQName : unresolvedDependecies)
         {
            msg += " " + internalQName.getAsString();
         }

         throw new RepositoryException(msg);
      }
   }

   /**
    * Check according the JSR-170
    */
   private void validateNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      if (nodeType == null)
      {
         throw new RepositoryException("NodeType object " + nodeType + " is null");
      }

      if (nodeType.getName() == null)
      {
         throw new RepositoryException("NodeType implementation class " + nodeType.getClass().getName()
            + " is not supported in this method");
      }

      for (InternalQName sname : nodeType.getDeclaredSupertypeNames())
      {
         if (!nodeType.getName().equals(Constants.NT_BASE) && nodeType.getName().equals(sname))
         {
            throw new RepositoryException("Invalid super type name" + sname.getAsString());
         }
      }

      for (PropertyDefinitionData pdef : nodeType.getDeclaredPropertyDefinitions())
      {
         if (!pdef.getDeclaringNodeType().equals(nodeType.getName()))
         {
            throw new RepositoryException("Invalid declared node type in property definitions with name "
               + pdef.getName().getAsString() + " not registred");
         }
         
         // validate default values
         try
         {
            validateValueDefaults(pdef.getRequiredType(), pdef.getDefaultValues());
         }
         catch (ValueFormatException e)
         {
            throw new ValueFormatException("Default value is incompatible with Property type "
               + PropertyType.nameFromValue(pdef.getRequiredType()) + " of " + pdef.getName().getAsString()
               + " in nodetype " + nodeType.getName().getAsString(), e);
         }
         
         // TODO validate constraints, we have issue with TCK nodetype tests:canSetProperty
         try
         {
            validateValueConstraints(pdef.getRequiredType(), pdef.getValueConstraints());
         }
         catch (ValueFormatException e)
         {
            throw new ValueFormatException("Constraints is incompatible with Property type "
               + PropertyType.nameFromValue(pdef.getRequiredType()) + " of " + pdef.getName().getAsString()
               + " in nodetype " + nodeType.getName().getAsString(), e);
         }
      }

      for (NodeDefinitionData cndef : nodeType.getDeclaredChildNodeDefinitions())
      {
         if (!cndef.getDeclaringNodeType().equals(nodeType.getName()))
         {
            throw new RepositoryException("Invalid declared node type in child node definitions with name "
               + cndef.getName().getAsString() + " not registred");
         }
      }
   }

   private void validateValueDefaults(int requiredType, String[] defValues) throws ValueFormatException
   {
      if (requiredType != PropertyType.STRING && requiredType != PropertyType.UNDEFINED && requiredType != PropertyType.BINARY)
      {
         for (String dv : defValues)
         {
            valueFactory.createValue(dv, requiredType);
         }
      }
   }
   
   private void validateValueConstraints(int requiredType, String[] constraints) throws ValueFormatException
   {
      ValueConstraintsValidator validator = new ValueConstraintsValidator(constraints);
      validator.validateFor(requiredType);
   }
}
