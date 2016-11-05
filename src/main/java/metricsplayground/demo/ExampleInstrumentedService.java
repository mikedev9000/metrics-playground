package metricsplayground.demo;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import metricsplayground.instrumentation.MetricsHelper;
import metricsplayground.instrumentation.TimerContext;

@Component
public class ExampleInstrumentedService {

	@Autowired
	MetricsHelper metrics;

	private final Random random = new Random(0);

	public void convertThis(String thing) {
		try (TimerContext timer = metrics.timer("convert,thing=" + thing)) {
			try {
				Thread.sleep(50 + random.nextInt(5000));
			} catch (InterruptedException e) {
				// no worries
				e.printStackTrace();
			}

			if (random.nextInt(100) == 1) {
				metrics.meter("convert_failure,thing=" + thing);
			}
		}
	}

	public void persistThis(String thing) {
		try (TimerContext timer = metrics.timer("persist_thing=" + thing)) {
			try {
				Thread.sleep(10 + random.nextInt(5000));
			} catch (InterruptedException e) {
				// no worries
				e.printStackTrace();
			}

			if (random.nextInt(3) == 1) {
				metrics.meter("persist_failure,thing=" + thing);
			}
		}
	}

	public void findThis(String thing) {
		try (TimerContext timer = metrics.timer("find,thing=" + thing)) {
			try {
				Thread.sleep(5 + random.nextInt(5000));
			} catch (InterruptedException e) {
				// no worries
				e.printStackTrace();
			}

			if (random.nextInt(20) == 1) {
				metrics.meter("find_failure,thing=" + thing);
			}
		}
	}
}
