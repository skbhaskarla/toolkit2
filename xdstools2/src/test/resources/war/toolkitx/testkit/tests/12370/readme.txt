12370 R.b Accept association documenation classification

Lifecycle management type associations may contain an optional classification labeling the
reason for the update.  This association may be present on RPLC, APND, XFRM etc. assocations.

Sub Tests

not_configured - attempt to add this association on a HasMember assocation - fails

submit - submit a document

rplc - replace the document and the RPLC association has the documentation classification

query - validate the documentation classification is returned in a stored query 
