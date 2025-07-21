package com.gradientgeeks.ageis.backendapp.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that wraps requests to allow multiple reads of request body.
 * This is needed for signature validation which requires reading the body.
 */
public class RepeatableRequestBodyFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if ("POST".equalsIgnoreCase(httpRequest.getMethod()) || 
                "PUT".equalsIgnoreCase(httpRequest.getMethod())) {
                RepeatableRequestWrapper wrappedRequest = new RepeatableRequestWrapper(httpRequest);
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * Request wrapper that caches the request body for multiple reads.
     */
    private static class RepeatableRequestWrapper extends HttpServletRequestWrapper {
        
        private final byte[] body;
        private final Map<String, String[]> parameterMap;
        
        public RepeatableRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            
            // Cache the request body
            InputStream inputStream = request.getInputStream();
            this.body = StreamUtils.copyToByteArray(inputStream);
            
            // Cache parameters as well
            this.parameterMap = new HashMap<>(request.getParameterMap());
        }
        
        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(this.body);
        }
        
        @Override
        public BufferedReader getReader() throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.body);
            return new BufferedReader(new InputStreamReader(byteArrayInputStream, getCharacterEncoding()));
        }
        
        @Override
        public String getParameter(String name) {
            String[] values = parameterMap.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }
        
        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(parameterMap);
        }
        
        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parameterMap.keySet());
        }
        
        @Override
        public String[] getParameterValues(String name) {
            return parameterMap.get(name);
        }
    }
    
    /**
     * ServletInputStream implementation that reads from a byte array.
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        
        private final ByteArrayInputStream inputStream;
        
        public CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }
        
        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }
        
        @Override
        public boolean isReady() {
            return true;
        }
        
        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}