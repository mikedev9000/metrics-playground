package metricsplayground.demo;

import java.util.Random;

import org.springframework.stereotype.Component;

import metricsplayground.instrumentation.Action;
import metricsplayground.instrumentation.ActionInterceptorBinding;
import metricsplayground.instrumentation.Context;

@Component
@ActionInterceptorBinding
public class ExampleAnnotatedService {

	private final Random random = new Random(0);

	@Action("convert")
	public void convertThis(@Context(key = "thing") String thing) {
		try {
			Thread.sleep(50 + random.nextInt(5000));
		} catch (InterruptedException e) {
			// no worries
			e.printStackTrace();
		}

		if (random.nextInt(100) == 1) {
			throw new RuntimeException("convert oops");
		}
	}

	@Action("persist")
	public void persistThis(@Context(key = "thing") String thing) {
		try {
			Thread.sleep(10 + random.nextInt(5000));
		} catch (InterruptedException e) {
			// no worries
			e.printStackTrace();
		}

		if (random.nextInt(3) == 1) {
			throw new RuntimeException("persist oops");
		}
	}

	@Action("find")
	public void findThis(@Context(key = "thing") String thing) {
		try {
			Thread.sleep(5 + random.nextInt(5000));
		} catch (InterruptedException e) {
			// no worries
			e.printStackTrace();
		}

		if (random.nextInt(20) == 1) {
			throw new RuntimeException("find oops");
		}
	}
}
