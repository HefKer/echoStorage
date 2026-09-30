package dev.hefker.echostorage.block;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Echo Shulker Box block: an Echo Chest shaped, opened and broken like a vanilla shulker box.
 * It faces the side it was placed against, its lid will not open by hand against a block, and
 * breaking it, by hand or by a piston, drops one item holding everything inside rather than
 * spilling it. Broken in creative, it drops only if it holds something, as vanilla's does.
 *
 * <p>Opened from an Echo Interface it opens whatever is on the lid, since nothing lifts.
 */
public class EchoShulkerBoxBlock extends BaseEntityBlock {
	public static final MapCodec<EchoShulkerBoxBlock> CODEC = simpleCodec(EchoShulkerBoxBlock::new);
	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	/** How many stacks the tooltip names before it says how many more there are, as vanilla's does. */
	private static final int TOOLTIP_STACKS = 5;
	/** What an open box still supports on the side opposite its lid: a sliver of its base. */
	private static final Map<Direction, VoxelShape> OPEN_SUPPORT_SHAPES = Map.of(
			Direction.NORTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0),
			Direction.EAST, Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0),
			Direction.SOUTH, Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0),
			Direction.WEST, Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0),
			Direction.UP, Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0),
			Direction.DOWN, Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0));

	public EchoShulkerBoxBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
	}

	/** For the block's properties: suffocating and blocking sight only while the lid is shut. */
	static boolean isClosed(BlockState state, BlockGetter level, BlockPos pos) {
		return !(level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box) || box.isClosed();
	}

	@Override
	protected MapCodec<EchoShulkerBoxBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box) {
			box.assignNewId();
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSpectator()) {
			return InteractionResult.CONSUME;
		}
		if (level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box) {
			if (canOpen(state, level, pos, box)) {
				player.openMenu(new EchoChestMenuProvider(box));
				player.awardStat(Stats.OPEN_SHULKER_BOX);
				PiglinAi.angerNearbyPiglins(player, true);
			}
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	/** Whether the lid has room to rise: a shut box with a block against its lid stays shut. */
	private static boolean canOpen(BlockState state, Level level, BlockPos pos, EchoShulkerBoxBlockEntity box) {
		if (!box.isClosed()) {
			return true;
		}
		AABB lid = Shulker.getProgressDeltaAabb(1.0F, state.getValue(FACING), 0.0F, 0.5F).move(pos).deflate(1.0E-6);
		return level.noCollision(lid);
	}

	@Nullable
	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box ? new EchoChestMenuProvider(box) : null;
	}

	/** A creative player's break drops nothing, so a box with anything in it drops itself here. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && player.isCreative()
				&& level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box && !box.isEmpty()) {
			ItemStack stack = new ItemStack(this);
			stack.applyComponents(box.collectComponents());
			ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Spills nothing: the contents leave on the dropped item, through the loot table. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			BlockEntity box = level.getBlockEntity(pos);
			super.onRemove(state, level, pos, newState, movedByPiston);
			if (box instanceof EchoShulkerBoxBlockEntity) {
				level.updateNeighbourForOutputSignal(pos, state.getBlock());
			}
		}
	}

	/** Vanilla's contents preview, then the Echo Chest's Category and Strict lines. */
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
		super.appendHoverText(stack, context, lines, flag);
		int shown = 0;
		int stacks = 0;
		for (ItemStack inside : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
			stacks++;
			if (shown < TOOLTIP_STACKS) {
				shown++;
				lines.add(Component.translatable("container.shulkerBox.itemCount", inside.getHoverName(), inside.getCount()));
			}
		}
		if (stacks > shown) {
			lines.add(Component.translatable("container.shulkerBox.more", stacks - shown).withStyle(ChatFormatting.ITALIC));
		}
		EchoChestBlock.appendAssignment(stack, lines);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box) {
			box.recheckOpen();
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EchoShulkerBoxBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, EchoBlocks.ECHO_SHULKER_BOX_ENTITY, EchoShulkerBoxBlockEntity::tick);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box
				? Shapes.create(box.boundingBox(state))
				: Shapes.block();
	}

	@Override
	protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EchoShulkerBoxBlockEntity box && !box.isClosed()
				? OPEN_SUPPORT_SHAPES.get(state.getValue(FACING).getOpposite())
				: Shapes.block();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
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
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
