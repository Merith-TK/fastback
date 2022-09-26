/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.lib.StoredConfig;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.WRITE_CONFIG;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;

public class SetShutdownActionCommand {

    private static final String COMMAND_NAME = "set-shutdown-action";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final LiteralArgumentBuilder<ServerCommandSource> setCommand = literal(COMMAND_NAME).
                requires(subcommandPermission(ctx, COMMAND_NAME));
        for (final SchedulableAction action : SchedulableAction.values()) {
            final LiteralArgumentBuilder<ServerCommandSource> azz = literal(action.getConfigKey());
            azz.executes(cc -> execute(ctx, cc.getSource(), action));
            setCommand.then(azz);
        }
        argb.then(setCommand);
    }

    public static int execute(final ModContext ctx, final ServerCommandSource scs, SchedulableAction action) {
        final Logger log = commandLogger(ctx, scs);
        gitOp(ctx, WRITE_CONFIG, log, git -> {
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setShutdownAction(config, action);
            config.save();
            ctx.getLogger().info("Set shutdown action to " + action);
        });
        return SUCCESS;
    }
}