package metricsplayground.instrumentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import com.codahale.metrics.MetricRegistry;

@Component
@Configuration
@ApplicationScope
public class MetricsHelper {

	@Bean
	public MetricRegistry dropwizardRegistry() {
		return dropwizardRegistry;
	};

	private final MetricRegistry dropwizardRegistry = new MetricRegistry();

	public void meter(String name) {
		dropwizardRegistry.meter(name).mark();
	}

	public TimerContext timer(String name) {
		return new TimerContext(dropwizardRegistry.timer(name).time());
	}

	public void histogram(String name, long value) {
		dropwizardRegistry.histogram(name).update(value);
	}

	public void counterIncrement(String name) {
		dropwizardRegistry.counter(name).inc();
	}

	public void counterDecrement(String name) {
		dropwizardRegistry.counter(name).dec();
	}
}
