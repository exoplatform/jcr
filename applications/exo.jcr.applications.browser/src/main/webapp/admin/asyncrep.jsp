<html>
<head>
  <title>eXo Platform JCR browser sample REPOSITORY NOT FOUND</title>
	<link rel="stylesheet" href="../exojcrstyle.css">
</head>
<body>
  <h1>AsyncReplication manual testing tool localhost:8080</h1>
  <form action="http://localhost:8080/rest/async-test/addAsyncFolder" method="get"><input name="submit" type="submit" value="Create testing root node AsyncFolder"></form>
  <hr>
  <form action="http://localhost:8080/rest/async-test/addFileA" method="get"><input name="submit" type="submit" value="Add fileA.txt"></form>
  <form action="http://localhost:8080/rest/async-test/addFolder1" method="get"><input name="submit" type="submit" value="Add folder1"></form>
  <hr>
  <form action="http://localhost:8080/rest/async-test/checkinCheckoutFileA" method="get"><input name="submit" type="submit" value="Make checkin/checkout for fileA.txt"></form>
  <form action="http://localhost:8080/rest/async-test/restoreFileA" method="get"><input name="submit" type="submit" value="Restore fileA.txt"></form>
  <form action="http://localhost:8080/rest/async-test/deleteFileA" method="get"><input name="submit" type="submit" value="Delete fileA.txt"></form>
  <form action="http://localhost:8080/rest/async-test/moveFileA2Folder1" method="get"><input name="submit" type="submit" value="Move fileA.txt to folder1"></form>
  <form action="http://localhost:8080/rest/async-test/editFileASetValueL" method="get"><input name="submit" type="submit" value="Edit fileA.txt: set valueL"></form>
  <form action="http://localhost:8080/rest/async-test/editFileASetValueH" method="get"><input name="submit" type="submit" value="Edit fileA.txt: set valueH"></form>
  <hr>
  <form action="http://localhost:8080/rest/async-test/clean" method="get"><input name="submit" type="submit" value="Delete testing root node AsyncFolder"></form>
  <h1>AsyncReplication manual testing tool localhost:9080</h1>
  <form action="http://localhost:9080/rest/async-test/addAsyncFolder" method="get"><input name="submit" type="submit" value="Create testing root node AsyncFolder"></form>
  <hr>
  <form action="http://localhost:9080/rest/async-test/addFileA" method="get"><input name="submit" type="submit" value="Add fileA.txt"></form>
  <form action="http://localhost:9080/rest/async-test/addFolder1" method="get"><input name="submit" type="submit" value="Add folder1"></form>
  <hr>
  <form action="http://localhost:9080/rest/async-test/checkinCheckoutFileA" method="get"><input name="submit" type="submit" value="Make checkin/checkout for fileA.txt"></form>
  <form action="http://localhost:9080/rest/async-test/restoreFileA" method="get"><input name="submit" type="submit" value="Restore fileA.txt"></form>
  <form action="http://localhost:9080/rest/async-test/deleteFileA" method="get"><input name="submit" type="submit" value="Delete fileA.txt"></form>
  <form action="http://localhost:9080/rest/async-test/moveFileA2Folder1" method="get"><input name="submit" type="submit" value="Move fileA.txt to folder1"></form>
  <form action="http://localhost:9080/rest/async-test/editFileASetValueL" method="get"><input name="submit" type="submit" value="Edit fileA.txt: set valueL"></form>
  <form action="http://localhost:9080/rest/async-test/editFileASetValueH" method="get"><input name="submit" type="submit" value="Edit fileA.txt: set valueH"></form>
  <hr>
  <form action="http://localhost:9080/rest/async-test/clean" method="get"><input name="submit" type="submit" value="Delete testing root node AsyncFolder"></form>
</body>  
</html>
