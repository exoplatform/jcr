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
package org.exoplatform.services.jcr;

import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * Implements the <code>RepositoryStub</code> for the JCR Reference Implementation.
 */
public class ExoRepositoryStub extends RepositoryStub
{

   /**
    * Property for the repository name (used for jndi lookup)
    */

   public static final String PROP_REPOSITORY_NAME = "org.apache.jackrabbit.repository.name";

   /**
    * Property for the repository configuration file (used for repository instantiation)
    */
   public static final String PROP_REPOSITORY_CONFIG = "org.apache.jackrabbit.repository.config";

   /**
    * Property for the repository home directory (used for repository instantiation)
    */
   public static final String PROP_REPOSITORY_HOME = "org.apache.jackrabbit.repository.home";

   /**
    * Property for the jaas config path. If the system property
    * <code>java.security.auth.login.config</code> is not set this repository stub will try to read
    * this property from the environment and use the value retrieved as the value for the system
    * property.
    */
   public static final String PROP_JAAS_CONFIG = "org.apache.jackrabbit.repository.jaas.config";

   /**
    * The name of the jaas config system property.
    */
   private static final String SYS_JAAS_CONFIG = "java.security.auth.login.config";

   /**
    * The encoding of the test resources.
    */
   private static final String ENCODING = "UTF-8";

   /**
    * The repository instance
    */
   private ManageableRepository repository;

   private static boolean shoutDown;

   /**
    * Constructor as required by the JCR TCK.
    * 
    * @param env
    *          environment properties.
    */
   public ExoRepositoryStub(Properties env)
   {
      super(env);
      // set some attributes on the sessions
      superuser.setAttribute("exo", "exo");
      readwrite.setAttribute("exo", "exo");
      readonly.setAttribute("exo", "exo");
   }

   public synchronized Repository getRepository() throws RepositoryStubException
   {

      // System.out.println("GET REP >>>>>>>>>>>>>>>>>> >>> "+repository);
      if (repository == null)
      {
         try
         {
            String containerConf =
               ExoRepositoryStub.class.getResource(System.getProperty("jcr.test.configuration.file")).toString();
            String loginConf = ExoRepositoryStub.class.getResource("/login.conf").toString();

            StandaloneContainer.addConfigurationURL(containerConf);
            final StandaloneContainer servicesManager = StandaloneContainer.getInstance();

            if (System.getProperty("java.security.auth.login.config") == null)
               System.setProperty("java.security.auth.login.config", loginConf);

            RepositoryService repositoryService =
               (RepositoryService)servicesManager.getComponentInstanceOfType(RepositoryService.class);

            repository = repositoryService.getRepository("db1tck");
            SessionImpl session = (SessionImpl)repository.login(superuser);
            try
            {
               prepareTestContent(session);
            }
            finally
            {
               session.logout();
            }
            /*
             * try { repository.getNamespaceRegistry().getURI("test"); } catch (NamespaceException e) {
             * repository.getNamespaceRegistry().registerNamespace("test",
             * "http://www.apache.org/jackrabbit/test"); } ExtendedNodeTypeManager ntManager =
             * repository.getNodeTypeManager(); InputStream inXml = ExoRepositoryStub.class
             * .getResourceAsStream("/test/nodetypes-tck.xml"); ntManager.registerAllNodeTypes(inXml,
             * ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
             */

            if (!shoutDown)
            {
               Runtime.getRuntime().addShutdownHook(new Thread()
               {
                  public void run()
                  {
                     // database.close();
                     servicesManager.stop();

                     System.out.println("The container is stopped");
                  }
               });
               shoutDown = true;
            }

         }
         catch (Exception ex)
         {
            ex.printStackTrace();
            throw new RepositoryStubException(ex.getMessage());
         }
      }

      return repository;
   }

   private void prepareTestContent(SessionImpl session) throws RepositoryException, IOException
   {
      //      JackrabbitWorkspace workspace = (JackrabbitWorkspace)session.getWorkspace();
      //      Set workspaces = new HashSet(Arrays.asList(workspace.getAccessibleWorkspaceNames()));
      //      if (!workspaces.contains("test"))
      //      {
      //         workspace.createWorkspace("test");
      //      }

      InternalQName testVersioable = session.getLocationFactory().parseJCRName("test:versionable").getInternalName();
      NodeTypeDataManager ntHolder = session.getWorkspace().getNodeTypesHolder();
      if (ntHolder.getNodeType(testVersioable) == null)
      {
         InputStream xml = getResource("test-nodetypes.xml");
         try
         {
            ntHolder.registerNodeTypes(xml, ExtendedNodeTypeManager.FAIL_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
         }
         finally
         {
            xml.close();
         }
      }
      if (!session.getRootNode().hasNode("testdata"))
      {
         Node data = getOrAddNode(session.getRootNode(), "testdata");
         addPropertyTestData(getOrAddNode(data, "property"));
         addQueryTestData(getOrAddNode(data, "query"));
         addNodeTestData(getOrAddNode(data, "node"));
         addExportTestData(getOrAddNode(data, "docViewTest"));
         session.save();
      }

   }

   private Node getOrAddNode(Node node, String name) throws RepositoryException
   {
      try
      {
         return node.getNode(name);
      }
      catch (PathNotFoundException e)
      {
         return node.addNode(name);
      }
   }

   /**
   * Creates a boolean, double, long, calendar and a path property at the
   * given node.
   */
   private void addPropertyTestData(Node node) throws RepositoryException
   {
      node.setProperty("boolean", true);
      node.setProperty("double", Math.PI);
      node.setProperty("long", 90834953485278298l);
      Calendar c = Calendar.getInstance();
      c.set(2005, 6, 18, 17, 30);
      node.setProperty("calendar", c);
      ValueFactory factory = node.getSession().getValueFactory();
      node.setProperty("path", factory.createValue("/", PropertyType.PATH));
      node.setProperty("multi", new String[]{"one", "two", "three"});
   }

   /**
   * Creates four nodes under the given node. Each node has a String
   * property named "prop1" with some content set.
   */
   private void addQueryTestData(Node node) throws RepositoryException
   {
      while (node.hasNode("node1"))
      {
         node.getNode("node1").remove();
      }
      getOrAddNode(node, "node1").setProperty("prop1", "You can have it good, cheap, or fast. Any two.");
      getOrAddNode(node, "node1").setProperty("prop1", "foo bar");
      getOrAddNode(node, "node1").setProperty("prop1", "Hello world!");
      getOrAddNode(node, "node2").setProperty("prop1", "Apache Jackrabbit");
   }

   /**
   * Creates three nodes under the given node: one of type nt:resource
   * and the other nodes referencing it.
   */
   private void addNodeTestData(Node node) throws RepositoryException, IOException
   {
      if (node.hasNode("multiReference"))
      {
         node.getNode("multiReference").remove();
      }
      if (node.hasNode("resReference"))
      {
         node.getNode("resReference").remove();
      }
      if (node.hasNode("myResource"))
      {
         node.getNode("myResource").remove();
      }

      Node resource = node.addNode("myResource", "nt:resource");
      resource.setProperty("jcr:encoding", ENCODING);
      resource.setProperty("jcr:mimeType", "text/plain");
      resource.setProperty("jcr:data", new ByteArrayInputStream("Hello w\u00F6rld.".getBytes(ENCODING)));
      resource.setProperty("jcr:lastModified", Calendar.getInstance());

      Node resReference = getOrAddNode(node, "reference");
      resReference.setProperty("ref", resource);
      // make this node itself referenceable
      resReference.addMixin("mix:referenceable");

      Node multiReference = node.addNode("multiReference");
      ValueFactory factory = node.getSession().getValueFactory();
      multiReference.setProperty("ref", new Value[]{factory.createValue(resource), factory.createValue(resReference)});
   }

   private void addExportTestData(Node node) throws RepositoryException, IOException
   {
      getOrAddNode(node, "invalidXmlName").setProperty("propName", "some text");

      // three nodes which should be serialized as xml text in docView export
      // separated with spaces
      getOrAddNode(node, "jcr:xmltext").setProperty("jcr:xmlcharacters", "A text without any special character.");
      getOrAddNode(node, "some-element");
      getOrAddNode(node, "jcr:xmltext").setProperty("jcr:xmlcharacters",
         " The entity reference characters: <, ', ,&, >,  \" should" + " be escaped in xml export. ");
      getOrAddNode(node, "some-element");
      getOrAddNode(node, "jcr:xmltext").setProperty("jcr:xmlcharacters", "A text without any special character.");

      Node big = getOrAddNode(node, "bigNode");
      big.setProperty("propName0", "SGVsbG8gd8O2cmxkLg==;SGVsbG8gd8O2cmxkLg==".split(";"), PropertyType.BINARY);
      big.setProperty("propName1", "text 1");
      big.setProperty("propName2", "multival text 1;multival text 2;multival text 3".split(";"));
      big.setProperty("propName3", "text 1");

      addExportValues(node, "propName");
      addExportValues(node, "Prop<>prop");
   }

   /**
   * create nodes with following properties
   * binary & single
   * binary & multival
   * notbinary & single
   * notbinary & multival
   */
   private void addExportValues(Node node, String name) throws RepositoryException, IOException
   {
      String prefix = "valid";
      if (name.indexOf('<') != -1)
      {
         prefix = "invalid";
      }
      node = getOrAddNode(node, prefix + "Names");

      String[] texts = new String[]{"multival text 1", "multival text 2", "multival text 3"};
      getOrAddNode(node, prefix + "MultiNoBin").setProperty(name, texts);

      Node resource = getOrAddNode(node, prefix + "MultiBin");
      resource.setProperty("jcr:encoding", ENCODING);
      resource.setProperty("jcr:mimeType", "text/plain");
      String[] values = new String[]{"SGVsbG8gd8O2cmxkLg==", "SGVsbG8gd8O2cmxkLg=="};
      resource.setProperty(name, values, PropertyType.BINARY);
      resource.setProperty("jcr:lastModified", Calendar.getInstance());

      getOrAddNode(node, prefix + "NoBin").setProperty(name, "text 1");

      resource = getOrAddNode(node, "invalidBin");
      resource.setProperty("jcr:encoding", ENCODING);
      resource.setProperty("jcr:mimeType", "text/plain");
      byte[] bytes = "Hello w\u00F6rld.".getBytes(ENCODING);
      resource.setProperty(name, new ByteArrayInputStream(bytes));
      resource.setProperty("jcr:lastModified", Calendar.getInstance());
   }

   private static InputStream getResource(String name)
   {
      return ExoRepositoryStub.class.getResourceAsStream(name);
   }

}
