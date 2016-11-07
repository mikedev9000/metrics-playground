package metricsplayground.demo;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoDriver {

	private Random random = new Random(1);

	@Autowired
	ExampleAnnotatedService service;

	private final String[] things = { "Person", "TimeMachine", "AirplaneSeat" };

	private final Runnable[] runnables = { () -> {
		service.convertThis(random(things));
	}, () -> {
		service.persistThis(random(things));
	}, () -> {
		service.findThis(random(things));
	} };

	@Scheduled(initialDelay = 0, fixedDelay = 500)
	public synchronized void run() {
		if (random.nextBoolean()) {
			System.out.println(">>>>>>>> running a random service call now!>!?!?!?");
			random(runnables).run();
		}
	}

	private synchronized <T> T random(T[] things) {
		return things[random.nextInt(things.length)];
	}

}
