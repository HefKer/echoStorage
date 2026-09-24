package dev.hefker.echostorage.block;

import com.mojang.serialization.MapCodec;
import dev.hefker.echostorage.menu.EchoInterfaceMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Echo Interface block. Using it resolves its Links afresh and opens the list, so what the
 * player sees is never older than the click that showed it.
 */
public class EchoInterfaceBlock extends BaseEntityBlock {
	public static final MapCodec<EchoInterfaceBlock> CODEC = simpleCodec(EchoInterfaceBlock::new);

	public EchoInterfaceBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<EchoInterfaceBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof EchoInterfaceBlockEntity echoInterface) {
			echoInterface.resolve();
			player.openMenu(new EchoInterfaceMenuProvider(echoInterface));
		}
		return InteractionResult.CONSUME;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EchoInterfaceBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}
