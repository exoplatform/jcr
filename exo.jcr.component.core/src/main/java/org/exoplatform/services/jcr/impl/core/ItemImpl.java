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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;
import org.exoplatform.services.jcr.impl.core.value.PermissionValue;
import org.exoplatform.services.jcr.impl.core.value.ValueConstraintsMatcher;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.EditableValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */
public abstract class ItemImpl implements Item
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemImpl");

   /**
    * Session object.
    */
   protected final SessionImpl session;

   /**
    * ItemData object.
    */
   protected ItemData data;

   /**
    * Item JCRPath.
    */
   protected JCRPath location;

   /**
    * Item QPath.
    */
   protected QPath qpath;

   protected SessionDataManager dataManager;

   protected LocationFactory locationFactory;

   protected ValueFactoryImpl valueFactory;

   /**
    * ItemImpl constructor.
    * 
    * @param data ItemData object
    * @param session Session object
    * @throws RepositoryException if any Exception is occurred
    */
   ItemImpl(ItemData data, SessionImpl session) throws RepositoryException
   {

      this.session = session;
      this.data = data;

      this.dataManager = session.getTransientNodesManager();
      this.locationFactory = session.getLocationFactory();
      this.valueFactory = session.getValueFactory();
   }

   protected void invalidate()
   {
      this.data = null;
   }

   /**
    * Return a status of the item state. If the state is invalid the item can't
    * be used anymore.
    * 
    * @return boolean flag, true if an item is usable in the session.
    */
   public boolean isValid()
   {
      return data != null;
   }

   /**
    * Checking if this item has valid item state, i.e. wasn't removed (and
    * saved).
    * 
    * @return true or throws an InvalidItemStateException exception otherwise
    * @throws InvalidItemStateException
    */
   protected boolean checkValid() throws InvalidItemStateException
   {
      try
      {
         session.checkLive();
      }
      catch (RepositoryException e)
      {
         throw new InvalidItemStateException("This kind of operation is forbidden after a session.logout().", e);
      }

      if (data == null)
      {
         throw new InvalidItemStateException("Invalid item state. Item was removed or discarded.");
      }

      session.updateLastAccessTime();
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public String getPath() throws RepositoryException
   {
      return getLocation().getAsString(false);
   }

   /**
    * {@inheritDoc}
    */
   public String getName() throws RepositoryException
   {
      return getLocation().getName().getAsString();
   }

   /**
    * {@inheritDoc}
    */
   public Item getAncestor(int degree) throws ItemNotFoundException, AccessDeniedException, RepositoryException
   {
      checkValid();
      try
      {
         // 6.2.8 If depth > n is specified then an ItemNotFoundException is
         // thrown.
         if (degree < 0)
         {
            throw new ItemNotFoundException("Can't get ancestor with ancestor's degree < 0.");
         }

         final QPath myPath = getData().getQPath();
         int n = myPath.getDepth() - degree;
         if (n == 0)
         {
            return this;
         }
         else if (n < 0)
         {
            throw new ItemNotFoundException("Can't get ancestor with ancestor's degree > depth of this item.");
         }
         else
         {
            final QPath ancestorPath = myPath.makeAncestorPath(n);
            return dataManager.getItem(ancestorPath, true);
         }
      }
      catch (PathNotFoundException e)
      {
         throw new ItemNotFoundException(e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeImpl getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException
   {

      checkValid();

      if (isRoot())
      {
         throw new ItemNotFoundException("Root node does not have a parent");
      }

      return parent(true);
   }

   /**
    * {@inheritDoc}
    */
   public SessionImpl getSession()
   {
      return session;
   }

   /**
    * {@inheritDoc}
    */
   public int getDepth()
   {
      return qpath.getDepth();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSame(Item otherItem)
   {

      if (isValid())
      {
         if (otherItem == null)
         {
            return false;
         }

         if (!this.getClass().equals(otherItem.getClass()))
         {
            return false;
         }

         try
         {

            // identifier is not changed on ItemImpl
            return getInternalIdentifier().equals(((ItemImpl)otherItem).getInternalIdentifier());
         }
         catch (Exception e)
         {
            LOG.debug("Item.isSame() failed " + e.getMessage());
            return false;
         }
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNew()
   {
      if (isValid())
      {
         return dataManager.isNew(getInternalIdentifier());
      }

      // if was removed (is invalid by check), isn't new
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isModified()
   {
      if (isValid())
      {
         return dataManager.isModified(getData());
      }

      // if was removed (is invalid by check), was modified
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void remove() throws RepositoryException, ConstraintViolationException, VersionException, LockException
   {

      checkValid();

      if (isRoot())
      {
         throw new RepositoryException("Can't remove ROOT node.");
      }

      // Check constraints
      ItemDefinition def;
      if (isNode())
      {
         def = ((NodeImpl)this).getDefinition();
      }
      else
      {
         def = ((PropertyImpl)this).getDefinition();
      }

      if (def.isMandatory() || def.isProtected())
      {
         throw new ConstraintViolationException("Can't remove mandatory or protected item " + getPath());
      }

      NodeImpl parentNode = parent();
      // Check if versionable ancestor is not checked-in
      if (!parentNode.checkedOut())
      {
         throw new VersionException("Node " + parent().getPath() + " or its nearest ancestor is checked-in");
      }

      // Check locking
      if (!parentNode.checkLocking())
      {
         throw new LockException("Node " + parent().getPath() + " is locked ");
      }

      // launch event
      session.getActionHandler().preRemoveItem(this);

      removeVersionable();

      // remove from datamanager
      dataManager.delete(data);
   }

   /**
    * Check when it's a Node and is versionable will a version history removed.
    * Case of last version in version history.
    * 
    * @throws RepositoryException
    * @throws ConstraintViolationException
    * @throws VersionException
    */
   protected void removeVersionable() throws RepositoryException, ConstraintViolationException, VersionException
   {
      if (isNode())
      {
         NodeTypeDataManager ntManager = session.getWorkspace().getNodeTypesHolder();
         NodeData node = (NodeData)data;
         if (ntManager.isNodeType(Constants.MIX_VERSIONABLE, node.getPrimaryTypeName(), node.getMixinTypeNames()))
         {

            ItemData vhpd =
               dataManager.getItemData(node, new QPathEntry(Constants.JCR_VERSIONHISTORY, 1), ItemType.PROPERTY);
            if (vhpd != null && !vhpd.isNode())
            {
               String vhID = ValueDataUtil.getString(((PropertyData)vhpd).getValues().get(0));
               dataManager.removeVersionHistory(vhID, null, data.getQPath());
            }
            else
            {
               throw new RepositoryException("Version history not found for " + node.getQPath().getAsString());
            }
         }
      }
   }

   protected PropertyImpl doUpdateProperty(NodeImpl parentNode, InternalQName propertyName, Value propertyValue,
      boolean multiValue, int expectedType) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      Value[] val = null;
      if (propertyValue != null)
      {
         val = new Value[]{propertyValue};
      }
      return doUpdateProperty(parentNode, propertyName, val, multiValue, expectedType);
   }

   protected PropertyImpl doUpdateProperty(NodeImpl parentNode, InternalQName propertyName, Value[] propertyValues,
      boolean multiValue, int expectedType) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {
      boolean persistedParent = !parentNode.isNew();

      // Check if checked-in (versionable)
      if (persistedParent && !parentNode.checkedOut())
      {
         throw new VersionException("Node " + parentNode.getPath() + " or its nearest ancestor is checked-in");
      }

      // Check is locked
      if (persistedParent && !parentNode.checkLocking())
      {
         throw new LockException("Node " + parentNode.getPath() + " is locked ");
      }

      QPath qpath = QPath.makeChildPath(parentNode.getInternalPath(), propertyName);

      int state;

      String identifier;
      int version;
      PropertyImpl prevProp;
      PropertyDefinitionDatas defs;
      ItemImpl prevItem =
         dataManager.getItem(parentNode.nodeData(), new QPathEntry(propertyName, 0), true,
            dataManager.isNew(parentNode.getIdentifier()), ItemType.PROPERTY, false);

      NodeTypeDataManager ntm = session.getWorkspace().getNodeTypesHolder();
      NodeData parentData = (NodeData)parentNode.getData();
      boolean isMultiValue = multiValue;
      PropertyImpl prevProperty = null;
      if (prevItem == null || prevItem.isNode())
      { // new property
         identifier = IdGenerator.generate();
         version = -1;
         if (propertyValues == null)
         {
            // new property null values;
            TransientPropertyData nullData =
               new TransientPropertyData(qpath, identifier, version, PropertyType.UNDEFINED,
                  parentNode.getInternalIdentifier(), isMultiValue);
            PropertyImpl nullProperty = new PropertyImpl(nullData, session);
            nullProperty.invalidate();
            return nullProperty;
         }
         defs =
            ntm.getPropertyDefinitions(propertyName, parentData.getPrimaryTypeName(), parentData.getMixinTypeNames());
         prevProp = null;
         state = ItemState.ADDED;
      }
      else
      {
         // update of the property
         prevProp = (PropertyImpl)prevItem;
         prevProperty = new AuditPropertyImpl(prevProp.getData(), prevProp.getSession());
         isMultiValue = prevProp.isMultiValued();
         defs =
            ntm.getPropertyDefinitions(propertyName, parentData.getPrimaryTypeName(), parentData.getMixinTypeNames());

         identifier = prevProp.getInternalIdentifier();
         version = prevProp.getData().getPersistedVersion();
         if (propertyValues == null)
         {
            state = ItemState.DELETED;
         }
         else
         {
            state = ItemState.UPDATED;
         }
      }

      if (defs == null || defs.getAnyDefinition() == null)
      {
         throw new ConstraintViolationException("Property definition '" + propertyName.getAsString()
            + "' is not found.");
      }

      PropertyDefinitionData def = defs.getDefinition(isMultiValue);
      if (def != null && def.isProtected())
      {
         throw new ConstraintViolationException("Can not set protected property "
            + locationFactory.createJCRPath(qpath).getAsString(false));
      }

      if (multiValue && (def == null || (prevProp != null && !prevProp.isMultiValued())))
      {
         throw new ValueFormatException("Can not assign multiple-values Value to a single-valued property "
            + locationFactory.createJCRPath(qpath).getAsString(false));
      }

      if (!multiValue && (def == null || (prevProp != null && prevProp.isMultiValued())))
      {
         throw new ValueFormatException("Can not assign single-value Value to a multiple-valued property "
            + locationFactory.createJCRPath(qpath).getAsString(false));
      }

      List<ValueData> valueDataList = new ArrayList<ValueData>();

      // cast to required type if necessary
      int requiredType = def.getRequiredType();

      int propType = requiredType;
      // if list of values not null
      if (propertyValues != null)
      {
         // All Value objects in the array must be of the same type, otherwise a
         // ValueFormatException is thrown.
         if (propertyValues.length > 1)
         {
            if (propertyValues[0] != null)
            {
               int vType = propertyValues[0].getType();
               for (Value val : propertyValues)
               {
                  if (val != null && vType != val.getType())
                  {
                     throw new ValueFormatException("All Value objects in the array must be of the same type");
                  }
               }
            }
         }

         // if value count >0 and original type not UNDEFINED
         if (propertyValues.length > 0 && requiredType == PropertyType.UNDEFINED)
         {
            // if type what we expected to be UNDEFINED
            // set destination type = type of values else type expectedType
            if (expectedType == PropertyType.UNDEFINED)
            {
               for (Value val : propertyValues)
               {
                  if (val != null)
                  {
                     expectedType = val.getType();
                     break;
                  }
               }
            }
            propType = expectedType;
         }
         // fill datas and also remove null values and reorder values
         for (Value value : propertyValues)
         {
            if (value != null)
            {
               valueDataList.add(valueData(value, propType));
            }
            else
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Set null value (" + getPath() + ", multivalued: " + multiValue + ")");
               }
            }
         }
      }

      // Check value constraints
      checkValueConstraints(def, valueDataList, propType);

      if (requiredType != PropertyType.UNDEFINED && expectedType != PropertyType.UNDEFINED
         && requiredType != expectedType)
      {
         throw new ConstraintViolationException(" the type parameter "
            + ExtendedPropertyType.nameFromValue(expectedType) + " and the "
            + "type of the property do not match required type" + ExtendedPropertyType.nameFromValue(requiredType));
      }

      PropertyImpl prop;
      if (state != ItemState.DELETED)
      {
         // add or update       
         TransientPropertyData newData =
            new TransientPropertyData(qpath, identifier, version, propType, parentNode.getInternalIdentifier(),
               multiValue, valueDataList);

         ItemState itemState = new ItemState(newData, state, true, qpath, false);
         prop = (PropertyImpl)dataManager.update(itemState, true);

         // launch event: post-set 
         session.getActionHandler().postSetProperty(prevProperty, prop, parentNode.nodeData(), state);
      }
      else
      {
         if (def.isMandatory())
         {
            throw new ConstraintViolationException("Can not remove (by setting null value) mandatory property "
               + locationFactory.createJCRPath(qpath).getAsString(false));
         }

         TransientPropertyData newData =
            new TransientPropertyData(qpath, identifier, version, propType, parentNode.getInternalIdentifier(),
               multiValue);

         // launch event: pre-remove
         session.getActionHandler().preRemoveItem(prevProp);

         dataManager.delete(newData);
         prop = prevProp;
      }

      return prop;
   }

   /**
    * {@inheritDoc}
    */
   public void save() throws ReferentialIntegrityException, AccessDeniedException, LockException,
      ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, RepositoryException
   {
      checkValid();

      if (isNew())
      {
         throw new RepositoryException("It is impossible to call save() on the newly added item " + getPath());
      }

      NodeTypeDataManager ntManager = session.getWorkspace().getNodeTypesHolder();

      if (isNode())
      {
         // validate
         // 1. referenceable nodes - if a node is deleted and then added,
         // referential integrity is unchanged ('move' usecase)
         QPath path = getInternalPath();
         List<ItemState> changes = dataManager.getChangesLog().getDescendantsChanges(path);

         List<NodeData> refNodes = new ArrayList<NodeData>();

         for (int i = 0, length = changes.size(); i < length; i++)
         {
            ItemState changedItem = changes.get(i);
            if (changedItem.isNode())
            {
               NodeData refNode = (NodeData)changedItem.getData();

               // Check referential integrity (remove of mix:referenceable node)
               if (ntManager.isNodeType(Constants.MIX_REFERENCEABLE, refNode.getPrimaryTypeName(),
                  refNode.getMixinTypeNames()))
               {

                  // mix:referenceable
                  if (changedItem.isDeleted())
                  {
                     refNodes.add(refNode); // add to refs (delete - alway is first)
                  }
                  else if (changedItem.isAdded() || changedItem.isRenamed())
                  {
                     refNodes.remove(refNode); // remove from refs (add - always at the
                     // end)
                  }
               }
            }
         }

         // check ref changes
         for (int i = 0, length = refNodes.size(); i < length; i++)
         {
            NodeData refNode = refNodes.get(i);
            List<PropertyData> nodeRefs = dataManager.getReferencesData(refNode.getIdentifier(), true);
            for (int j = 0, length2 = nodeRefs.size(); j < length2; j++)
            {
               PropertyData refProp = nodeRefs.get(j);
               // if ref property is deleted in this session
               ItemState refState = dataManager.getChangesLog().getItemState(refProp.getIdentifier());
               if (refState != null && refState.isDeleted())
               {
                  continue;
               }

               NodeData refParent = (NodeData)dataManager.getItemData(refProp.getParentIdentifier());
               AccessControlList acl = refParent.getACL();
               AccessManager am = session.getAccessManager();

               if (!am.hasPermission(acl, PermissionType.READ, session.getUserState().getIdentity()))
               {
                  throw new AccessDeniedException("Can not delete node " + refNode.getQPath() + " ("
                     + refNode.getIdentifier() + ")" + ". It is currently the target of a REFERENCE property and "
                     + refProp.getQPath().getAsString());
               }
               throw new ReferentialIntegrityException("Can not delete node " + refNode.getQPath() + " ("
                  + refNode.getIdentifier() + ")" + ". It is currently the target of a REFERENCE property "
                  + refProp.getQPath().getAsString());
            }
         }
      }

      dataManager.commit(getInternalPath());
   }

   /**
    * {@inheritDoc}
    */
   public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException
   {

      checkValid();

      if (keepChanges)
      {
         dataManager.refresh(this.getData());
      }
      else
      {
         dataManager.rollback(this.getData());
      }
   }

   // -------------------- Implementation ----------------------

   public ItemData getData()
   {
      return data;
   }

   /**
    * Get identifier of parent item.
    * 
    * @return parent identifier
    */
   public String getParentIdentifier()
   {
      return getData().getParentIdentifier();
   }

   public QPath getInternalPath()
   {
      return getData().getQPath();
   }

   public InternalQName getInternalName()
   {
      return getData().getQPath().getName();
   }

   /**
    * Get parent node item. Session pool is ignored.
    * 
    * @return parent item
    * @throws RepositoryException if parent item is null
    */
   protected NodeImpl parent() throws RepositoryException
   {
      return parent(false);
   }

   /**
    * Get parent node item. 
    * 
    * @param pool - take a parent from session pool
    * @return parent item
    * @throws RepositoryException if parent item is null
    */
   protected NodeImpl parent(final boolean pool) throws RepositoryException
   {
      NodeImpl parent = (NodeImpl)dataManager.getItemByIdentifier(getParentIdentifier(), pool);
      if (parent == null)
      {
         throw new ItemNotFoundException("FATAL: Parent is null for " + getPath() + " parent UUID: "
            + getParentIdentifier());
      }
      return parent;
   }

   /**
    * Get and return parent node data.
    * 
    * @return parent node data
    * @throws RepositoryException if parent item is null
    */
   public NodeData parentData() throws RepositoryException
   {
      checkValid();
      NodeData parent = (NodeData)dataManager.getItemData(getData().getParentIdentifier());
      if (parent == null)
      {
         throw new ItemNotFoundException("FATAL: Parent is null for " + getPath() + " parent UUID: "
            + getData().getParentIdentifier());
      }

      return parent;
   }

   protected NodeTypeData[] nodeTypes(NodeData node) throws RepositoryException
   {

      InternalQName primaryTypeName = node.getPrimaryTypeName();
      InternalQName[] mixinNames = node.getMixinTypeNames();
      NodeTypeData[] nodeTypes = new NodeTypeData[mixinNames.length + 1];

      NodeTypeDataManager ntm = session.getWorkspace().getNodeTypesHolder();
      nodeTypes[0] = ntm.getNodeType(primaryTypeName);
      for (int i = 1; i <= mixinNames.length; i++)
      {
         nodeTypes[i] = ntm.getNodeType(mixinNames[i - 1]);
      }

      return nodeTypes;
   }

   public String getInternalIdentifier()
   {
      return data.getIdentifier();
   }

   /**
    * Get item JCRPath location.
    * 
    * @return item JCRPath
    */
   public JCRPath getLocation() throws RepositoryException
   {
      if (this.location == null)
      {
         this.location = session.getLocationFactory().createJCRPath(qpath);
      }

      return this.location;
   }

   /**
    * Check if item is root.
    * 
    * @return true if item is root and false in other case
    */
   public boolean isRoot()
   {
      return data.getIdentifier().equals(Constants.ROOT_UUID);
   }

   /**
    * Loads data.
    * 
    * @param data source item data
    * @throws RepositoryException if errors occurs
    */
   abstract void loadData(ItemData data) throws RepositoryException;

   /**
    * Loads data using existing parent data (used primary and mixin types for
    * Item Definition discovery).
    * 
    * @param data source item data
    * @param parent NodeData Items's parent
    * @throws RepositoryException if errors occurs
    */
   abstract void loadData(ItemData data, NodeData parent) throws RepositoryException;

   /**
    * Returns Item definition data.
    * 
    * @return
    */
   abstract ItemDefinitionData getItemDefinitionData();

   public boolean hasPermission(String action) throws RepositoryException
   {
      checkValid();
      NodeData ndata;
      if (isNode())
      {
         ndata = (NodeData)getData();
      }
      else
      {
         ndata = parentData();
      }

      return session.getAccessManager().hasPermission(ndata.getACL(), action, session.getUserState().getIdentity());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof ItemImpl)
      {
         ItemImpl otherItem = (ItemImpl)obj;

         if (!otherItem.isValid() || !this.isValid())
         {
            return false;
         }

         try
         {
            return getInternalIdentifier().equals(otherItem.getInternalIdentifier());
         }
         catch (Exception e)
         {
            return false;
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    * 
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() 
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((getInternalIdentifier() == null) ? 0 : getInternalIdentifier().hashCode());
      result = prime * result + qpath.hashCode();
      return result;
   }
    
   private ValueData valueData(Value value, int type) throws RepositoryException, ValueFormatException
   {
      if (value == null)
      {
         return null;
      }
      else if (value.getType() == type)
      {
         ValueData internalData = ((BaseValue)value).getInternalData();
         if (!(internalData instanceof EditableValueData))
         {
            return internalData;
         }
      }

      switch (type)
      {
         case PropertyType.STRING :
            return new TransientValueData(value.getString());
         case PropertyType.BINARY :
            if (value instanceof BaseValue)
            {
               return ((BaseValue)getSession().getValueFactory().createValue(value.getStream())).getInternalData();
            }
            else
            {
               // third part value impl, convert via String
               return ((BaseValue)getSession().getValueFactory().createValue(value.getString(), PropertyType.BINARY))
                  .getInternalData();
            }
         case PropertyType.BOOLEAN :
            return new TransientValueData(value.getBoolean());
         case PropertyType.LONG :
            return new TransientValueData(value.getLong());
         case PropertyType.DOUBLE :
            return new TransientValueData(value.getDouble());
         case PropertyType.DATE :
            return new TransientValueData(value.getDate());
         case PropertyType.PATH :
            QPath pathValue = locationFactory.parseJCRPath(value.getString()).getInternalPath();
            return new TransientValueData(pathValue);
         case PropertyType.NAME :
            InternalQName nameValue = locationFactory.parseJCRName(value.getString()).getInternalName();
            return new TransientValueData(nameValue);
         case PropertyType.REFERENCE :
            try
            {
               Identifier identifier = new Identifier(value.getString());
               return new TransientValueData(identifier);
            }
            catch (IllegalArgumentException e)
            {
               throw new ValueFormatException(e.getMessage());
            }
         case ExtendedPropertyType.PERMISSION :
            PermissionValue permValue = (PermissionValue)value;
            AccessControlEntry ace = new AccessControlEntry(permValue.getIdentity(), permValue.getPermission());
            return new TransientValueData(ace);
         default :
            throw new ValueFormatException("ValueFactory.convert() unknown or unconvertable type " + type);
      }
   }

   private void checkValueConstraints(PropertyDefinitionData def, List<ValueData> newValues, int type)
      throws RepositoryException
   {
      ValueConstraintsMatcher constraints =
         new ValueConstraintsMatcher(def.getValueConstraints(), session.getLocationFactory(),
            session.getTransientNodesManager(), session.getWorkspace().getNodeTypesHolder());

      for (ValueData value : newValues)
      {
         if (!constraints.match(value, type))
         {
            String strVal = null;
            if (type != PropertyType.BINARY)
            {
               strVal = ValueDataUtil.getString(value);
            }
            else
            {
               strVal = "PropertyType.BINARY";
            }

            throw new ConstraintViolationException("Can not set value '" + strVal + "' to " + getPath()
               + " due to value constraints ");
         }
      }
   }
}
