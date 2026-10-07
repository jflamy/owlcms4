package app.owlcms.monitors;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;

public class ResourceTableEncoder extends EncoderBase<ILoggingEvent> {
	private final List<String> collectorNames = ManagementFactory.getGarbageCollectorMXBeans().stream()
			.map(GarbageCollectorMXBean::getName).toList();

	@Override
	public byte[] headerBytes() {
		return bytes(ResourceSample.tableHeader(this.collectorNames));
	}

	@Override
	public byte[] encode(ILoggingEvent event) {
		return bytes(event.getFormattedMessage());
	}

	@Override
	public byte[] footerBytes() {
		return null;
	}

	private static byte[] bytes(String line) {
		return (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
	}
}
