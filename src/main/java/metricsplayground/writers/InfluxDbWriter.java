package metricsplayground.writers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;

@Component
public class InfluxDbWriter {

	@Autowired
	MetricRegistry dropwizardRegistry;

	private static class NameAndTags {

		private final String name;
		private final Map<String, String> tags;

		private NameAndTags(String name, Map<String, String> tags) {
			this.name = name;
			this.tags = tags;
		}
	}

	private NameAndTags parseNameAndTags(String type, String metricName) {
		String[] parts = metricName.split(",");

		if (parts.length == 0) {
			throw new IllegalArgumentException("invalid format: " + metricName);
		}

		if (!parts[0].contains("=")) {
			return new NameAndTags(parts[0] + "_" + type, parseTags(Arrays.copyOfRange(parts, 1, parts.length)));
		} else {
			return new NameAndTags(type, parseTags(parts));
		}
	}

	private Map<String, String> parseTags(String[] parts) {
		Map<String, String> tags = new LinkedHashMap<>();

		for (int i = 0; i < parts.length; i++) {
			String[] pair = parts[i].split("=");

			if (pair.length == 2) {
				tags.put(pair[0], pair[1]);
			} else {
				tags.put(Integer.toString(i), parts[i]);
			}
		}

		return tags;
	}

	private final InfluxDB influxDB = InfluxDBFactory.connect("http://192.168.99.100:32771/", "root", "root");

	@Scheduled(initialDelay = 1000, fixedDelay = 1000)
	private void writeData() {
		influxDB.setLogLevel(LogLevel.FULL);
		String dbName = "metrics";

		BatchPoints batchPoints = BatchPoints.database(dbName).tag("host", "g92.1").retentionPolicy("autogen")
				.consistency(ConsistencyLevel.ONE).build();

		int size = dropwizardRegistry.getMetrics().size();

		System.err.println("counters: " + dropwizardRegistry.getCounters().size());
		System.err.println("meters: " + dropwizardRegistry.getMeters().size());
		System.err.println("hisograms: " + dropwizardRegistry.getHistograms().size());
		System.err.println("gauges: " + dropwizardRegistry.getGauges().size());
		System.err.println("timers: " + dropwizardRegistry.getTimers().size());

		dropwizardRegistry.getTimers().forEach((name, timer) -> {
			System.out.println("timer: " + name);

			Snapshot snapshot = timer.getSnapshot();
			long count = timer.getCount();
			Metered meter = timer;

			NameAndTags nameAndTags = parseNameAndTags("timer", name);

			batchPoints.point(Point.measurement(nameAndTags.name)
					.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).addField("count", count)

					// meter fields
					.addField("count", meter.getCount()).addField("fifteenMinuteRate", meter.getFifteenMinuteRate())
					.addField("fiveMinuteRate", meter.getFiveMinuteRate())
					.addField("oneMinuteRate", meter.getOneMinuteRate()).addField("meanRate", meter.getMeanRate())

					// histogram fields
					.addField("count", count).addField("percentile.75th", snapshot.get75thPercentile())
					.addField("percentile.95th", snapshot.get95thPercentile())
					.addField("percentile.98th", snapshot.get98thPercentile())
					.addField("percentile.99th", snapshot.get99thPercentile())
					.addField("percentile.999th", snapshot.get999thPercentile()).addField("max", snapshot.getMax())
					.addField("mean", snapshot.getMean()).addField("median", snapshot.getMedian())
					.addField("stddev", snapshot.getStdDev())

					.tag(nameAndTags.tags)

					.build());
		});

		dropwizardRegistry.getCounters().forEach((name, counter) -> {
			System.out.println("counter: " + name);

			NameAndTags nameAndTags = parseNameAndTags("counter", name);

			batchPoints
					.point(Point.measurement(nameAndTags.name).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
							.addField("count", counter.getCount()).tag(nameAndTags.tags).build());
		});

		dropwizardRegistry.getMeters().forEach((name, meter) -> {
			System.out.println("meter: " + name);

			NameAndTags nameAndTags = parseNameAndTags("meter", name);

			batchPoints.point(Point.measurement(nameAndTags.name)
					.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).addField("count", meter.getCount())
					.addField("fifteenMinuteRate", meter.getFifteenMinuteRate())
					.addField("fiveMinuteRate", meter.getFiveMinuteRate())
					.addField("oneMinuteRate", meter.getOneMinuteRate()).addField("meanRate", meter.getMeanRate())
					.tag(nameAndTags.tags).build());
		});

		dropwizardRegistry.getGauges().forEach((name, gauge) -> {
			System.out.println("gauge: " + name);
			Object value = gauge.getValue();

			NameAndTags nameAndTags = parseNameAndTags("gauge", name);

			if (value instanceof Number) {
				batchPoints.point(
						Point.measurement(nameAndTags.name).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
								.addField("value", (Number) value).tag(nameAndTags.tags).build());
			}
		});

		dropwizardRegistry.getHistograms().forEach((name, histogram) -> {
			System.out.println("histogram: " + name);
			Snapshot snapshot = histogram.getSnapshot();
			long count = histogram.getCount();

			NameAndTags nameAndTags = parseNameAndTags("histogram", name);

			batchPoints.point(Point.measurement(nameAndTags.name)
					.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).addField("count", count)
					.addField("percentile.75th", snapshot.get75thPercentile())
					.addField("percentile.95th", snapshot.get95thPercentile())
					.addField("percentile.98th", snapshot.get98thPercentile())
					.addField("percentile.99th", snapshot.get99thPercentile())
					.addField("percentile.999th", snapshot.get999thPercentile()).addField("max", snapshot.getMax())
					.addField("mean", snapshot.getMean()).addField("median", snapshot.getMedian())
					.addField("stddev", snapshot.getStdDev()).tag(nameAndTags.tags).build());
		});

		System.out.println("writing!");
		influxDB.write(batchPoints);
	}

}
