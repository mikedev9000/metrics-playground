package metricsplayground.instrumentation;

import java.io.Closeable;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class TimerContext implements Closeable {

	private Context context;

	public TimerContext(Timer.Context context) {
		this.context = context;
	}

	@Override
	public void close() {
		context.close();
	}

}
