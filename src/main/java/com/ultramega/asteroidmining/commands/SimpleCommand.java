//package com.ultramega.asteroidmining.commands;
//
//import com.mojang.brigadier.builder.LiteralArgumentBuilder;
//import net.minecraft.commands.CommandSourceStack;
//import net.minecraft.commands.Commands;
//
//public abstract class SimpleCommand {
//    public void register(final LiteralArgumentBuilder<CommandSourceStack> builder) {
//        final var subCommandBuilder = Commands.literal(getName())
//            .requires(requirement -> requirement.hasPermission(4))
//            .executes(command -> execute(command.getSource()));
//
//        builder.then(subCommandBuilder);
//    }
//
//    protected abstract String getName();
//
//    protected abstract int execute(CommandSourceStack source);
//}
