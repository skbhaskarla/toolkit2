SQ.b GetDocuments Stored Query

This test relies on test 12346 to pre-load test data.

Stored Query must use SOAP version 1.2 

Test Steps

uniqueid
	Operation with UniqueId

uniqueid2
	Operation with multiple UniqueIds

uuid
	Operation with UUID

uuid2
	Operation with multiple UUIDs

uuid_multiple_slot_values
	SQ parameter values spread across multiple Value elements
of a Slot.  (CP 295)

homeCommunityId
	Section 3.18.4.1.3, part 4, For Document Registry Actors says
	that the Document Registry must accept the homeCommunityId
	on a Stored Query. This test repeats step uniqueid above
	but with the homeCommunityId present.

