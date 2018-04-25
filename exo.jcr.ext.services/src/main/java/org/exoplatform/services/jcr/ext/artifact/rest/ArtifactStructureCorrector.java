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
package org.exoplatform.services.jcr.ext.artifact.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.services.log.Log;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.ext.artifact.CRCGenerator;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

/**
 * Created by The eXo Platform SARL Author : Volodymyr Krasnikov
 * volodymyr.krasnikov@exoplatform.com.ua 29.10.2007
 */
@Path("/maven2-structure-corrector/")
public class ArtifactStructureCorrector implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(ArtifactStructureCorrector.class);

  private RepositoryService repoService;

  private String            repoWorkspaceName;

  private String            rootNodePath;

  private SessionProvider   sessionProvider;

  public ArtifactStructureCorrector(InitParams initParams,
                                    RepositoryService repoService,
                                    Authenticator authenticator) throws Exception {

    this.repoService = repoService;

    if (initParams == null) {
      throw new RepositoryConfigurationException("Init parameters expected !!!");
    }

    PropertiesParam props = initParams.getPropertiesParam("artifact.workspace");

    if (props == null) {
      throw new RepositoryConfigurationException("Property parameters 'locations' expected");
    }

    repoWorkspaceName = props.getProperty("workspace");
    rootNodePath = props.getProperty("rootNode");
    String username = props.getProperty("username");
    String password = props.getProperty("password");
    String userId = authenticator.validateUser(new Credential[] { new UsernameCredential(username),
        new PasswordCredential(password) });

    sessionProvider = new SessionProvider(new ConversationState(authenticator.createIdentity(userId)));

  }

  @GET
  public Response correctStructure() throws RepositoryException {
    new Thread(new ChecksumGenerator(currentSession(sessionProvider)),
               "Correct jcr struct, Append checksums to artifacts").start();
    return Response.ok().build();
  }

  private Session currentSession(SessionProvider sp) throws RepositoryException {
    return sp.getSession(repoWorkspaceName, repoService.getCurrentRepository());
  }

  class ChecksumGenerator implements Runnable {
    private Session session;

    public ChecksumGenerator(Session session) {
      this.session = session;
    }

    public void run() {

      LOG.info("Maven artifact checksums Updater started");

      try {
        Node rootNode = (Node) session.getItem(rootNodePath);

        jcrSpaning(rootNode);

      } catch (PathNotFoundException e) {
        LOG.error("Cannot get target node", e);
      } catch (RepositoryException e) {
        LOG.error("General repository exception", e);
      }

      LOG.info("Maven artifact checksums Updater finished!");
    }

    private void jcrSpaning(Node current_root) throws RepositoryException {
      String algorithm = "sha1";

      // detect all resources that needs for checksums accompanied
      for (NodeIterator nodeIterator = current_root.getNodes(); nodeIterator.hasNext();) {
        Node node = nodeIterator.nextNode();

        if (!node.isNodeType("nt:file")) { // not a resource
          jcrSpaning(node);
        } else {

          String path = node.getPath();
          String ext = FilenameUtils.getExtension(path);
          if (!ext.equalsIgnoreCase(algorithm)) {
            // perform with real artifact data only, checksums filtered
            try {
              // checks if there is a checksum node
              session.getItem(path + "." + algorithm);
            } catch (PathNotFoundException e) {

              Node resource_node = (Node) session.getItem(path);

              addChecksumNode(resource_node);

            } catch (ItemExistsException e) {
              LOG.info("There is checksum for : " + node.getName());
            }
          }

          session.refresh(true);
          session.save();

        }
      }
    }

    private void addChecksumNode(Node src) throws RepositoryException {

      Node parent = src.getParent();
      Node content = src.getNode("jcr:content");
      Property data = content.getProperty("jcr:data");
      String algorithm = "SHA1";
      try {
        String checksum = CRCGenerator.getChecksum(data.getStream(), algorithm);

        LOG.info("Generate checksum for : " + src.getName());

        Node checkNode = parent.addNode(src.getName() + "." + algorithm.toLowerCase(), "nt:file");

        InputStream checksum_is = new ByteArrayInputStream(checksum.getBytes());
        String mimeType = "text/plain";

        Node sum_content = checkNode.addNode("jcr:content", "nt:resource");
        sum_content.setProperty("jcr:mimeType", mimeType);
        sum_content.setProperty("jcr:lastModified", Calendar.getInstance());
        sum_content.setProperty("jcr:data", checksum_is);

        IOUtils.closeQuietly(checksum_is);

      } catch (NoSuchAlgorithmException e) {
        LOG.error("Cannot eval checksum with this algorithm", e);
      } catch (IOException e) {
        LOG.error("Streams exception while eval checksums", e);
      }

    }
  }
}
