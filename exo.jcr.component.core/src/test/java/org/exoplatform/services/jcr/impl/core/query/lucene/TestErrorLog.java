/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.impl.core.query.BaseQueryTest;
import org.exoplatform.services.jcr.impl.core.query.ErrorLog;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko
 * <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */
public class TestErrorLog extends BaseQueryTest {
    ErrorLog log;

    File file;

    private static final int SIZE = 100;

    public void setUp() throws Exception {
	super.setUp();
	file = File.createTempFile("error", "log");
	if (file.exists()) {
	    file.delete();
	}
    }

    public void tearDown() throws Exception {
	super.tearDown();
	log.clear();
	file.delete();
    }

    public void testConcurrentWrite() throws Exception {

	class Loader extends Thread {
	    private String name;

	    public Loader(String n) {
		name = n;
	    }

	    public void run() {
		/*
		 * try { for (int i = 0; i < SIZE; i++) {
		 * log.append(ErrorLog.ADD,name + i); //System.out.println(name
		 * + i); } log.flush(); } catch (Exception e) {
		 * System.out.println(e); }
		 */

		try {
		    HashSet<String> add = new HashSet<String>();
		    HashSet<String> rem = new HashSet<String>();

		    for (int j = 0; j < 10; j++) {
			add.clear();
			for (int i = 0; i < 10; i++) {
			    int el = j * 10 + i;
			    add.add(name + el);
			}
			log.writeChanges(rem, add);
		    }

		} catch (Exception e) {
		    System.out.println(e);
		}
	    }
	}

	log = new ErrorLog(file, SearchIndex.DEFAULT_ERRORLOG_FILE_SIZE);

	Thread one = new Loader("first");
	Thread two = new Loader("second");
	one.start();
	two.start();
	one.join();
	two.join();

	List<String> list = log.readList();

	int lost_first = 0;
	int lost_second = 0;
	for (int i = 0; i < SIZE; i++) {
	    String firstname = ErrorLog.ADD + " first" + i;
	    String secondname = ErrorLog.ADD + " second" + i;
	    int ffinded = 0;
	    int sfinded = 0;
	    for (int j = 0; j < list.size(); j++) {
		if (list.get(j).equals(firstname)) {
		    ffinded++;
		}
		if (list.get(j).equals(secondname)) {
		    sfinded++;
		}
	    }
	    if (ffinded == 0) {
		System.out.println(firstname + " NOT FINDED");
	    }
	    if (ffinded > 1) {
		System.out.println(firstname + " DUPLICATED");
	    }
	    if (sfinded == 0) {
		System.out.println(secondname + " NOT FINDED");
	    }
	    if (sfinded > 1) {
		System.out.println(secondname + " DUPLICATED");
	    }
	}

	assertEquals("There is mismatch of expected writed messages count ",
		200, list.size());
	assertEquals("First thread has lost apdates", 0, lost_first);
	assertEquals("Second thread has lost apdates", 0, lost_second);
    }

    public void testExctractNotifyList() throws Exception {
	log = new ErrorLog(file, SearchIndex.DEFAULT_ERRORLOG_FILE_SIZE);

	Set<String> removed = new HashSet<String>();
	Set<String> added = new HashSet<String>();

	for (int i = 0; i < 10; i++) {
	    added.add("uuidadd" + i);
	}

	for (int i = 0; i < 5; i++) {
	    removed.add("uuidrem" + i);
	}

	log.writeChanges(removed, added);

	Set<String> rem = new HashSet<String>();
	Set<String> add = new HashSet<String>();

	log.readChanges(rem, add);

	assertTrue(rem.containsAll(removed));
	assertTrue(add.containsAll(added));
    }

}
