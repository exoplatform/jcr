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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.nodetype.PropertyDefinitionImpl;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: PropertyImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class PropertyImpl extends ItemImpl implements Property
{

   /**
    * Value type.
    */
   protected int type;

   private PropertyDefinitionData propertyDef;

   private PropertyData propertyData;

   /**
    * PropertyImpl constructor.
    * 
    * @param data
    *          ItemData object
    * @param session
    *          Session object
    * @throws RepositoryException
    * @throws ConstraintViolationException
    */
   PropertyImpl(ItemData data, SessionImpl session) throws RepositoryException, ConstraintViolationException
   {
      super(data, session);
      loadData(data);
   }

   /**
    * PropertyImpl constructor.
    * 
    * @param data
    *          ItemData object
    * @param parent NodeData Property's parent
    * @param session
    *          Session object
    * @throws RepositoryException
    * @throws ConstraintViolationException
    */
   PropertyImpl(ItemData data, NodeData parent, SessionImpl session) throws RepositoryException,
      ConstraintViolationException
   {
      super(data, session);
      loadData(data, parent);
   }

   /**
    * {@inheritDoc}
    */
   void loadData(ItemData data) throws RepositoryException, ConstraintViolationException
   {
      loadData(data, (NodeData)null);
   }

   /**
    * {@inheritDoc}
    */
   void loadData(ItemData data, NodeData parent) throws RepositoryException, ConstraintViolationException
   {
      if (data.isNode())
      {
         throw new RepositoryException("Load data failed: Property expected");
      }

      this.data = data;
      this.propertyData = (PropertyData)data;
      this.type = propertyData.getType();

      this.qpath = data.getQPath();
      this.location = null;

      initDefinitions(this.propertyData.isMultiValued(), parent);
   }

   /**
    * {@inheritDoc}
    */
   @Deprecated
   void loadData(ItemData data, ItemDefinitionData itemDefinitionData) throws RepositoryException,
      ConstraintViolationException
   {
      this.data = data;
      this.propertyData = (PropertyData)data;
      this.type = propertyData.getType();

      this.location = null;
      this.qpath = data.getQPath();
      this.propertyDef = (PropertyDefinitionData)itemDefinitionData;
   }

   /**
    * {@inheritDoc}
    */
   public ItemDefinitionData getItemDefinitionData()
   {
      return propertyDef;
   }

   /**
    * {@inheritDoc}
    */
   public Value getValue() throws ValueFormatException, RepositoryException
   {

      checkValid();

      if (isMultiValued())
      {
         throw new ValueFormatException("The property " + getPath() + " is multi-valued (6.2.4)");
      }

      if (propertyData.getValues() != null && propertyData.getValues().size() == 0)
      {
         throw new ValueFormatException("The single valued property " + getPath() + " is empty");
      }

      return valueFactory.loadValue(propertyData.getValues().get(0), propertyData.getType());

   }

   /**
    * {@inheritDoc}
    */
   public Value[] getValues() throws ValueFormatException, RepositoryException
   {

      checkValid();

      // Check property definition and life-state flag both
      if (!isMultiValued())
      {
         throw new ValueFormatException("The property " + getPath() + " is single-valued (6.2.4)");
      }

      // The array returned is a copy of the stored values
      return getValueArray();
   }

   /**
    * {@inheritDoc}
    */
   public String getString() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getValue().getString();
      }
      catch (ValueFormatException e)
      {
         throw new ValueFormatException("PropertyImpl.getString() for " + getPath() + " failed: " + e);
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getString() for " + getPath() + " failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public double getDouble() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getValue().getDouble();
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getDouble() failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getLong() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getValue().getLong();
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getLong() failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getStream() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getValue().getStream();
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getStream() failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getDate() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getValue().getDate();
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getDate() failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean getBoolean() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getValue().getBoolean();
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getBoolean() failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Node getNode() throws ValueFormatException, RepositoryException
   {
      try
      {
         String identifier = ((BaseValue)getValue()).getReference();
         return session.getNodeByUUID(identifier);
      }
      catch (IllegalStateException e)
      {
         throw new ValueFormatException("PropertyImpl.getNode() failed: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getLength() throws ValueFormatException, RepositoryException
   {

      return ((BaseValue)getValue()).getLength();
   }

   /**
    * {@inheritDoc}
    */
   public long[] getLengths() throws ValueFormatException, RepositoryException
   {

      Value[] thisValues = getValues();

      long[] lengths = new long[thisValues.length];
      for (int i = 0; i < lengths.length; i++)
      {
         lengths[i] = ((BaseValue)thisValues[i]).getLength();
      }
      return lengths;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinition getDefinition() throws RepositoryException
   {

      checkValid();

      if (propertyDef == null)
      {
         throw new RepositoryException("FATAL: property definition is NULL " + getPath() + " "
            + propertyData.getValues());
      }
      String name =
         locationFactory.createJCRName(propertyDef.getName() != null ? propertyDef.getName() : Constants.JCR_ANY_NAME)
            .getAsString();
      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();

      Value[] defaultValues = new Value[propertyDef.getDefaultValues().length];
      String[] propVal = propertyDef.getDefaultValues();
      // there can be null in definition but should not be null value
      if (propVal != null)
      {
         for (int i = 0; i < propVal.length; i++)
         {
            if (propertyDef.getRequiredType() == PropertyType.UNDEFINED)
               defaultValues[i] = valueFactory.createValue(propVal[i]);
            else
               defaultValues[i] = valueFactory.createValue(propVal[i], propertyDef.getRequiredType());
         }
      }

      return new PropertyDefinitionImpl(propertyDef, session.getWorkspace().getNodeTypesHolder(),
         (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager(), session.getSystemLocationFactory(),
         session.getValueFactory());

   }

   /**
    * @param multiple
    * @param parent
    * @throws RepositoryException
    * @throws ConstraintViolationException
    */
   private void initDefinitions(boolean multiple, NodeData parent) throws RepositoryException,
      ConstraintViolationException
   {

      InternalQName pname = getData().getQPath().getName();

      if (parent == null)
      {
         parent = parentData();
      }

      PropertyDefinitionDatas definitions =
         session.getWorkspace().getNodeTypesHolder().getPropertyDefinitions(pname, parent.getPrimaryTypeName(),
            parent.getMixinTypeNames());

      if (definitions == null)
      {
         throw new ConstraintViolationException("Definition for property " + getPath() + " not found.");
      }

      propertyDef = definitions.getDefinition(multiple);
   }

   /**
    * {@inheritDoc}
    */
   public int getType()
   {
      return type;
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(Value value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      doUpdateProperty(parent(), getInternalName(), value, false, PropertyType.UNDEFINED);
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      doUpdateProperty(parent(), getInternalName(), values, true, PropertyType.UNDEFINED);
   }

   /**
    * Check if property is multi valued.
    * 
    * @return multiValued property of data field (PropertyData) it's a life-state property field
    *         which contains multiple-valued flag for value(s) data. Can be set in property creation
    *         time or from persistent storage.
    */
   public boolean isMultiValued()
   {
      return ((PropertyData)data).isMultiValued();
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(String value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(value));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(InputStream stream) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(stream));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(double number) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(number));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(long number) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(number));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(Calendar date) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(date));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(boolean b) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(b));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(Node value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      setValue(valueFactory.createValue(value));
   }

   /**
    * {@inheritDoc}
    */
   public void setValue(String[] values) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      Value[] strValues = null;
      if (values != null)
      {
         strValues = new Value[values.length];
         for (int i = 0; i < values.length; i++)
            strValues[i] = valueFactory.createValue(values[i]);
      }
      setValue(strValues);
   }

   // ////////////////// Item implementation ////////////////////

   /**
    * {@inheritDoc}
    */
   public void accept(ItemVisitor visitor) throws RepositoryException
   {
      checkValid();

      visitor.visit(this);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return false;
   }

   // // ----------------------- ExtendedProperty -----------------------
   //
   // public void updateValue(int index, InputStream value, long length,
   // long position) throws ValueFormatException, VersionException,
   // LockException, ConstraintViolationException, RepositoryException {
   //
   // checkValid();
   //    
   // PropertyData pdata = (PropertyData) getData();
   // TransientValueData vdata = (TransientValueData)
   // pdata.getValues().get(index);
   //
   // doUpdateProperty(parent(), getInternalName(),
   // valueFactory.loadValue(vdata, PropertyType.BINARY), false,
   // PropertyType.BINARY);
   //    
   // // get new data
   // vdata = (TransientValueData) ((PropertyData)
   // getData()).getValues().get(index);
   //    
   // try {
   // vdata.update(value, length, position);
   // } catch (IOException e) {
   // throw new RepositoryException(e);
   // }
   //    
   // //setValue(valueFactory.loadValue(vdata, PropertyType.BINARY));
   // }

   // ////////////////////////////////

   /**
    * Copies property values into array.
    * 
    * @return array of property values
    * @throws RepositoryException
    *           if any Exception is occurred
    */
   public Value[] getValueArray() throws RepositoryException
   {

      Value[] values = new Value[propertyData.getValues().size()];
      for (int i = 0; i < values.length; i++)
      {
         values[i] = valueFactory.loadValue(propertyData.getValues().get(i), propertyData.getType());
      }
      return values;
   }

   /**
    * Get info about property values.
    * 
    * @return string with property values
    */
   public String dump()
   {
      String vals = "Property ";
      try
      {
         vals = getPath() + " values: ";
         for (int i = 0; i < getValueArray().length; i++)
         {
            vals += new String(((BaseValue)getValueArray()[i]).getInternalData().getAsByteArray()) + ";";
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return vals;
   }

   // ----------------------- Object -----------------------

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof PropertyImpl)
      {
         try
         {
            // by path
            return getLocation().equals(((PropertyImpl)obj).getLocation());
         }
         catch (Exception e)
         {
            return false;
         }
      }
      return false;
   }

}
