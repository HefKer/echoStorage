package dev.hefker.echostorage.block;

import java.util.List;

import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * What every kind of Echo Chest's block does alike, whatever its shape: placing it gives it a new
 * id, it opens the chest's menu, it rechecks who has it open, and it reads out to a comparator.
 *
 * <p>The kinds share this rather than one extending another because their block states differ: an
 * {@link EchoChestBlock} faces one of four ways, an {@link EchoShulkerBoxBlock} any of six.
 */
public abstract class AbstractEchoChestBlock extends BaseEntityBlock {
	protected AbstractEchoChestBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	/** The property the block turns and mirrors by. */
	protected abstract DirectionProperty facing();

	/**
	 * Whether this kind keeps out every shulker box, from every source, the player's hand included,
	 * as vanilla's shulker box does (ADR-0009). Its block entity and its menu both ask this.
	 */
	public boolean refusesShulkerBoxes() {
		return false;
	}

	/** Placed by a player: a new id, so data copied from another chest cannot clone its id. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(pos) instanceof EchoChestBlockEntity chest) {
			chest.assignNewId();
		}
	}

	/**
	 * The chest's menu, opened in person. Asks nothing of the lid: a kind whose lid can be blocked
	 * checks that where the player's hand opens it.
	 */
	@Nullable
	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EchoChestBlockEntity chest ? new EchoChestMenuProvider(chest) : null;
	}

	/** The Category and Strict lines of any kind of Echo Chest's item. */
	static void appendAssignment(ItemStack stack, List<Component> lines) {
		EchoChestAssignment assignment = stack.getOrDefault(EchoComponents.ECHO_CHEST_ASSIGNMENT, EchoChestAssignment.DEFAULT);
		assignment.category().ifPresent(category -> lines.add(
				Component.translatable("item.echostorage.echo_chest.category", category.displayName()).withStyle(ChatFormatting.GRAY)));
		if (assignment.strict()) {
			lines.add(Component.translatable("item.echostorage.echo_chest.strict").withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof EchoChestBlockEntity chest) {
			chest.recheckOpen();
		}
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(facing(), rotation.rotate(state.getValue(facing())));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(facing())));
	}
}
