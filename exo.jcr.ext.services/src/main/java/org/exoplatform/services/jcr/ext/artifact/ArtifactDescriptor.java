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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public final class ArtifactDescriptor implements Descriptor {

  private final FolderDescriptor groupId;

  private final String           artifactId;

  private final String           versionId;

  public ArtifactDescriptor(FolderDescriptor groupId, String artifactId, String versionId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.versionId = versionId;
  }

  public FolderDescriptor getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersionId() {
    return versionId;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.jcr.ext.maven.Descriptor#getAsString()
   */
  public String getAsString() {
    // is that correct?
    return groupId.getAsString() + "-" + artifactId + "-" + versionId;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.jcr.ext.maven.Descriptor#getAsPath()
   */
  public String getAsPath() {
    // is that correct?
    if (StringUtils.isBlank(artifactId) || StringUtils.isBlank(versionId))
      return groupId.getAsString(); // do not replace dots "." at the tail, as a version id
    return groupId.getAsPath() + "/" + artifactId + "/" + versionId;
  }

  public static ArtifactDescriptor createFromPomfile(File pomfile) throws SAXException,
                                                                  ParserConfigurationException,
                                                                  IOException {
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(pomfile);

    NodeList groupIdList = doc.getElementsByTagName("groupId");
    String groupId = groupIdList.item(0).getTextContent().trim();

    NodeList artifactIdList = doc.getElementsByTagName("artifactId");
    // String artifactId = artifactIdList.item(0).getTextContent();
    String artifactId = "";
    for (int i = 0; i < artifactIdList.getLength(); i++) {
      if (artifactIdList.item(i).getParentNode().getNodeName().equals("project"))
        artifactId = artifactIdList.item(i).getTextContent().trim();
    }

    NodeList versionList = doc.getElementsByTagName("version");
    String versionId = validMavenVersion(versionList.item(0).getTextContent().trim());

    return new ArtifactDescriptor(new FolderDescriptor(groupId), artifactId, versionId);
  }

  private static String validMavenVersion(String version) {
    // Not necessary checking, -SNAPSHOT will be ignoreg if uncomment;
    // CharSet charSet = CharSet.getInstance("A-Za-z");
    // int pos = version.indexOf("-");
    // char next_ch = version.charAt(pos + 1);
    // if ((pos > 0) && (charSet.contains(next_ch)))
    // version = version.substring(0, pos);

    return version;
  }

}
