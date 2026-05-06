package com.ultramega.asteroidmining.storage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record NetworkConfiguration(LaunchPadConfiguration launchPadConfiguration,
                                   Optional<RocketProperties> rocketProperties,
                                   ModuleProperties moduleProperties) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkConfiguration> STREAM_CODEC = StreamCodec.composite(
        LaunchPadConfiguration.STREAM_CODEC, NetworkConfiguration::launchPadConfiguration,
        RocketProperties.STREAM_CODEC.apply(ByteBufCodecs::optional), NetworkConfiguration::rocketProperties,
        ModuleProperties.STREAM_CODEC, NetworkConfiguration::moduleProperties,
        NetworkConfiguration::new
    );

    public static final Codec<NetworkConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        LaunchPadConfiguration.CODEC.fieldOf("rocketInfo").forGetter(NetworkConfiguration::launchPadConfiguration),
        RocketProperties.CODEC.optionalFieldOf("rocketProperties").forGetter(NetworkConfiguration::rocketProperties),
        ModuleProperties.CODEC.fieldOf("moduleProperties").forGetter(NetworkConfiguration::moduleProperties)
        ).apply(instance, NetworkConfiguration::new));

    public static final Codec<Map<UUID, NetworkConfiguration>> MAP_CODEC = Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), NetworkConfiguration.CODEC);

    public NetworkConfiguration withModuleProperties(final ModuleProperties moduleProperties) {
        return new NetworkConfiguration(this.launchPadConfiguration(), this.rocketProperties(), moduleProperties);
    }
}
