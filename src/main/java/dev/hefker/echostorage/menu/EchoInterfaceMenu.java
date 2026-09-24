package dev.hefker.echostorage.menu;

import java.util.List;

import dev.hefker.echostorage.block.EchoInterfaceBlockEntity;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.network.EchoInterfaceRowsPayload;
import dev.hefker.echostorage.network.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An open Echo Interface: a list of rows and nothing else. It has no slots, because it holds
 * nothing — it names chests and opens one (ADR-0002).
 *
 * <p>The server re-resolves while the screen is open and sends the rows again whenever they
 * change. Opening and dismissing a row name the chest by id, never by its place in the list, so
 * a list that changed under the player's click can never open the wrong chest.
 */
public class EchoInterfaceMenu extends AbstractContainerMenu {
	/** An action: quick-stacks into every linked chest once on the server. */
	public static final int QUICK_STACK_BUTTON = 0;

	/** Null on the client. */
	@Nullable
	private final EchoInterfaceBlockEntity echoInterface;
	private final Player player;
	/** On the server the rows the client was last sent; on the client the rows it was last sent. */
	private List<Row> rows;

	/** Client-side constructor. */
	public EchoInterfaceMenu(int containerId, Inventory playerInventory, EchoInterfaceMenuData data) {
		this(containerId, playerInventory, null, data.rows());
	}

	public EchoInterfaceMenu(int containerId, Inventory playerInventory, EchoInterfaceBlockEntity echoInterface) {
		this(containerId, playerInventory, echoInterface, echoInterface.rows());
	}

	private EchoInterfaceMenu(int containerId, Inventory playerInventory, @Nullable EchoInterfaceBlockEntity echoInterface,
			List<Row> rows) {
		super(EchoMenus.ECHO_INTERFACE, containerId);
		this.echoInterface = echoInterface;
		this.player = playerInventory.player;
		this.rows = rows;
	}

	/** The rows as this side last knew them. */
	public List<Row> rows() {
		return rows;
	}

	/** Client-only: the server's newer rows. */
	public void setRows(List<Row> rows) {
		this.rows = List.copyOf(rows);
	}

	/** The interface behind this menu; empty on the client. */
	@Nullable
	public EchoInterfaceBlockEntity echoInterface() {
		return echoInterface;
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (button == QUICK_STACK_BUTTON) {
			if (echoInterface != null) {
				echoInterface.quickStack(player);
			}
			return true;
		}
		return false;
	}

	/** Runs every tick the screen is open on the server: keeps the rows fresh and the link visual going. */
	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (echoInterface != null && player instanceof ServerPlayer viewer) {
			echoInterface.refresh();
			echoInterface.traceFor(viewer);
			List<Row> now = echoInterface.rows();
			if (!now.equals(rows)) {
				rows = now;
				Net.sendTo(viewer, new EchoInterfaceRowsPayload(containerId, now));
			}
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return echoInterface == null || echoInterface.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
