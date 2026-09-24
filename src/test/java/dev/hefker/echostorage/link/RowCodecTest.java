package dev.hefker.echostorage.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.JsonOps;
import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.link.LinkedChests.State;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** A row is saved with the interface and sent to the screen; both must give back what went in. */
class RowCodecTest {
	private static final Row ORES = new Row(UUID.randomUUID(), new BlockPos(3, 64, -9), "Ores",
			Optional.of(Categories.ORES), State.UNLOADED);
	private static final Row BLANK = new Row(UUID.randomUUID(), BlockPos.ZERO, "", Optional.empty(), State.LOST);

	@Test
	void aRowSurvivesTheWire() {
		assertEquals(ORES, CodecRoundTrip.of(Row.STREAM_CODEC, ORES));
		assertEquals(BLANK, CodecRoundTrip.of(Row.STREAM_CODEC, BLANK));
	}

	@Test
	void aRowSurvivesASaveAndLoad() {
		for (Row row : new Row[] {ORES, BLANK}) {
			Tag saved = Row.CODEC.encodeStart(NbtOps.INSTANCE, row).getOrThrow();
			assertEquals(row, Row.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
		}
	}

	@Test
	void aRowSavedWithACategoryThatNoLongerShipsLoadsWithNone() {
		var saved = Row.CODEC.encodeStart(JsonOps.INSTANCE, ORES).getOrThrow().getAsJsonObject();
		saved.addProperty("category", "gone");

		assertEquals(Optional.empty(), Row.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().category());
	}

	@Test
	void aRowStateTheWireDoesNotKnowIsRefusedRatherThanReadAsOpenable() {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
		Row.STREAM_CODEC.encode(buf, ORES);
		buf.setByte(buf.writerIndex() - 1, State.values().length);

		assertThrows(DecoderException.class, () -> Row.STREAM_CODEC.decode(buf));
	}
}
