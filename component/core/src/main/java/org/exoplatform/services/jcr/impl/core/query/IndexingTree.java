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
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey
 *         Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 * 
 */
public class IndexingTree {
    private final QPath indexingRootQpath;
    private final NodeData indexingRoot;

    private final List<QPath> excludedPaths;

    /**
     * @param indexingRoot
     * @param excludedPaths
     */
    public IndexingTree(NodeData indexingRoot, List<QPath> excludedPaths) {
	super();
	this.indexingRoot = indexingRoot;
	this.indexingRootQpath = indexingRoot.getQPath();
	this.excludedPaths = excludedPaths;
    }

    /**
     * @param indexingRoot
     * @param excludedPaths
     */
    public IndexingTree(NodeData indexingRoot) {
	super();
	this.indexingRoot = indexingRoot;
	this.indexingRootQpath = indexingRoot.getQPath();
	this.excludedPaths = new ArrayList<QPath>();
    }

    /**
     * @return the excludedPaths
     */
    public List<QPath> getExcludedPaths() {
	return excludedPaths;
    }

    /**
     * @return the indexingRoot
     */
    public NodeData getIndexingRoot() {
	return indexingRoot;
    }

    /**
     * Checks if the given event should be excluded based on the
     * {@link #excludePath} setting.
     * 
     * @param event
     *            observation event
     * @return <code>true</code> if the event should be excluded,
     *         <code>false</code> otherwise
     */
    public boolean isExcluded(ItemState event) {

	for (QPath excludedPath : excludedPaths) {
	    if (event.getData().getQPath().isDescendantOf(excludedPath)
		    || event.getData().getQPath().equals(excludedPath))
		return true;
	}

	return !event.getData().getQPath().isDescendantOf(indexingRootQpath)
		&& !event.getData().getQPath().equals(indexingRootQpath);
    }

    /**
     * Checks if the given event should be excluded based on the
     * {@link #excludePath} setting.
     * 
     * @param event
     *            observation event
     * @return <code>true</code> if the event should be excluded,
     *         <code>false</code> otherwise
     */
    public boolean isExcluded(ItemData eventData) {

	for (QPath excludedPath : excludedPaths) {
	    if (eventData.getQPath().isDescendantOf(excludedPath)
		    || eventData.getQPath().equals(excludedPath))
		return true;
	}

	return !eventData.getQPath().isDescendantOf(indexingRootQpath)
		&& !eventData.getQPath().equals(indexingRootQpath);
    }
}
