/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.index.persistent.api;

import java.util.Set;

public class TransientQueueEntrySet {
  private String      workspace;

  private Set<String> removedNodes;

  private Set<String> addedNodes;

  private Set<String> parentRemovedNodes;

  private Set<String> parentAddedNodes;

  public TransientQueueEntrySet(String workspace,
                                Set<String> removedNodes,
                                Set<String> addedNodes,
                                Set<String> parentRemovedNodes,
                                Set<String> parentAddedNodes) {
    this.workspace = workspace;
    this.removedNodes = removedNodes;
    this.addedNodes = addedNodes;
    this.parentRemovedNodes = parentRemovedNodes;
    this.parentAddedNodes = parentAddedNodes;
  }

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public Set<String> getRemovedNodes() {
    return removedNodes;
  }

  public void setRemovedNodes(Set<String> removedNodes) {
    this.removedNodes = removedNodes;
  }

  public Set<String> getAddedNodes() {
    return addedNodes;
  }

  public void setAddedNodes(Set<String> addedNodes) {
    this.addedNodes = addedNodes;
  }

  public Set<String> getParentRemovedNodes() {
    return parentRemovedNodes;
  }

  public void setParentRemovedNodes(Set<String> parentRemovedNodes) {
    this.parentRemovedNodes = parentRemovedNodes;
  }

  public Set<String> getParentAddedNodes() {
    return parentAddedNodes;
  }

  public void setParentAddedNodes(Set<String> parentAddedNodes) {
    this.parentAddedNodes = parentAddedNodes;
  }

}
