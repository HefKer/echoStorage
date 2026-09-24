package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.link.LinkedChests.State;
import dev.hefker.echostorage.menu.EchoInterfaceMenuData;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** Everything the Echo Interface sends, each way, must arrive as it left. */
class EchoInterfacePayloadTest {
	private static final List<Row> ROWS = List.of(
			new Row(UUID.randomUUID(), new BlockPos(1, 2, 3), "Ores", Optional.empty(), State.LINKED),
			new Row(UUID.randomUUID(), new BlockPos(-4, 70, 9), "", Optional.of(Categories.FOOD), State.LOST));

	@Test
	void theOpenDataSurvivesTheWire() {
		EchoInterfaceMenuData sent = new EchoInterfaceMenuData(ROWS);
		assertEquals(sent, CodecRoundTrip.of(EchoInterfaceMenuData.STREAM_CODEC, sent));
	}

	@Test
	void newerRowsSurviveTheWire() {
		EchoInterfaceRowsPayload sent = new EchoInterfaceRowsPayload(7, ROWS);
		assertEquals(sent, CodecRoundTrip.of(EchoInterfaceRowsPayload.STREAM_CODEC, sent));
	}

	@Test
	void anOpenRequestSurvivesTheWire() {
		OpenLinkedChestPayload sent = new OpenLinkedChestPayload(7, UUID.randomUUID());
		assertEquals(sent, CodecRoundTrip.of(OpenLinkedChestPayload.STREAM_CODEC, sent));
	}

	@Test
	void aDismissRequestSurvivesTheWire() {
		DismissLinkedChestPayload sent = new DismissLinkedChestPayload(7, UUID.randomUUID());
		assertEquals(sent, CodecRoundTrip.of(DismissLinkedChestPayload.STREAM_CODEC, sent));
	}
}
