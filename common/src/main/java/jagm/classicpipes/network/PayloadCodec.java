package jagm.classicpipes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface PayloadCodec<T> {
    StreamCodec<RegistryFriendlyByteBuf, T>  getPayloadCodec();
}
