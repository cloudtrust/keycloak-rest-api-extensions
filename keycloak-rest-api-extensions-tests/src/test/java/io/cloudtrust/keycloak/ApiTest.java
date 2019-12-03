package io.cloudtrust.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.test.FluentTestsHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class ApiTest {
    private static final String KEYCLOAK_URL = getKeycloakUrl();

    protected RealmResource testRealm;
    protected Keycloak keycloak;
    protected String token;

    private static String getKeycloakUrl() {
        String url = FluentTestsHelper.DEFAULT_KEYCLOAK_URL;
        try {
            URI uri = new URI(FluentTestsHelper.DEFAULT_KEYCLOAK_URL);
            url = url.replace(String.valueOf(uri.getPort()), System.getProperty("auth.server.http.port", "8080"));
        } catch (Exception e) {
            // Ignore
        }
        return url;
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "run-on-server-classes.war")
                .addPackages(true, "io.cloudtrust.keycloak")
                .addAsManifestResource(new File("src/test/resources", "manifest.xml"))
                .addAsServiceProvider(RealmResourceProviderFactory.class, RealmResourceProviderFactory.class);
    }

    @Before
    public void createTestRealm() throws IOException {
        keycloak = Keycloak.getInstance(KEYCLOAK_URL, "master", "admin", "admin", "admin-cli");
        token = keycloak.tokenManager().getAccessTokenString();
        testRealm = importTestRealm(keycloak);
    }

    @After
    public void deleteTestRealm() {
        testRealm.remove();
    }

    private RealmResource importTestRealm(Keycloak keycloak) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        RealmRepresentation realmRepresentation = mapper.readValue(
                getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        keycloak.realms().create(realmRepresentation);
        return keycloak.realm("test");
    }

    protected String callApi(String apiPath) throws IOException, URISyntaxException {
        return callApi(apiPath, new ArrayList<>());
    }

    protected String callApi(String apiPath, List<NameValuePair> nvps) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            URIBuilder uriBuilder = new URIBuilder(KEYCLOAK_URL + "/realms/master/api/" + apiPath );
            uriBuilder.addParameters(nvps);
            HttpGet get = new HttpGet(uriBuilder.build());
            get.addHeader("Authorization", "Bearer " + token);

            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "call failed: "+ response.getStatusLine().getStatusCode());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))){
                return reader.lines().collect(Collectors.joining());
            }
        }
    }
}
