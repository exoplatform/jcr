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
package org.exoplatform.services.jcr.core;

import org.exoplatform.services.jcr.impl.core.LocationFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.transaction.xa.XAResource;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ExtendedSession.java 12649 2008-04-02 12:46:37Z ksm $
 */

public interface ExtendedSession extends Session
{

   /**
    * @return
    */
   String getId();

   /**
    * @return Returns the locationFactory.
    */
   LocationFactory getLocationFactory();

   /**
    * Get node by unique identifier.
    * 
    * @param identifier node identifier
    * @return node with specified identifier
    * @throws ItemNotFoundException id node with supplied identifier not found
    * @throws RepositoryException if any repository errors occurs
    */
   Node getNodeByIdentifier(String identifier) throws ItemNotFoundException, RepositoryException;

   /**
    * Deserialize an XML document and adds the resulting item subtree as a child of the node at
    * parentAbsPath.
    * 
    * @param parentAbsPath
    *          the absolute path of the node below which the deserialized subtree is added.
    * @param in
    *          The <code>Inputstream</code> from which the XML to be deserilaized is read.
    * @param uuidBehavior
    *          a four-value flag that governs how incoming UUIDs are handled.
    * @param context
    * @throws IOException
    * @throws PathNotFoundException
    * @throws ItemExistsException
    * @throws ConstraintViolationException
    * @throws InvalidSerializedDataException
    * @throws RepositoryException
    */
   void importXML(String parentAbsPath, InputStream in, int uuidBehavior, Map<String, Object> context)
      throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
      InvalidSerializedDataException, RepositoryException;

   /**
    * Serializes the node (and if <code>noRecurse</code> is <code>false</code>,
    * the whole subtree) at <code>absPath</code> into a series of SAX events by
    * calling the methods of the supplied <code>org.xml.sax.ContentHandler</code>.
    * The resulting XML is in the document view form. Note that <code>absPath</code>
    * must be the path of a node, not a property.
    * <p>
    * If <code>skipBinary</code> is true then any properties of <code>PropertyType.BINARY</code> will be
    * serialized as if they are empty. That is, the existence of the property
    * will be serialized, but its content will not appear in the serialized
    * output (the value of the attribute will be empty). If <code>skipBinary</code> is false
    * then the actual value(s) of each <code>BINARY</code> property is recorded using Base64
    * encoding.
    * <p>
    * If <code>noRecurse</code> is true then only the node at
    * <code>absPath</code> and its properties, but not its child nodes, are
    * serialized. If <code>noRecurse</code> is <code>false</code> then the entire subtree
    * rooted at <code>absPath</code> is serialized.
    * <p>
    * If the user lacks read access to some subsection of the specified tree,
    * that section simply does not get serialized, since, from the user's
    * point of view, it is not there.
    * <p>
    * The serialized output will reflect the state of the current workspace as
    * modified by the state of this <code>Session</code>. This means that
    * pending changes (regardless of whether they are valid according to
    * node type constraints) and the current session-mapping of namespaces
    * are reflected in the output.
    * <p>
    * A <code>PathNotFoundException</code> is thrown if no node exists at <code>absPath</code>.
    * <p>
    * A <code>SAXException</code> is thrown if an error occurs while feeding events to the
    * <code>ContentHandler</code>.
    *
    * @param absPath The path of the root of the subtree to be serialized.
    * This must be the path to a node, not a property
    * @param contentHandler The  <code>org.xml.sax.ContentHandler</code> to
    * which the SAX events representing the XML serialization of the subtree
    * will be output.
    * @param skipBinary A <code>boolean</code> governing whether binary
    * properties are to be serialized.
    * @param noRecurse A <code>boolean</code> governing whether the subtree at
    * absPath is to be recursed.
    * @param exportChildVersionHisotry A <code>boolean</code> governing whether child nodes 
    * version histories must be included into resulting xml. 
    * 
    * @throws PathNotFoundException if no node exists at <code>absPath</code>.
    * @throws org.xml.sax.SAXException if an error occurs while feeding events to the
    * <code>org.xml.sax.ContentHandler</code>.
    * @throws RepositoryException if another error occurs.
    */
   void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse,
      boolean exportChildVersionHisotry) throws IOException, PathNotFoundException, RepositoryException;

   /**
    * Registers session listener.
    * 
    * @param listener
    */
   void registerLifecycleListener(SessionLifecycleListener listener);

   /**
    * Gives the local timeout of the session
    * 
    * @return the timeout the local timeout expressed in milliseconds
    */
   long getTimeout();

   /**
    * Sets the local timeout of the session
    * 
    * @param timeout the new local timeout any value lower or equals to 0 will disable the timeout,
    * the expected value is expressed in milliseconds 
    */
   void setTimeout(long timeout);
   
   /**
    * Indicates whether the session has expired or not. A session expired when it has not
    * been modified since an amount of time bigger than the timeout
    * @return <code>true</code> if it has expired, <code>false</code> otherwise.
    */
   boolean hasExpired();
   
   /**
    * Gives the XA representation of the session
    * @return the {@link XAResource} corresponding to the session
    */
   XAResource getXAResource();
}
