Summary

    Status: Can't access file containing special characters in file name via Webdav
    CCP Issue: CCP-875, Product Jira Issue: JCR-1605.
    Complexity: Low

The Proposal
Problem description

What is the problem to fix?

     Can't open file containing special characters (e.g Japanese characters) in file name/path via WebDav in Nautilus. In case of cadaver, when using "ls" command, cadaver can't list such files.

Fix description

How is the problem fixed?

     Set the selected tab after user add any permission to the wanted one.

Patch information: JCR-1605.patch

Tests to perform

Reproduction test
* Steps to reproduce:
With WebDav, Ubuntu:

1. Go to Sites Management/acme/documents, upload a file containing Japanese characters in file name (e.g あいうえお.txt)
2. Open http://localhost:8080/ecmdemo/rest-ecmdemo/jcr/repository/collaboration in WebDav Nautilus
3. Can't open あいうえお.txt file
Similar problem occurs if opening a file containing accented characters (e.g é)

With WebDav, Windows XP:

1. With file containing Japanese characters in filename: can't open folder containing the file.
2. With file containing accented characters (e.g é): can open folder containing the file but can't open the file.

With cadaver:

1. Upload a file containing Japanese characters in file name (e.g あいうえお.txt) in Site Explorer/collaboration
2. Open http://localhost:8080/ecmdemo/rest-ecmdemo/jcr/repository/collaboration in cadaver
3. Use "ls" command: the file isn't listed: not OK
Similar problem occurs if opening a file containing accented characters (e.g é)

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
* No

Configuration changes

Configuration changes:
* No

Will previous configuration continue to work?
* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    Function or ClassName change

Is there a performance risk/cost?
*

Validation (PM/Support/QA)

PM Comment
*

Support Comment
*

QA Feedbacks
*

