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

public class SearchCriteria {

  private String  containsExpr;

  private boolean includePom;

  private boolean includeJar;

  public SearchCriteria() {

  }

  public SearchCriteria(String containsExpr) {
    this.containsExpr = containsExpr;
  }

  public String getContainsExpr() {
    return containsExpr;
  }

  public void setContainsExpr(String containsExpr) {
    this.containsExpr = containsExpr;
  }

  public boolean isIncludePom() {
    return includePom;
  }

  public void setIncludePom(boolean includePom) {
    this.includePom = includePom;
  }

  public boolean isIncludeJar() {
    return includeJar;
  }

  public void setIncludeJar(boolean includeJar) {
    this.includeJar = includeJar;
  }

}
