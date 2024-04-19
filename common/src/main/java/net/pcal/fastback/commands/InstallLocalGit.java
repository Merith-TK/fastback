package net.pcal.fastback.commands;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;

enum InstallLocalGit implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "local-git";
    private static final String ARGUMENT = "enabled";

    @Override
    public void register(final LiteralArgumentBuilder<CommandSourceStack> argb, PermissionsFactory<CommandSourceStack> pf) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(COMMAND_NAME, pf)).then(
                                argument(ARGUMENT, StringArgumentType.string()).
                                        executes(RemoteRestoreCommand::remoteRestore)
                        )
        );
    }

    private static int installLocalGit(final CommandContext<CommandSourceStack> cc) {
        final UserLogger ulog = ulog(cc);
        // detect local 
        return SUCCESS;
    }
    