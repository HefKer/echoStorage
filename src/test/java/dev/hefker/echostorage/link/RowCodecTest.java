package dev.hefker.echostorage.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonArray;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

/** A row is saved with the interface and sent to the screen; both must give back what went in. */
class RowCodecTest {
	private static final ResourceLocation DEEP = ResourceLocation.fromNamespaceAndPath("echostorage", "deep_echo_chest");
	private static final ResourceLocation SHULKER = ResourceLocation.fromNamespaceAndPath("echostorage", "echo_shulker_box");
	private static final Row ORES = new Row(UUID.randomUUID(), new BlockPos(3, 64, -9), DEEP, Optional.empty(), "Ores",
			Optional.of(Categories.ORES), State.UNLOADED);
	private static final Row BLANK = new Row(UUID.randomUUID(), BlockPos.ZERO, LinkedChests.ECHO_CHEST_ITEM, Optional.empty(), "",
			Optional.empty(), State.LOST);
	private static final Row DYED = new Row(UUID.randomUUID(), new BlockPos(0, 5, 0), SHULKER, Optional.of(DyeColor.LIME), "Food",
			Optional.of(Categories.FOOD), State.LINKED);

	@Test
	void aRowSurvivesTheWire() {
		assertEquals(ORES, CodecRoundTrip.of(Row.STREAM_CODEC, ORES));
		assertEquals(BLANK, CodecRoundTrip.of(Row.STREAM_CODEC, BLANK));
		assertEquals(DYED, CodecRoundTrip.of(Row.STREAM_CODEC, DYED));
	}

	@Test
	void aRowSurvivesASaveAndLoad() {
		for (Row row : new Row[] {ORES, BLANK, DYED}) {
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
	void aRowSavedWithAGarbageNameAndCategoryLoadsWithNeitherAndKeepsItsPlace() {
		var saved = Row.CODEC.encodeStart(JsonOps.INSTANCE, ORES).getOrThrow().getAsJsonObject();
		saved.addProperty("category", 5);
		JsonArray garbage = new JsonArray();
		garbage.add(1);
		saved.add("name", garbage);

		assertEquals(new Row(ORES.id(), ORES.pos(), ORES.item(), ORES.color(), "", Optional.empty(), ORES.state()),
				Row.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
	}

	@Test
	void aRowSavedBeforeRowsNamedTheirChestLoadsAsAnEchoChestRow() {
		var saved = Row.CODEC.encodeStart(JsonOps.INSTANCE, ORES).getOrThrow().getAsJsonObject();
		saved.remove("item");

		assertEquals(LinkedChests.ECHO_CHEST_ITEM, Row.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().item());
	}

	@Test
	void aRowSavedWithAGarbageItemLoadsAsAnEchoChestRowAndKeepsItsPlace() {
		var saved = Row.CODEC.encodeStart(JsonOps.INSTANCE, ORES).getOrThrow().getAsJsonObject();
		saved.addProperty("item", "Not An Id!");

		assertEquals(new Row(ORES.id(), ORES.pos(), LinkedChests.ECHO_CHEST_ITEM, ORES.color(), ORES.name(), ORES.category(), ORES.state()),
				Row.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
	}

	@Test
	void aRowSavedBeforeRowsHadAColourLoadsUndyed() {
		var saved = Row.CODEC.encodeStart(JsonOps.INSTANCE, DYED).getOrThrow().getAsJsonObject();
		saved.remove("color");

		assertEquals(new Row(DYED.id(), DYED.pos(), DYED.item(), Optional.empty(), DYED.name(), DYED.category(), DYED.state()),
				Row.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
	}

	@Test
	void aRowSavedWithAGarbageColourLoadsUndyedAndKeepsItsPlace() {
		var saved = Row.CODEC.encodeStart(JsonOps.INSTANCE, DYED).getOrThrow().getAsJsonObject();
		saved.addProperty("color", "plaid");

		assertEquals(Optional.empty(), Row.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().color());
	}

	@Test
	void aRowStateTheWireDoesNotKnowIsRefusedRatherThanReadAsOpenable() {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
		Row.STREAM_CODEC.encode(buf, ORES);
		buf.setByte(buf.writerIndex() - 1, State.values().length);

		assertThrows(DecoderException.class, () -> Row.STREAM_CODEC.decode(buf));
	}
}
