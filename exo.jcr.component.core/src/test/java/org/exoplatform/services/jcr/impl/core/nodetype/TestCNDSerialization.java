package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.CNDStreamReader;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.CNDStreamWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Created by The eXo Platform SAS.<br>
 * Class that tests read-write-read cycle for compact node type definition tools
 * ({@link CNDStreamReader} and {@link CNDStreamWriter})
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class TestCNDSerialization extends JcrImplBaseTest
{

   private static final String TEST_FILE = "cnd-reader-test-input.cnd";

   public void testSerialization() throws Exception
   {
      /** input stream */
      InputStream is = getClass().getClassLoader().getResourceAsStream("" + TEST_FILE);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      NamespaceRegistryImpl nsm = new NamespaceRegistryImpl();
      /** reading and writing */
      List<NodeTypeData> ntdList1 = new CNDStreamReader(nsm).read(is);
      new CNDStreamWriter(nsm).write(ntdList1, baos);
      /** new reader to read previous output */
      List<NodeTypeData> ntdList2 = new CNDStreamReader(nsm).read(new ByteArrayInputStream(baos.toByteArray()));
      /** checking equality */
      if (ntdList1.size() == 0 || ntdList1.size() != ntdList2.size())
      {
         fail("Exported node type definition was not successfully read back in");
      }
      else
      {
         for (int k = 0; k < ntdList1.size(); k++)
         {
            NodeTypeData ntd1 = ntdList1.get(k);
            NodeTypeData ntd2 = ntdList2.get(k);
            if (!ntd1.equals(ntd2))
            {
               fail("Exported node type definition was not successfully read back in. \r\n" + ntd2.getName()
                  + "differs from original " + ntd1.getName() + "\r\n" + baos.toString());
            }
         }
      }
   }
}
