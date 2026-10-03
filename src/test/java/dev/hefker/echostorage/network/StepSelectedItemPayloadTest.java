package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hefker.echostorage.CodecRoundTrip;
import org.junit.jupiter.api.Test;

class StepSelectedItemPayloadTest {
	@Test
	void aStepEitherWaySurvivesTheWire() {
		assertEquals(new StepSelectedItemPayload(1), CodecRoundTrip.of(StepSelectedItemPayload.STREAM_CODEC, new StepSelectedItemPayload(1)));
		assertEquals(new StepSelectedItemPayload(-1), CodecRoundTrip.of(StepSelectedItemPayload.STREAM_CODEC, new StepSelectedItemPayload(-1)));
	}

	@Test
	void payloadTypeIsNamespacedToTheMod() {
		assertEquals("echostorage:step_selected_item", StepSelectedItemPayload.TYPE.id().toString());
	}
}
