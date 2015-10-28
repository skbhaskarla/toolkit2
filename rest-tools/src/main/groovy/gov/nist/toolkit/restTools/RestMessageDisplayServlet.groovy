package gov.nist.toolkit.restTools

import org.apache.log4j.Logger

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RestMessageDisplayServlet extends HttpServlet {
    static logger = Logger.getLogger(RestMessageDisplayServlet);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        request.headerNames.each { hdrName ->
            request.getHeaders(hdrName).each { header ->
                logger.info("${hdrName}: ${header}")
            }
        }
        logger.info ""
        logger.info request.reader.text
    }

}
