package dev.hefker.echostorage.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Echo Shulker Box as an item. Like a vanilla shulker box it goes into no other container, and
 * {@link BlockItem} already spills its contents when the item entity is destroyed.
 */
public class EchoShulkerBoxItem extends BlockItem {
	public EchoShulkerBoxItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** Keeps it out of Echo Bundles, vanilla bundles and shulker boxes of either kind. */
	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	/**
	 * Washes a dyed box back to undyed in a water cauldron, keeping everything else, as vanilla
	 * washes its shulker boxes. An undyed box does nothing, as vanilla's undyed one does.
	 */
	static ItemInteractionResult wash(BlockState cauldron, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack box) {
		if (!box.has(DataComponents.BASE_COLOR)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide) {
			ItemStack washed = box.copyWithCount(1);
			washed.remove(DataComponents.BASE_COLOR);
			player.setItemInHand(hand, ItemUtils.createFilledResult(box, player, washed, false));
			player.awardStat(Stats.CLEAN_SHULKER_BOX);
			LayeredCauldronBlock.lowerFillLevel(cauldron, level, pos);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}
}
