package gov.nist.toolkit.xdstools2.client.widgets.queryFilter;

import java.util.List;
import java.util.Map;

/**
 * Created by bill on 9/1/15.
 */
public interface QueryFilter {
    void addToCodeSpec(Map<String, List<String>> codeSpec, String codeType);
}
