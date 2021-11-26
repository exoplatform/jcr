/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.util;

public class XPathUtils {

  /**
   * Avoid the illegal xPath
   * 
   * @param path original path
   * @return Escaped string by ISO9075
   */
  public static String escapeIllegalXPathName(String path) {
    if (path == null)
      return null;
    if (path.length() == 0)
      return "";
    StringBuilder encoded = new StringBuilder();
    StringBuilder currentItem = new StringBuilder();
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        if (currentItem.length() > 0) {
          encoded.append(ISO9075.encode(currentItem.toString()));
          currentItem = new StringBuilder();
        }
        encoded.append('/');
      } else {
        currentItem.append(path.charAt(i));
      }
    }
    if (currentItem.length() > 0) {
      encoded.append(ISO9075.encode(currentItem.toString()));
    }
    return encoded.toString();
  }

  public static String escapeIllegalSQLName(String path) {
    return (path == null || path.trim().length() == 0) ? path : path.replace("'", "''");
  }
}
