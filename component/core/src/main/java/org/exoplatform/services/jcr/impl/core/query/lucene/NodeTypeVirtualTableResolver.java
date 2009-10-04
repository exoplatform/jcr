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
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34027 2009-07-15 23:26:43Z
 *          aheritier $
 */
public abstract class NodeTypeVirtualTableResolver<Q> implements
	VirtualTableResolver<Q> {
    private final NodeTypeDataManager nodeTypeDataManager;

    /**
     * @param nodeTypeDataManager
     */
    public NodeTypeVirtualTableResolver(
	    final NodeTypeDataManager nodeTypeDataManager) {
	super();
	this.nodeTypeDataManager = nodeTypeDataManager;
    }

    /**
     * @param nodeTypeName
     *            name.
     * @return Returns all subtypes of node type <code>nodeTypeName</code> in
     *         the node type inheritance hierarchy.
     * @throws RepositoryException
     */
    protected Set<InternalQName> getSubTypes(final InternalQName nodeTypeName)
	    throws RepositoryException {
	return this.nodeTypeDataManager.getSubtypes(nodeTypeName);
    }

    /**
     * @param nodeTypeName
     *            name.
     * @return true if node type with name <code>nodeTypeName</code> is mixin.
     * @throws RepositoryException
     */
    protected boolean isMixin(final InternalQName nodeTypeName)
	    throws RepositoryException {
	final NodeTypeData nodeType = this.nodeTypeDataManager
		.getNodeType(nodeTypeName);
	if (nodeType == null) {
	    throw new NoSuchNodeTypeException("Node type "
		    + nodeTypeName.getAsString() + " not found");
	}
	return nodeType.isMixin();
    }
}
