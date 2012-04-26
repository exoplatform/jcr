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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.JCRItemExistsException;

import java.io.IOException;
import java.sql.SQLException;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * The eXo JCR database has constraints can be violated.
 * 
 * JCR_PK_XCONTAINER - Can not be exisits two containers with same version
 * 
 * JCR_PK_XITEM - Item already exists with this ID JCR_FK_XITEM_PARENT - Parent not found by ID
 * JCR_IDX_XITEM_PARENT - Item already exists with the parent, name, index, type(N/P), persisted
 * version JCR_IDX_XITEM_PARENT_NAME - Item already exists with the type(N/P), parent, name, index,
 * version JCR_IDX_XITEM_PARENT_ID - Item already exists with the type(N/P), parent, ID,
 * persisted version
 * 
 * JCR_PK_XVALUE - Value already exists with the ID (impossible, ID is autiincremented)
 * JCR_FK_XVALUE_PROPERTY - There is no property exists for the value. JCR_IDX_XVALUE_PROPERTY -
 * Value already exists with the property and order number
 * 
 * JCR_PK_XREF - Reference already exists to the node from property with order number
 * JCR_IDX_XREF_PROPERTY - Reference already exists with the property and order number
 * 
 * @author Peter Nedonosko
 * @version $Id: SQLExceptionHandler.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class SQLExceptionHandler
{

   /**
    * Ccontainer name.
    */
   private final String containerName;

   /**
    * Storage connection.
    */
   private final JDBCStorageConnection conn;

   // ---------------- SQLException handler -------------------

   /**
    * SQLExceptionHandler constructor.
    * 
    * @param containerName
    *          - workspace container name
    * @param conn
    *          - storage connection
    */
   SQLExceptionHandler(String containerName, JDBCStorageConnection conn)
   {
      this.containerName = containerName;
      this.conn = conn;
   }

   /**
    * Handle Add SQLException.
    * 
    * @param e
    *          - an SQLException
    * @param item
    *          - context ItemData
    * @return String with error message
    * @throws RepositoryException
    *           if <code>RepositoryException</code> should be thrown
    * @throws InvalidItemStateException
    *           if <code>InvalidItemStateException</code> should be thrown
    */
   protected String handleAddException(SQLException e, ItemData item) throws RepositoryException,
      InvalidItemStateException
   {
      StringBuilder message = new StringBuilder("[");
      message.append(containerName).append("] ADD ").append(item.isNode() ? "NODE. " : "PROPERTY. ");

      String errMessage = e.getMessage();
      String itemInfo =
         item.getQPath().getAsString() + ", ID: " + item.getIdentifier() + ", ParentID: " + item.getParentIdentifier()
            + (errMessage != null ? ". Cause >>>> " + errMessage : "");

      if (errMessage != null)
      {
         // try detect error by foreign key names
         String umsg = errMessage.toLowerCase().toUpperCase();
         if (umsg.indexOf(conn.JCR_FK_ITEM_PARENT) >= 0)
         {
            message.append("Parent not found. Item ").append(itemInfo);
            throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.ADDED, e);
         }
         else if (umsg.indexOf(conn.JCR_PK_ITEM) >= 0)
         {
            message.append("Item already exists. Condition: ID. ").append(itemInfo);
            // InvalidItemStateException ! - because it's impossible add new item with existed UUID
            throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.ADDED, e);
         }
         else if (umsg.indexOf(conn.JCR_IDX_ITEM_PARENT_NAME) >= 0)
         {
            message.append("Item already exists. Condition: parent ID and ID. ").append(itemInfo);
            throw new ItemExistsException(message.toString(), e);
         }
         else if (umsg.indexOf(conn.JCR_IDX_ITEM_PARENT_ID) >= 0)
         {
            message.append("Item already exists. Condition: parent ID and ID. ").append(itemInfo);
            throw new ItemExistsException(message.toString(), e);
         }
         else if (umsg.indexOf(conn.JCR_IDX_ITEM_PARENT) >= 0)
         {
            message.append("Item already exists. Condition: parent ID, name, index. ").append(itemInfo);
            throw new ItemExistsException(message.toString(), e);
         }        
         else if (umsg.indexOf(conn.JCR_FK_VALUE_PROPERTY) >= 0)
         {
            message.append("Property is not exist but the value is being created. Condition: property ID. ").append(
               itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
         else if (umsg.indexOf(conn.JCR_IDX_VALUE_PROPERTY) >= 0)
         {
            message.append("Property already exists. Condition: property ID, order number. ").append(itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
         else if (umsg.indexOf(conn.JCR_PK_VALUE) >= 0)
         {
            message
               .append(
                  "[FATAL] Value already exists with the ValueID. Impossible state, check is ValueID is autoincremented. ")
               .append(itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
         else if (umsg.indexOf(conn.JCR_PK_REF) >= 0)
         {
            message.append("Reference chain already exists. Condition: node ID, property ID, order number. ").append(
               itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
         else if (umsg.indexOf(conn.JCR_IDX_REF_PROPERTY) >= 0)
         {
            message.append("Referenceable property value already exists. Condition: property ID, order number. ")
               .append(itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
      }

      // try detect integrity violation
      RepositoryException ownException = null;
      try
      {
         NodeData parent = (NodeData)conn.getItemData(item.getParentIdentifier());
         if (parent != null)
         {
            // have a parent
            try
            {
               ItemData me = conn.getItemData(item.getIdentifier());
               if (me != null)
               {
                  // item already exists
                  message.append("Item already exists in storage: ").append(itemInfo);
                  ownException = new JCRItemExistsException(message.toString(), me.getIdentifier(), ItemState.ADDED, e);
                  throw ownException;
               }

               me =
                  conn.getItemData(parent, new QPathEntry(item.getQPath().getName(), item.getQPath().getIndex()),
                     ItemType.getItemType(item));
               if (me != null)
               {
                  message.append("Item already exists in storage: ").append(itemInfo);
                  ownException = new JCRItemExistsException(message.toString(), me.getIdentifier(), ItemState.ADDED, e);
                  throw ownException;
               }

            }
            catch (Exception ep)
            {
               // item not found or other things but error of item reading
               if (ownException != null)
                  throw ownException;
            }

            // MySQL violation
            if (e.getClass().getName().indexOf("MySQLIntegrityConstraintViolationException") >= 0
               && errMessage.indexOf(item.getIdentifier()) >= 0)
            {
               // it's JCR_PK_ITEM violation 
               message.append("Item already exists. Condition: ID. ").append(itemInfo);
               throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.ADDED, e);
            }

            // DB2 violation
            if (e.getClass().getName().indexOf("SqlIntegrityConstraintViolationException") >= 0
               && errMessage.indexOf("SQLCODE=-803") >= 0)
            {
               message.append("Item already exists.").append(itemInfo);
               throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.ADDED, e);
            }

            message.append("Error of item add. ").append(itemInfo);
            ownException = new RepositoryException(message.toString(), e);
            throw ownException;
         }
      }
      catch (Exception ep)
      {
         // no parent or error access it
         if (ownException != null)
            throw ownException;
      }
      message.append("Error of item add. ").append(itemInfo);
      throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.ADDED, e);
   }

   /**
    * Handle Add IOException.
    * 
    * @param e
    *          - an IOException
    * @param item
    *          - context ItemData
    * @return String with error message
    * @throws RepositoryException
    *           if <code>RepositoryException</code> should be thrown
    * @throws InvalidItemStateException
    *           if <code>InvalidItemStateException</code> should be thrown
    */
   protected String handleAddException(IOException e, ItemData item) throws RepositoryException,
      InvalidItemStateException
   {
      StringBuilder message = new StringBuilder("[");
      message.append(containerName).append("] ADD ").append(item.isNode() ? "NODE. " : "PROPERTY. ");

      String errMessage = e.getMessage();
      String itemInfo =
         item.getQPath().getAsString() + ", ID: " + item.getIdentifier() + ", ParentID: " + item.getParentIdentifier()
            + (errMessage != null ? ". Cause >>>> " + errMessage : "");

      // try detect integrity violation
      RepositoryException ownException = null;
      try
      {
         NodeData parent = (NodeData)conn.getItemData(item.getParentIdentifier());
         if (parent != null)
         {
            // have a parent
            try
            {
               ItemData me = conn.getItemData(item.getIdentifier());
               if (me != null)
               {
                  // item already exists
                  message.append("Item already exists in storage: ").append(itemInfo);
                  ownException = new ItemExistsException(message.toString(), e);
                  throw ownException;
               }

               me =
                  conn.getItemData(parent, new QPathEntry(item.getQPath().getName(), item.getQPath().getIndex()),
                     ItemType.getItemType(item));
               if (me != null)
               {
                  message.append("Item already exists in storage: ").append(itemInfo);
                  ownException = new ItemExistsException(message.toString(), e);
                  throw ownException;
               }

            }
            catch (Exception ep)
            {
               // item not found or other things but error of item reading
               if (ownException != null)
                  throw ownException;
            }
            message.append("Error of item add. ").append(itemInfo);
            ownException = new RepositoryException(message.toString(), e);
            throw ownException;
         }
      }
      catch (Exception ep)
      {
         // no parent or error access it
         if (ownException != null)
            throw ownException;
      }
      message.append("Error of item add. ").append(itemInfo);
      throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.ADDED, e);
   }

   /**
    * Handle delete Exceptions.
    * 
    * @param e
    *          - an SQLException
    * @param item
    *          - context ItemData
    * @return String with error message
    * @throws RepositoryException
    *           if <code>RepositoryException</code> should be thrown
    * @throws InvalidItemStateException
    *           if <code>InvalidItemStateException</code> should be thrown
    */
   protected String handleDeleteException(SQLException e, ItemData item) throws RepositoryException,
      InvalidItemStateException
   {
      StringBuilder message = new StringBuilder("[");
      message.append(containerName).append("] DELETE ").append(item.isNode() ? "NODE. " : "PROPERTY. ");

      String errMessage = e.getMessage();
      String itemInfo =
         item.getQPath().getAsString() + " " + item.getIdentifier()
            + (errMessage != null ? ". Cause >>>> " + errMessage : "");

      if (errMessage != null)
      {
         // try detect error by foreign key names
         String umsg = errMessage.toLowerCase().toUpperCase();
         if (umsg.indexOf(conn.JCR_FK_ITEM_PARENT) >= 0)
         {
            message.append("Can not delete parent till childs exists. Item ").append(itemInfo);
            throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.DELETED, e);
         }
         else if (umsg.indexOf(conn.JCR_FK_VALUE_PROPERTY) >= 0)
         {
            message.append("[FATAL] Can not delete property item till it contains values. Condition: property ID. ")
               .append(itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
      }

      message.append("Error of item delete ").append(itemInfo);
      throw new RepositoryException(message.toString(), e);
   }

   /**
    * Handle update Exceptions.
    * 
    * @param e
    *          - an SQLException
    * @param item
    *          - context ItemData
    * @return String with error message
    * @throws RepositoryException
    *           if <code>RepositoryException</code> should be thrown
    * @throws InvalidItemStateException
    *           if <code>InvalidItemStateException</code> should be thrown
    */
   public String handleUpdateException(SQLException e, ItemData item) throws RepositoryException,
      InvalidItemStateException
   {
      StringBuilder message = new StringBuilder("[");
      message.append(containerName).append("] EDIT ").append(item.isNode() ? "NODE. " : "PROPERTY. ");

      String errMessage = e.getMessage();
      String itemInfo =
         item.getQPath().getAsString() + " " + item.getIdentifier()
            + (errMessage != null ? ". Cause >>>> " + errMessage : "");

      if (errMessage != null)
         // try detect error by foreign key names
         if (errMessage.toLowerCase().toUpperCase().indexOf(conn.JCR_FK_VALUE_PROPERTY) >= 0)
         {
            message.append("Property is not exists but the value is being created. Condition: property ID. ").append(
               itemInfo);
            throw new RepositoryException(message.toString(), e);
         }
         else if (errMessage.toLowerCase().toUpperCase().indexOf(conn.JCR_PK_ITEM) >= 0)
         {
            message.append("Item already exists. Condition: ID. ").append(itemInfo);
            throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.UPDATED, e);
         }

      // try detect integrity violation
      RepositoryException ownException = null;
      try
      {
         ItemData me = conn.getItemData(item.getIdentifier());
         if (me != null)
         {
            // item already exists
            message.append("Item already exists. But update errors. ").append(itemInfo);
            ownException = new RepositoryException(message.toString(), e);
            throw ownException;
         }
      }
      catch (Exception ep)
      {
         // item not found or other things but error of item reading
         if (ownException != null)
            throw ownException;
      }
      message.append("Error of item update. ").append(itemInfo);
      throw new JCRInvalidItemStateException(message.toString(), item.getIdentifier(), ItemState.UPDATED, e);
   }

}
