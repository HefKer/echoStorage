package dev.hefker.echostorage.block;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hefker.echostorage.menu.EchoChestMenuData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Echo Chest block. Shaped and animated like a vanilla chest, but it never pairs
 * (ADR-0006): there is no chest-type property, so there is no half-a-chest to become.
 *
 * <p>Breaking it spills the contents like any chest and drops the chest itself, name,
 * Category and strictness and all, through its loot table — reorganising a storage wall
 * should never cost materials.
 *
 * <p>The Echo Chest and the Deep Echo Chest are this one class with a different number of rows;
 * everything else about them is the same. The Echo Shulker Box is a block of its own, since it is
 * shaped, opened and broken like a shulker box: see {@link EchoShulkerBoxBlock}.
 */
public class EchoChestBlock extends AbstractEchoChestBlock {
	public static final MapCodec<EchoChestBlock> CODEC = RecordCodecBuilder.mapCodec(block -> block.group(
			Codec.intRange(1, EchoChestMenuData.MAX_ROWS).fieldOf("rows").forGetter(EchoChestBlock::rows),
			propertiesCodec()
	).apply(block, EchoChestBlock::new));
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final int SLOTS_PER_ROW = 9;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

	private final int rows;

	/** @param rows how many rows of nine slots the chest has: never more than a chest screen can lay out */
	public EchoChestBlock(int rows, BlockBehaviour.Properties properties) {
		super(properties);
		if (rows < 1 || rows > EchoChestMenuData.MAX_ROWS) {
			throw new IllegalArgumentException("an Echo Chest has 1 to " + EchoChestMenuData.MAX_ROWS + " rows, not " + rows);
		}
		this.rows = rows;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public int rows() {
		return rows;
	}

	@Override
	protected MapCodec<EchoChestBlock> codec() {
		return CODEC;
	}

	@Override
	protected DirectionProperty facing() {
		return FACING;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		MenuProvider menu = getMenuProvider(state, level, pos);
		if (menu != null) {
			player.openMenu(menu);
			player.awardStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));
		}
		return InteractionResult.CONSUME;
	}

	/** Why an Echo Chest item does not stack with a blank one: what it will be set to when placed. */
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
		super.appendHoverText(stack, context, lines, flag);
		appendAssignment(stack, lines);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		Containers.dropContentsOnDestroy(state, newState, level, pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EchoChestBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide
				? createTickerHelper(type, EchoBlocks.ECHO_CHEST_ENTITY, EchoChestBlockEntity::lidAnimateTick)
				: null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
