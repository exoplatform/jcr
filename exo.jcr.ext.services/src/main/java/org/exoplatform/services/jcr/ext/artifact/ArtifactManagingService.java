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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Administration maven repository
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public interface ArtifactManagingService {
  /**
   * @param sp
   *          the session provider
   * @param parentFolder
   *          the folder which children we need to get
   * @return list of child descriptors
   * @throws RepositoryException
   */
  List<Descriptor> getDescriptors(SessionProvider sp, FolderDescriptor parentFolder) throws RepositoryException;

  /**
   * adds (deploys) artifact including creating necessary group folders, pom and checksum files
   * 
   * @param sp
   *          the session provider
   * @param artifact
   *          descriptor
   * @param jarIStream
   * @param pomIStream
   * @throws RepositoryException
   */

  void addArtifact(SessionProvider sp,
                   ArtifactDescriptor artifact,
                   InputStream jarIStream,
                   InputStream pomIStream) throws RepositoryException;

  /**
   * removes artifact
   * 
   * @param sp
   *          the session provider
   * @param artifact
   *          descriptor
   * @throws RepositoryException
   */
  void removeArtifact(SessionProvider sp, Descriptor artifact) throws RepositoryException;

  /**
   * @param sp
   *          the session provider
   * @param criteria
   *          for search
   * @return list of descriptors
   * @throws RepositoryException
   */
  List<Descriptor> searchArtifacts(SessionProvider sp, SearchCriteria criteria) throws RepositoryException;

  /**
   * imports list of artifacts into maven repo
   * 
   * @param sp
   *          the session provider
   * @param zipInputStream
   *          input stream which contains artifact related files
   * @throws RepositoryException
   */
  void importArtifacts(SessionProvider sp, InputStream zipInputStream) throws RepositoryException,
                                                                      FileNotFoundException;

  /**
   * @param sp
   *          - Session provider
   * @param folder
   * @throws RepositoryException
   */
  void importArtifacts(SessionProvider sp, File folder) throws RepositoryException,
                                                       FileNotFoundException;

  /**
   * exports list of artifacts from maven repo into output stream
   * 
   * @param sp
   *          the session provider
   * @param parentFolder
   *          the folder which children we need to get
   * @param zipOutputStream
   *          output stream to export to
   * @throws RepositoryException
   */
  void exportArtifacts(SessionProvider sp,
                       FolderDescriptor parentFolder,
                       OutputStream zipOutputStream) throws RepositoryException,
                                                    FileNotFoundException;

  /**
   * @param sp
   * @param parentFolder
   * @param folder
   * @throws RepositoryException
   */
  void exportArtifacts(SessionProvider sp, FolderDescriptor parentFolder, File folder) throws RepositoryException,
                                                                                      FileNotFoundException;

  /**
   * Returns acess control list for the specified artifact
   * 
   * @param sp
   *          the session provider
   * @param artifact
   *          the artifact which ACL we need to get
   * @throws RepositoryException
   */
  List getPermission(SessionProvider sp, Descriptor artifact) throws RepositoryException;

  /**
   * Changes acess permissions for the specified artifact ang given identity
   * 
   * @param sp
   *          the session provider
   * @param artifact
   *          the artifact which will be changed
   * @param identity
   *          identity for addin/removing permissions
   * @param permissions
   *          array of permissions to set/remove
   * @param delete
   * 
   * @throws RepositoryException
   */
  void changePermission(SessionProvider sp,
                        Descriptor artifact,
                        String identity,
                        String[] permissions,
                        boolean delete) throws RepositoryException;

  List getListErrors();
}
