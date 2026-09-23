package dev.hefker.echostorage.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.hefker.echostorage.menu.ProbeMenuData;
import dev.hefker.echostorage.menu.ProbeMenuProvider;
import dev.hefker.echostorage.network.SeamProbe;
import dev.hefker.echostorage.platform.Services;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;

/**
 * {@code /echostorage seam …} — the manual trigger for the two seam probes, so the networking
 * and menu seams can be exercised in game before there is any content to exercise them with.
 *
 * <p>Operator-only and dev-facing; it goes away once real blocks open real menus.
 */
public final class SeamCommand {
	private static final int OPERATOR = 2;

	private SeamCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> dispatcher.register(root()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> root() {
		return Commands.literal("echostorage")
				.requires(source -> source.hasPermission(OPERATOR))
				.then(Commands.literal("seam")
						.then(Commands.literal("ping").executes(context -> ping(context.getSource())))
						.then(Commands.literal("menu").executes(context -> menu(context.getSource()))));
	}

	private static int ping(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		SeamProbe.ping(player);
		source.sendSuccess(() -> Component.literal("seam ping sent; waiting for the client to answer"), false);
		return 1;
	}

	private static int menu(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ProbeMenuData data = new ProbeMenuData("Seam Probe (" + Services.PLATFORM.platformName() + ")", 3);
		player.openMenu(new ProbeMenuProvider(new SimpleContainer(data.slotCount()), data));
		return 1;
	}
}
