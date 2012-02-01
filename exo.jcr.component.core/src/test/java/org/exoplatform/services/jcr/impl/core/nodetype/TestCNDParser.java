package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.CNDStreamReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS. This class provides set of test for CND
 * grammar parser implementation
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class TestCNDParser extends AbstractNodeTypeTest
{

   private final NamespaceRegistryImpl namespaceRegistry = new NamespaceRegistryImpl();

   private final LocationFactory locationFactory = new LocationFactory(namespaceRegistry);

   private InternalQName toName(String name) throws RepositoryException
   {
      return locationFactory.parseJCRName(name).getInternalName();
   }

   /**
    * Runs {@link CNDStreamReader} with std.err and std.out redirecting to avoid
    * parse errors output
    * 
    * @param line
    *           String containing node types in compact node type format
    * @return list of {@link NodeTypeData}
    * @throws RepositoryException
    */
   private List<NodeTypeData> parseString(String line) throws RepositoryException
   {
      /**
       * Start of I/O Redirecting. gUnit test uses std.err and std.out to
       * examine the result of running test. So in ordinary situation parse
       * errors are printed in std.err. In this case std devices are replaced by
       * stubs
       */
      PrintStream console = System.out;
      PrintStream consoleErr = System.err;
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ByteArrayOutputStream err = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(out);
      PrintStream ps2 = new PrintStream(err);
      System.setOut(ps);
      System.setErr(ps2);

      try
      {
         return new CNDStreamReader(namespaceRegistry).read(new ByteArrayInputStream(line.getBytes()));
      }
      finally
      {
         System.setOut(console); // Reset standard output
         System.setErr(consoleErr); // Reset standard err
      }
   }

   /**
    * Testing whether correct default nodeType parameters are assigned
    */
   public void testDefaultNodeTypeParameters()
   {

      try
      {
         String ntString = "<ns = 'http://namespace.com/ns'> [ns:NodeType]";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("ns:NodeType"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), null);
         assertEquals(ntdActual.isMixin(), false);
         assertEquals(ntdActual.hasOrderableChildNodes(), false);
         //assertEquals(ntdActual.isAbstract(), false);
         //assertEquals(ntdActual.isQueryable(), true);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{Constants.NT_BASE}));
         /** Correct properties */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredPropertyDefinitions(), new PropertyDefinitionData[0]));
         /** Correct children */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredChildNodeDefinitions(), new PropertyDefinitionData[0]));

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * Testing registered words usage
    */
   public void testRegisteredWordsUsage1()
   {

      try
      {
         String ntString = "<ns = 'http://namespace.com/ns'> [ns:COPY]";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("ns:COPY"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), null);
         assertEquals(ntdActual.isMixin(), false);
         assertEquals(ntdActual.hasOrderableChildNodes(), false);
         //assertEquals(ntdActual.isAbstract(), false);
         //assertEquals(ntdActual.isQueryable(), true);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{Constants.NT_BASE}));
         /** Correct properties */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredPropertyDefinitions(), new PropertyDefinitionData[0]));
         /** Correct children */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredChildNodeDefinitions(), new PropertyDefinitionData[0]));

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }

   }

   /**
    * Testing registered words usage
    */
   public void testRegisteredWordsUsage2()
   {

      try
      {
         String ntString =
            "<opv = 'http://namespace.com/opv'> [opv:STRING] > opv:decimal, opv:URI abstract orderable mixin noquery";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("opv:STRING"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), null);
         assertEquals(ntdActual.isMixin(), true);
         assertEquals(ntdActual.hasOrderableChildNodes(), true);
         //assertEquals(ntdActual.isAbstract(), true);
         //assertEquals(ntdActual.isQueryable(), false);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{toName("opv:decimal"),
            toName("opv:URI")}));
         /** Correct properties */

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * Testing registered words usage
    */
   public void testRegisteredWordsUsage3()
   {

      try
      {
         String ntString =
            "<opv = 'http://namespace.com/opv'> [opv:mixin] > opv:decimal, opv:URI abstract orderable mixin noquery";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("opv:mixin"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), null);
         assertEquals(ntdActual.isMixin(), true);
         assertEquals(ntdActual.hasOrderableChildNodes(), true);
         //assertEquals(ntdActual.isAbstract(), true);
         //assertEquals(ntdActual.isQueryable(), false);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{toName("opv:decimal"),
            toName("opv:URI")}));

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * Testing whether NT:BASE is assigned to nodeType.
    */
   public void testBaseTypeAssign()
   {

      try
      {
         String ntString = "<ns = 'http://namespace.com/ns'> [ns:NodeType] abstract orderable mixin noquery";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("ns:NodeType"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), null);
         assertEquals(ntdActual.isMixin(), true);
         assertEquals(ntdActual.hasOrderableChildNodes(), true);
         //assertEquals(ntdActual.isAbstract(), true);
         //assertEquals(ntdActual.isQueryable(), false);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{Constants.NT_BASE}));

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * The simplest node type definition is provided to parser. Checking equality
    * to expected result
    */
   public void testSimpleNodeType()
   {

      try
      {
         String ntString =
            "<ns = 'http://namespace.com/ns'> [ns:NodeType] > ns:ParentType1, ns:ParentType2 abstract orderable mixin noquery";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("ns:NodeType"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), null);
         assertEquals(ntdActual.isMixin(), true);
         assertEquals(ntdActual.hasOrderableChildNodes(), true);
         //assertEquals(ntdActual.isAbstract(), true);
         //assertEquals(ntdActual.isQueryable(), false);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{
            toName("ns:ParentType1"), toName("ns:ParentType2")}));

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * Checking if property's default parameters are assigned correctly
    */
   public void testDefalultProprtyParameters()
   {

      try
      {
         String ntString =
            "<ns = 'http://namespace.com/ns'> <ex = 'http://namespace.com/ex'> [ns:NodeType] > ns:ParentType1, ns:ParentType2 a o m nq ! ex:property "
               + "- ex:property";
         NodeTypeData ntdActual = parseString(ntString).get(0);
         PropertyDefinitionData ptdActual = ntdActual.getDeclaredPropertyDefinitions()[0];

         assertEquals(ptdActual.getName(), toName("ex:property"));
         assertEquals(ptdActual.isAutoCreated(), false);
         assertEquals(ptdActual.isMandatory(), false);
         assertEquals(ptdActual.getOnParentVersion(), OnParentVersionAction.COPY);
         assertEquals(ptdActual.isProtected(), false);
         assertEquals(ptdActual.getRequiredType(), PropertyType.STRING);
         assertEquals(ptdActual.getValueConstraints(), null);
         assertEquals(ptdActual.getDefaultValues(), null);
         assertEquals(ptdActual.isMultiple(), false);
         //         assertEquals(ptdActual.isQueryOrderable(), true);
         //         String[] actual = ptdActual.getAvailableQueryOperators();
         //         String[] expected = Operator.getAllQueryOperators();
         //         Arrays.sort(actual);
         //         Arrays.sort(expected);
         //         assertTrue(Arrays.deepEquals(actual, expected));
         //         assertEquals(ptdActual.isFullTextSearchable(), true);

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * Checking if child's default parameters are assigned correctly
    */
   public void testDefaultChildParameters()
   {

      try
      {
         String ntString =
            "<ns = 'http://namespace.com/ns'> <ex = 'http://namespace.com/ex'> [ns:NodeType] > ns:ParentType1, ns:ParentType2 a o m nq + ns:node";
         NodeTypeData ntdActual = parseString(ntString).get(0);
         NodeDefinitionData chdActual = ntdActual.getDeclaredChildNodeDefinitions()[0];

         assertEquals(chdActual.getName(), toName("ns:node"));
         assertEquals(chdActual.isAutoCreated(), false);
         assertEquals(chdActual.isMandatory(), false);
         assertEquals(chdActual.getOnParentVersion(), OnParentVersionAction.COPY);
         assertEquals(chdActual.isProtected(), false);
         assertEquals(chdActual.getDefaultPrimaryType(), null);
         assertEquals(chdActual.isAllowsSameNameSiblings(), false);
         assertTrue(Arrays.deepEquals(chdActual.getRequiredPrimaryTypes(), new InternalQName[]{Constants.NT_BASE}));

      }
      catch (RepositoryException e)
      {
         fail("Exception found " + e.getMessage());
      }
   }

   /**
    * Full nodetype definition is provided to parser. Checking equality to
    * expected result
    */
   public void testComplexNodeType()
   {
      try
      {
         String ntString =
            "<ns = 'http://namespace.com/ns'> <ex = 'http://namespace.com/ex'> [ns:NodeType] > ns:ParentType1, ns:ParentType2 a o m nq ! ex:property "
               + "- ex:property (STRING) = 'default1', 'default2' mandatory aut protected * INITIALIZE "
               + "< 'constraint1', 'constraint2' queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder "
               + "+ ns:node (ns:reqType1, ns:reqType2) = ns:defaultType mandatory autocreated protected sns VERSION "
               + "+ ns:node2 (ns:reqType1, ns:reqType2, nt:subBase)  man p * VERSION";
         NodeTypeData ntdActual = parseString(ntString).get(0);

         PropertyDefinitionData ptdActual = ntdActual.getDeclaredPropertyDefinitions()[0];

         NodeDefinitionData chdActual1 = ntdActual.getDeclaredChildNodeDefinitions()[0];
         NodeDefinitionData chdActual2 = ntdActual.getDeclaredChildNodeDefinitions()[1];

         /** Correct names */
         assertEquals(ntdActual.getName(), toName("ns:NodeType"));
         /** Correct attributes */
         assertEquals(ntdActual.getPrimaryItemName(), toName("ex:property"));
         assertEquals(ntdActual.isMixin(), true);
         assertEquals(ntdActual.hasOrderableChildNodes(), true);
         //         assertEquals(ntdActual.isAbstract(), true);
         //         assertEquals(ntdActual.isQueryable(), false);
         /** Correct supertypes */
         assertTrue(Arrays.deepEquals(ntdActual.getDeclaredSupertypeNames(), new InternalQName[]{
            toName("ns:ParentType1"), toName("ns:ParentType2")}));

         /** Correct properties */
         assertEquals(ptdActual.getName(), toName("ex:property"));
         assertEquals(ptdActual.isAutoCreated(), true);
         assertEquals(ptdActual.isMandatory(), true);
         assertEquals(ptdActual.getOnParentVersion(), OnParentVersionAction.INITIALIZE);
         assertEquals(ptdActual.isProtected(), true);
         assertEquals(ptdActual.getRequiredType(), PropertyType.STRING);
         assertTrue(Arrays.deepEquals(ptdActual.getValueConstraints(), new String[]{"constraint1", "constraint2"}));
         assertTrue(Arrays.deepEquals(ptdActual.getDefaultValues(), new String[]{"default1", "default2"}));
         assertEquals(ptdActual.isMultiple(), true);
         //         assertEquals(ptdActual.isQueryOrderable(), false);
         //         String[] actual = ptdActual.getAvailableQueryOperators();
         //         String[] expected = new String[]{"=", "<>", "<", "<=", ">", ">=", "LIKE"};
         //         Arrays.sort(actual);
         //         Arrays.sort(expected);
         //         assertTrue(Arrays.deepEquals(actual, expected));
         //         assertEquals(ptdActual.isFullTextSearchable(), false);
         /** Correct children */
         // first child
         assertEquals(chdActual1.getName(), toName("ns:node"));
         assertEquals(chdActual1.isAutoCreated(), true);
         assertEquals(chdActual1.isMandatory(), true);
         assertEquals(chdActual1.getOnParentVersion(), OnParentVersionAction.VERSION);
         assertEquals(chdActual1.isProtected(), true);
         assertEquals(chdActual1.getDefaultPrimaryType(), toName("ns:defaultType"));
         assertEquals(chdActual1.isAllowsSameNameSiblings(), true);
         assertTrue(Arrays.deepEquals(chdActual1.getRequiredPrimaryTypes(), new InternalQName[]{toName("ns:reqType1"),
            toName("ns:reqType2")}));
         // second child
         assertEquals(chdActual2.getName(), toName("ns:node2"));
         assertEquals(chdActual2.isAutoCreated(), false);
         assertEquals(chdActual2.isMandatory(), true);
         assertEquals(chdActual2.getOnParentVersion(), OnParentVersionAction.VERSION);
         assertEquals(chdActual2.isProtected(), true);
         assertEquals(chdActual2.getDefaultPrimaryType(), null);
         assertEquals(chdActual2.isAllowsSameNameSiblings(), true);
         assertTrue(Arrays.deepEquals(chdActual2.getRequiredPrimaryTypes(), new InternalQName[]{toName("ns:reqType1"),
            toName("ns:reqType2"), toName("nt:subBase")}));

      }
      catch (RepositoryException e)
      {
         fail("Exception found ");
      }

   }

   /**
    * Testing whether wrong operator is handled correctly.
    */
   public void testWrongOperator()
   {
      try
      {
         String ntString =
            "<ns = 'http://namespace.com/ns'> <ex = 'http://namespace.com/ex'> [ns:NodeType] > ns:ParentType1, ns:ParentType2 a o m nq ! ex:property - ex:property (STRING) "
               + "= 'default1', 'default2' mandatory autocreated protected multiple VERSION < 'constraint1', 'constraint2' "
               + "queryops '=, <>, <, =>, >, >=, LIKE' nofulltext noqueryorder";
         parseString(ntString);

         fail("Exception should be generated! Wrong operator.");
      }
      catch (RepositoryException e)
      {
         // it's ok!
      }
   }

   /**
    * Testing whether wrong name is handled correctly.
    */
   public void testWrongName()
   {
      try
      {
         String ntString =
            "<ns = 'http://namespace.com/ns'> [ns::::NodeType] > ns:ParentType1, ns:ParentType2 a o m nq ! ex:property - ex:property (STRING) "
               + "= 'default1', 'default2' mandatory autocreated protected multiple VERSION < 'constraint1', 'constraint2' "
               + "queryops '=, <>, <, =>, >, >=, LIKE' nofulltext noqueryorder";
         parseString(ntString);
         fail("Exception should be generated! Wrong name.");
      }
      catch (RepositoryException e)
      {
         // it's ok!
      }
   }

   /**
    * Testing whether wrong input sequence is handled correctly.
    */
   public void testWrongInput1()
   {
      try
      {
         String ntString =
            "]ns:NodeType[ >< ns:ParentType1, ns:ParentType2 a o m nq ! ex:property - ex:property (STRING) ";
         parseString(ntString);
         fail("Exception should be generated! Wrong input.");
      }
      catch (RepositoryException e)
      {
         // it's ok!
      }
   }

   /**
    * Testing whether wrong input sequence is handled correctly.
    */
   public void testWrongInput2()
   {
      try
      {
         String ntString =
            "[ns:NodeType] < ns:ParentType1, ns:ParentType2 >a o m nq ! ex:property - ex:property (STRING) ";
         parseString(ntString);
         fail("Exception should be generated! Wrong input.");
      }
      catch (RepositoryException e)
      {
         // it's ok!
      }
   }

   /**
    * Testing whether wrong input sequence is handled correctly.
    */
   public void testWrongInput3()
   {
      try
      {
         String ntString =
            "[ns:NodeType] > ns:ParentType1, ns:ParentType2 a o =m -nq ! ex:property - ex:property (STRING) ";
         parseString(ntString);
         fail("Exception should be generated! Wrong input.");
      }
      catch (RepositoryException e)
      {
         // it's ok!
      }
   }

}
