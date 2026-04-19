//package com.ultramega.asteroidmining.commands;
//
//import com.ultramega.asteroidmining.AsteroidMining;
//import com.ultramega.asteroidmining.network.s2c.OpenAsteroidEditScreenMessage;
//
//import com.mojang.brigadier.exceptions.CommandSyntaxException;
//import net.minecraft.commands.CommandSourceStack;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.level.ServerPlayer;
//import net.neoforged.neoforge.network.PacketDistributor;
//
//public class EditCommand extends SimpleCommand {
//    @Override
//    protected int execute(final CommandSourceStack source) {
//        try {
//            final ServerPlayer player = source.getPlayerOrException();
//            PacketDistributor.sendToPlayer(player, new OpenAsteroidEditScreenMessage());
//        } catch (CommandSyntaxException e) {
//            AsteroidMining.LOGGER.error(String.valueOf(e));
//            source.sendFailure(Component.literal(e.toString()));
//        }
//
//        return 0;
//    }
//
//    @Override
//    protected String getName() {
//        return "edit";
//    }
//}
