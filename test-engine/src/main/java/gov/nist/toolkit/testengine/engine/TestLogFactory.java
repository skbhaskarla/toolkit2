package gov.nist.toolkit.testengine.engine;

public class TestLogFactory {
	static OmLogger logger;
	
	static {
		logger = new OmLogger();
	}
	
	public OmLogger getLogger() { return logger; }
	
	public TestLogFactory() {}
	
}
