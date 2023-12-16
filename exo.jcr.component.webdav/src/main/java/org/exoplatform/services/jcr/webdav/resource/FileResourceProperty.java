/*
 * Copyright (C) 2003-2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.webdav.resource;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class FileResourceProperty extends HierarchicalProperty {
  private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.FileResourceProperty");

  private Property         property;

  public FileResourceProperty(QName name, Property property) {
    super(name);
    this.property = property;
  }

  @Override
  public String getValue() {
    if (property != null && value == null) {
      try {
        if (property.getDefinition().isMultiple()) {
          if (property.getValues().length >= 1) {
            value = property.getValues()[0].getString();
          } else {
            // this means that we return empty value, because according to
            // WebDAV spec: this is a property whose semantics and syntax are
            // not enforced
            // by the server the server only records the value of a dead
            // property.
            // the client is responsible for maintaining the consistency of the
            // syntax and semantics of a dead property.
            value = "";
          }
        } else {
          value = property.getString();
        }
      } catch (RepositoryException e) {
        LOG.warn("Error getting property {} value. Returning the already retrieved value", name, e);
      } finally {
        property = null;
      }
    }
    return value;
  }

}
