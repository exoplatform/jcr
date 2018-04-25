/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;

/**
 * Created by The eXo Platform SAS. <br>
 * Date: 2009
 *
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id$
 */
public class TestRemoveIncoherentValue extends JcrImplBaseTest {

  // Reproduces the issue described in
  // http://jira.exoplatform.org/browse/JCR-1094
  public void testGetValue() throws Exception {
    String data = "Test JCR";

    Node testRoot = root.addNode("NodeRepresentationTest", "nt:unstructured");

    Node file = testRoot.addNode("file", "nt:file");
    Node d = file.addNode("jcr:content", "nt:resource");
    d.setProperty("jcr:mimeType", "text/plain");
    d.setProperty("jcr:lastModified", Calendar.getInstance());
    d.setProperty("jcr:data", new ByteArrayInputStream(data.getBytes()));

    session.save();

    d.getProperties();

    PropertyImpl property = (PropertyImpl) d.getProperty("jcr:data");
    String propertyId = property.getInternalIdentifier();

    File rootDir = getValuesRootFile();
    assertNotNull(rootDir);
    assertTrue(rootDir.exists());

    File propertyFile = findFile(rootDir, propertyId);

    assertNotNull(propertyFile);
    assertTrue(propertyFile.exists());
    assertTrue(propertyFile.renameTo(new File(propertyFile.getParentFile(), propertyFile.getName() + ".CorruptionTest")));

    session.save();
    file = (Node) session.getItem(file.getPath());

    try {
      file.remove();
      session.save();
    } catch (Exception e) {
      fail("The data should be removed even with corrupted data");
    }
  }

  private File getValuesRootFile() throws RepositoryConfigurationException {
    File rootDir = null;
    List<WorkspaceEntry> workspaceEntries = repository.getConfiguration().getWorkspaceEntries();
    for (WorkspaceEntry workspaceEntry : workspaceEntries) {
      if (!workspaceEntry.getName().equals(WORKSPACE)) {
        continue;
      }
      for (ValueStorageEntry valueStorage : workspaceEntry.getContainer().getValueStorages()) {
        rootDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
      }
    }
    return rootDir;
  }

  private File findFile(File rootFile, String propertyId) {
    if (!rootFile.exists()) {
      return null;
    }
    if (rootFile.isDirectory()) {
      File[] listFiles = rootFile.listFiles();
      if (listFiles == null || listFiles.length == 0) {
        return null;
      }
      for (File file : listFiles) {
        File foundFile = findFile(file, propertyId);
        if (foundFile != null) {
          return foundFile;
        }
      }
    } else if (rootFile.getName().startsWith(propertyId)) {
      return rootFile;
    }
    return null;
  }

}
