Summary

    * Status: Empty multi-valued properties should be processed properly
    * CCP Issue: N/A, Product Jira Issue: JCR-1652.
    * Complexity: low

The Proposal
Problem description

What is the problem to fix?

    * When we have empty multi-valued property we receive exception, trying to get it via WebDAV, becuase of trying to get first element of array of property values which is empty.

Fix description

How is the problem fixed?

    * Before trying to get property value we check if array containing it is empty.

Patch file: JCR-1652.patch

Tests to perform

Reproduction test

    * Steps to reproduce:

   1. Upload a CSS file in the content explorer
         1. Login as root in the file explorer
         2. Go in Collaboration/Documents
         3. Create a new folder "TEST"
         4. Upload a new CSS file into this folder
         5. view it
   2. Edit the file in the IDE
         1. Go to Collaboration workspace
         2. Go to /Documents/TEST. You can see your CSS
         3. Modify and save the file
         4. The file is updated, you can modify and save it if you want.
   3. Use the Content Explorer to modify this file
         1. Go back to the file explorer
         2. Go on your file
         3. As you can see the file is updated
         4. You must Check Out the file (yes we are using DAV on the IDE)
         5. Edit it using the File Explorer
         6. Save it
         7. Close it
   4. Go back to the IDE. You cannot get back to the folder/file

Tests performed at DevLevel

    * Unit test were created to cover this issue. It tests for correct response if we're trying to get property value of multi-valued property

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:

    * None

Configuration changes

Configuration changes:

    * None

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * No

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
* Patch validated.

QA Feedbacks
*

