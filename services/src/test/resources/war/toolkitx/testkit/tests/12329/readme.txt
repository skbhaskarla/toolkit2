Special Allocate Patient ID for use with Public Registry

This test contacts the PID allocate service as configured in actors.xml.  It allocates a new Patient ID and stored it in xdstestkit/xdstest/admin. This is needed by Document Repository testers to initialize their copy of the testkit.  Once initialized, the same Patient ID can be reused.  Note, some tests force the allocation of new patient ids so don't count on the Patient ID you allocatewith this test remaining the default.
