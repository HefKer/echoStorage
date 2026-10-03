package dev.hefker.echostorage.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoBundleItem;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

/**
 * Helpers and constants the Echo Chest, Echo Bundle and Echo Interface game tests share.
 *
 * <p>GameTestHelper.assertValueEqual takes (actual, expected, what).
 */
final class EchoChestTests {
	/** What {@code @GameTest} defaults to, for the tests {@link #forEveryKind} makes. */
	private static final String DEFAULT_BATCH = "defaultBatch";
	private static final int DEFAULT_TIMEOUT_TICKS = 100;
	static final BlockPos CHEST = new BlockPos(1, 1, 1);
	static final BlockPos NEIGHBOUR = new BlockPos(2, 1, 1);
	/** The first slot of the player's main inventory, above the hotbar. */
	static final int FIRST_MAIN_INVENTORY_SLOT = 9;

	private EchoChestTests() {
	}

	/** Each kind of Echo Chest, for the tests that {@link #forEveryKind} runs once per kind. */
	enum ChestKind {
		ECHO(EchoBlocks.ECHO_CHEST, EchoItems.ECHO_CHEST, 27, false),
		DEEP(EchoBlocks.DEEP_ECHO_CHEST, EchoItems.DEEP_ECHO_CHEST, 54, false),
		SHULKER(EchoBlocks.ECHO_SHULKER_BOX, EchoItems.ECHO_SHULKER_BOX, 27, true);

		final Block block;
		final Item item;
		final int slots;
		/** Whether it keeps its contents when broken, rather than spilling them. */
		final boolean keepsContents;

		ChestKind(Block block, Item item, int slots, boolean keepsContents) {
			this.block = block;
			this.item = item;
			this.slots = slots;
			this.keepsContents = keepsContents;
		}

		ResourceLocation id() {
			return BuiltInRegistries.ITEM.getKey(item);
		}
	}

	/**
	 * Marks a test that takes a {@link ChestKind} after its helper, to be run once for each kind by
	 * a {@code @GameTestGenerator} that returns {@link #forEveryKind} of its class.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface EveryChestKind {
		/** Leaves out the kinds that keep their contents when broken, for what only a chest that spills does. */
		boolean placedOnly() default false;
	}

	/**
	 * One test function per kind for each {@link EveryChestKind} method of {@code tests}, named
	 * after the method and the kind, in the empty structure with vanilla's default timeout.
	 */
	static Collection<TestFunction> forEveryKind(Class<? extends FabricGameTest> tests) {
		String suite = tests.getSimpleName().toLowerCase(Locale.ROOT);
		List<TestFunction> functions = new ArrayList<>();
		Arrays.stream(tests.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(EveryChestKind.class))
				.sorted(Comparator.comparing(Method::getName))
				.forEach(method -> {
					for (ChestKind kind : ChestKind.values()) {
						if (kind.keepsContents && method.getAnnotation(EveryChestKind.class).placedOnly()) {
							continue;
						}
						String name = suite + "." + method.getName().toLowerCase(Locale.ROOT) + "_" + kind.name().toLowerCase(Locale.ROOT);
						functions.add(new TestFunction(DEFAULT_BATCH, name, FabricGameTest.EMPTY_STRUCTURE, DEFAULT_TIMEOUT_TICKS, 0, true,
								helper -> invoke(tests, method, helper, kind)));
					}
				});
		return functions;
	}

	private static void invoke(Class<?> tests, Method method, GameTestHelper helper, ChestKind kind) {
		try {
			method.invoke(tests.getDeclaredConstructor().newInstance(), helper, kind);
		} catch (InvocationTargetException e) {
			throw e.getCause() instanceof RuntimeException failure ? failure : new RuntimeException(e.getCause());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	static EchoChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos) {
		return placeChest(helper, pos, ChestKind.ECHO);
	}

	static EchoChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos, ChestKind kind) {
		helper.setBlock(pos, kind.block);
		return chestAt(helper, pos);
	}

	static EchoChestBlockEntity chestAt(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos);
	}

	/** Places {@code stack} at {@code pos} the way a player would, through the block item. */
	static void placeFromItem(GameTestHelper helper, ItemStack stack, BlockPos pos) {
		// Read before placing, which uses the stack up.
		Block block = ((BlockItem) stack.getItem()).getBlock();
		Player player = holding(helper, stack);
		helper.placeAt(player, player.getMainHandItem(), pos.below(), Direction.UP);
		helper.assertBlockPresent(block, pos);
	}

	/** Placement reads the stack from the player's hand, not from the stack it is handed. */
	static Player holding(GameTestHelper helper, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	/** Breaks the block as a player's tool would: loot table drops and all. */
	static void breakChest(GameTestHelper helper, BlockPos pos) {
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
	}

	static ItemStack droppedChest(GameTestHelper helper) {
		return droppedChest(helper, ChestKind.ECHO);
	}

	static ItemStack droppedChest(GameTestHelper helper, ChestKind kind) {
		List<ItemStack> chests = helper.getEntities(EntityType.ITEM).stream()
				.map(ItemEntity::getItem)
				.filter(stack -> stack.is(kind.item))
				.toList();
		helper.assertValueEqual(chests.size(), 1, "dropped chests of kind " + kind);
		return chests.getFirst();
	}

	/** Opens {@code chest}'s menu for a player standing above {@link #CHEST}, wherever the chest is. */
	static ServerPlayer openedBy(GameTestHelper helper, EchoChestBlockEntity chest) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(CHEST.getCenter()).add(0, 1, 0));
		player.openMenu(new EchoChestMenuProvider(chest));
		helper.assertTrue(player.containerMenu instanceof EchoChestMenu, "the Echo Chest menu did not open");
		return player;
	}

	static EchoChestMenu menu(ServerPlayer player) {
		return (EchoChestMenu) player.containerMenu;
	}

	/** An item entity at the player's feet, touched by them as a tick would. */
	static ItemEntity drop(GameTestHelper helper, Player player, ItemStack stack) {
		ItemEntity entity = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), stack);
		helper.getLevel().addFreshEntity(entity);
		entity.playerTouch(player);
		return entity;
	}

	static ItemStack bundleOf(ItemStack... contents) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		for (ItemStack stack : contents) {
			mutable.tryInsert(stack.copy());
		}
		ItemStack bundle = new ItemStack(EchoItems.ECHO_BUNDLE);
		EchoBundleItem.setContents(bundle, mutable.toImmutable());
		return bundle;
	}

	/** A shulker box item of the given kind, its slots filled in order from the first. */
	static ItemStack boxOf(Item kind, ItemStack... contents) {
		ItemStack box = new ItemStack(kind);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
		return box;
	}

	/**
	 * Runs {@code action} with {@code config} in force, then puts the config back. A test runs on
	 * the server thread start to finish, so no other test sees the change.
	 */
	static void withConfig(EchoConfig config, Runnable action) {
		withConfig(config, () -> {
			action.run();
			return null;
		});
	}

	/** {@link #withConfig(EchoConfig, Runnable)} for an action with a result. */
	static <T> T withConfig(EchoConfig config, Supplier<T> action) {
		EchoConfig before = EchoConfig.get();
		EchoConfig.set(config);
		try {
			return action.get();
		} finally {
			EchoConfig.set(before);
		}
	}

	static void assertStack(GameTestHelper helper, ItemStack expected, ItemStack actual, String what) {
		helper.assertTrue(ItemStack.matches(expected, actual), what + ": expected " + expected + ", got " + actual);
	}
}
