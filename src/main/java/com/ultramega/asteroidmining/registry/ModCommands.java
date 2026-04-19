//package com.ultramega.asteroidmining.registry;
//
//import com.ultramega.asteroidmining.AsteroidMining;
//import com.ultramega.asteroidmining.commands.EditCommand;
//
//import com.mojang.brigadier.CommandDispatcher;
//import com.mojang.brigadier.builder.LiteralArgumentBuilder;
//import net.minecraft.commands.CommandSourceStack;
//
//public final class ModCommands {
//    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
//        final LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal(AsteroidMining.MOD_ID);
//
//        new EditCommand().register(builder);
//
//        dispatcher.register(builder);
//    }
//}
