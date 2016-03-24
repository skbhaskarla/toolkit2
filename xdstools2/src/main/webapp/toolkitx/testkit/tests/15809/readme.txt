Retrieve Single Responding Gateway test

<p>This test can only be run if test 15808 Query Single Responding Gateway test is
successful.</p>

<h2>OneDocRetrieve</h2>
<p>
A Retrieve request is sent to the Initiating Gateway
for the Document corresponding to the DocumentEntry queried in test
15808/SingleDocumentFindDocuments. This is the Document corresponding to the
Single document in Community 1 Patient ID.
</p>

<p>
The following validations are performed:
<ul>
<li>Schema and metadata formatting
<li>Returned Home Community Id matches Community configuration
<li>Returned Repository Unique Id matches Community configuration
<li>Returned Document Mime Type matches  configuration
<li>Returned DocumentEntry Unique Id matches  configuration
</ul>
</p>

<h2>TwoDocRetrieve</h2>
<p>
A Retrieve request is sent to the Initiating Gateway
for the two Documents corresponding to the DocumentEntries queried in
test 15808/TwoDocumentFindDocuments.
This is the Document corresponding to the
Two documents in Community 1 Patient ID.
</p>

<p>
The following validations are performed:
<ul>
<li>Schema and metadata formatting
<li>Returned Home Community Id matches Community configuration
<li>Returned Repository Unique Id matches Community configuration
<li>Returned Document Mime Type matches  configuration
<li>Returned DocumentEntry Unique Id matches  configuration
</ul>
</p>
