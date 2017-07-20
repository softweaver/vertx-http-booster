package io.openshift.booster;

import com.jayway.restassured.RestAssured;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.openshift.booster.test.OpenShiftTestAssistant;
import org.arquillian.cube.kubernetes.api.Session;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static io.openshift.booster.HttpApplication.template;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(Arquillian.class)
public class OpenShiftIT {
	private String project;

    @ArquillianResource
    private OpenShiftClient client;

    @ArquillianResource
    private Session session;

    private final String applicationName = "http-vertx";

    @Before
    public void setup() {
        final Route route = this.client.adapt(OpenShiftClient.class)
                .routes()
                .inNamespace(this.session.getNamespace())
                .withName(this.applicationName)
                .get();
        Assertions.assertThat(route)
                .isNotNull();
        RestAssured.baseURI = String.format("http://%s", Objects.requireNonNull(route)
                .getSpec()
                .getHost());
        project = this.client.getNamespace();
	    System.out.println("\nRoute is: " + route.getSpec().getHost() + "\n");
    }

    @Test
    public void testThatWeAreReady() throws Exception {
	    await().atMost(5, TimeUnit.MINUTES).until(() -> {
				    List<Pod> list = client.pods().inNamespace(project).list().getItems();
				    return list.stream()
						    .filter(pod -> pod.getMetadata().getName().startsWith(applicationName))
						    .filter(this::isRunning)
						    .collect(Collectors.toList()).size() >= 1;
			    }
	    );
        // Check that the route is served.
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> get().getStatusCode() < 500);
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> get("/api/greeting")
            .getStatusCode() < 500);

	    System.out.println("\nStatus code: " + get("/api/greeting").getStatusCode() + "\n");
    }

    @Test
    public void testThatWeServeAsExpected() throws MalformedURLException {
        get("/api/greeting").then().body("content", equalTo(String.format(template, "World")));
        get("/api/greeting?name=vert.x").then().body("content", equalTo(String.format(template, "vert.x")));
    }

	private boolean isRunning(Pod pod) {
		return "running".equalsIgnoreCase(pod.getStatus().getPhase());
	}

//	@AfterClass
//	public static void cleanUp() {
//		List<String> keys = new ArrayList<>(created.keySet());
//		keys.sort(String::compareTo);
//		for (String key : keys) {
//			created.remove(key)
//					.stream()
//					.sorted(Comparator.comparing(HasMetadata::getKind))
//					.forEach(metadata -> {
//						System.out.println(String.format("Deleting %s : %s", key, metadata.getKind()));
//						deleteWithRetries(metadata);
//					});
//		}
//	}

}
