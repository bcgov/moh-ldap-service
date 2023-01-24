package ca.bc.gov.hlth.ldapapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Component
public class RestTemplateConfiguration {
    private final String proxyType;

    private final String proxyHost;

    private final int proxyPort;

    private final RestTemplate nonProxyRestTemplate;

    private final RestTemplate proxyRestTemplate;

    public RestTemplateConfiguration(@Value("${proxy.type}") String proxyType,
                                     @Value("${proxy.host}") String proxyHost,
                                     @Value("${proxy.port}") int proxyPort) {
        this.proxyType = proxyType;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        nonProxyRestTemplate = new RestTemplate();
        proxyRestTemplate = new RestTemplate(configureProxyForOrgApi());
    }

    public RestTemplate getNonProxyRestTemplate() {
        return nonProxyRestTemplate;
    }

    public RestTemplate getProxyRestTemplate() {
        return proxyRestTemplate;
    }

    /**
     * on local environment: set proxy.type=DIRECT in application.properties to skip the proxy config part
     */
    private SimpleClientHttpRequestFactory configureProxyForOrgApi() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Proxy proxy;

        if(Proxy.Type.valueOf(proxyType) == Proxy.Type.DIRECT){
            proxy = Proxy.NO_PROXY;
        } else {
            proxy = new Proxy(Proxy.Type.valueOf(proxyType), new InetSocketAddress(proxyHost, proxyPort));
        }

        requestFactory.setProxy(proxy);
        return requestFactory;
    }
}
