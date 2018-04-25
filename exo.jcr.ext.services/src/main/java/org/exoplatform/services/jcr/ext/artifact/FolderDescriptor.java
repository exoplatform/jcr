/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.artifact;

public class FolderDescriptor implements Descriptor {

  private final String folderId;

  public FolderDescriptor(String folderId) {
    this.folderId = folderId;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.jcr.ext.maven.Descriptor#getAsString()
   */
  public String getAsString() {
    return folderId;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.jcr.ext.maven.Descriptor#getAsPath()
   */
  public String getAsPath() {
    // String.replaceAll() - cannot be used because of it uses a regex as a
    // parameter and a '.'(dot) means a any symbol.
    StringBuffer str = new StringBuffer();
    for (int i = 0; i < folderId.length(); i++)
      if (folderId.charAt(i) == '.')
        str.append('/');
      else
        str.append(folderId.charAt(i));
    return str.toString();
  }
}
