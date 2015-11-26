package org.keycloak.adapters.spi;

import javax.security.cert.X509Certificate;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface HttpFacade {
    Request getRequest();

    Response getResponse();

    X509Certificate[] getCertificateChain();

    interface Request {

        String getMethod();
        /**
         * Full request URI with query params
         *
         * @return
         */
        String getURI();

        /**
         * HTTPS?
         *
         * @return
         */
        boolean isSecure();

        /**
         * Get first query or form param
         *
         * @param param
         * @return
         */
        String getFirstParam(String param);
        String getQueryParamValue(String param);
        Cookie getCookie(String cookieName);
        String getHeader(String name);
        List<String> getHeaders(String name);
        InputStream getInputStream();

        String getRemoteAddr();
        void setError(AuthenticationError error);
        void setError(LogoutError error);
    }

    interface Response {
        void setStatus(int status);
        void addHeader(String name, String value);
        void setHeader(String name, String value);
        void resetCookie(String name, String path);
        void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly);
        OutputStream getOutputStream();
        void sendError(int code);
        void sendError(int code, String message);

        /**
         * If the response is finished, end it.
         *
         */
        void end();
    }

    public class Cookie {
        protected String name;
        protected String value;
        protected int version;
        protected String domain;
        protected String path;

        public Cookie(String name, String value, int version, String domain, String path) {
            this.name = name;
            this.value = value;
            this.version = version;
            this.domain = domain;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public int getVersion() {
            return version;
        }

        public String getDomain() {
            return domain;
        }

        public String getPath() {
            return path;
        }
    }
}
