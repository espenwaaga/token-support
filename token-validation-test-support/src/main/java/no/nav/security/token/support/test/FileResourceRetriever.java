package no.nav.security.token.support.test;

/*
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jose.util.Resource;

import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;

@Deprecated
public class FileResourceRetriever extends ProxyAwareResourceRetriever {

    private final String metadataFile;
    private final String jwksFile;

    public FileResourceRetriever(String metadataFile, String jwksFile) {
        this.metadataFile = metadataFile;
        this.jwksFile = jwksFile;
    }

    @Override
	public Resource retrieveResource(URL url) {
		String content = getContentFromFile(url);
    	return content != null ? new Resource(content, "application/json") : null;
	}

    private String getContentFromFile(URL url){
    	try {
	    	if (url.toString().contains("metadata")) {
	            return IOUtils.readInputStreamToString( getInputStream(metadataFile), StandardCharsets.UTF_8);
	        }
	        if (url.toString().contains("jwks")) {
	            return IOUtils.readInputStreamToString(getInputStream(jwksFile), StandardCharsets.UTF_8);
	        }
	        return null;
    	 } catch (IOException e) {
             throw new RuntimeException(e);
         }
    }

    private InputStream getInputStream(String file) {
    	return FileResourceRetriever.class.getResourceAsStream(file);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [metadataFile=" + metadataFile + ", jwksFile=" + jwksFile + "]";
    }
}
