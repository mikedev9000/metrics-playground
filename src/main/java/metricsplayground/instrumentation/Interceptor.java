package metricsplayground.instrumentation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.logging.NDC;
import org.mvel2.MVEL;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@javax.interceptor.Interceptor
@ActionInterceptorBinding
public class Interceptor {

	@Autowired
	MetricsHelper metrics;

	private Object proceed(InvocationContext ctx) throws Exception {
		// TODO - determine if InvocationTargetException get thrown here,
		// unwrap if so
		return ctx.proceed();
	}

	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {
		Action action = ctx.getMethod().getAnnotation(Action.class);

		if (action == null) {
			return proceed(ctx);
		}

		System.err.println("????? intercepted!!!!!");

		NDC.push(action.value());

		Map<String, String> instrumentationContext = buildInstrumentationContext(ctx);

		List<MDCCloseable> mdcs = instrumentationContext.entrySet().stream()
				.map((entry) -> MDC.putCloseable(entry.getKey(), entry.getValue())).collect(Collectors.toList());

		String instrumentationContextString = instrumentationContext.entrySet().stream()
				.map((entry) -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(","));

		instrumentationContextString = String.join(",", action.value(), instrumentationContextString);

		try (TimerContext timer = metrics.timer(String.join(",", action.value(), instrumentationContextString))) {
			return proceed(ctx);
		} catch (Exception e) {
			metrics.meter(String.join(",", action.value() + ".failure", instrumentationContextString));
			throw e;
		} finally {
			mdcs.forEach(MDCCloseable::close);

			if (action != null) {
				NDC.pop();
			}
		}
	}

	private Map<String, String> buildInstrumentationContext(InvocationContext ctx) {

		Map<String, String> result = new LinkedHashMap<>();

		for (int i = 0; i < ctx.getMethod().getParameterCount(); i++) {
			Object parameter = ctx.getParameters()[i];

			Stream.of(ctx.getMethod().getParameterAnnotations()[i])
					.filter((annotation) -> annotation instanceof Context).map((annotation) -> (Context) annotation)
					.forEach((context) -> result.put(context.key(), convertParameterToString(parameter, context)));
		}

		return result;
	}

	private String convertParameterToString(Object parameter, Context contextAnnotation) {
		// TODO - keep a cache of compiled expressions
		Serializable expression = MVEL.compileExpression(contextAnnotation.valueExpression());

		return MVEL.executeExpression(expression, parameter, new HashMap<>(), String.class);
	}
}
