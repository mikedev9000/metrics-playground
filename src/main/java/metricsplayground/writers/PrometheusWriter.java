package metricsplayground.writers;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.PushGateway;

@Component
public class PrometheusWriter {

	@Autowired
	MetricRegistry dropwizardRegistry;

	private final CollectorRegistry prometheusRegistry = new CollectorRegistry();

	// @PostConstruct
	public void initialize() {
		DropwizardExports prometheus = new DropwizardExports(dropwizardRegistry);
		prometheus.register(prometheusRegistry);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(prometheus::collect, 0, 500,
				TimeUnit.MILLISECONDS);

		PushGateway prometheusPush = new PushGateway("192.168.99.100:32770");

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			try {
				prometheusPush.push(prometheusRegistry, "my-job");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}, 0, 500, TimeUnit.MILLISECONDS);
	}

}
