package jagm.classicpipes;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ForgeFluidPipeWrapper;
import jagm.classicpipes.blockentity.ForgeItemPipeWrapper;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.network.ForgeClientPacketHandler;
import jagm.classicpipes.client.renderer.FluidPipeRenderer;
import jagm.classicpipes.client.renderer.PipeRenderer;
import jagm.classicpipes.client.renderer.RecipePipeRenderer;
import jagm.classicpipes.client.screen.*;
import jagm.classicpipes.network.*;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

@Mod(ClassicPipes.MOD_ID)
@SuppressWarnings("unused")
public final class ForgeEntrypoint {

    @Mod.EventBusSubscriber(modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventHandler {

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {

            event.register(ForgeRegistries.Keys.BLOCKS, helper -> ClassicPipes.BLOCKS.forEach(helper::register));
            event.register(ForgeRegistries.Keys.ITEMS, helper -> ClassicPipes.ITEMS.forEach(helper::register));
            event.register(ForgeRegistries.Keys.SOUND_EVENTS, helper -> ClassicPipes.SOUNDS.forEach(helper::register));
            event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ClassicPipes.PIPES_TAB_KEY, ClassicPipes.PIPES_TAB));
            event.register(Registries.DATA_COMPONENT_TYPE, helper -> helper.register(ClassicPipes.LABEL_COMPONENT_KEY, ClassicPipes.LABEL_COMPONENT));
            event.register(Registries.TRIGGER_TYPE, helper -> helper.register(MiscUtil.identifier("request_item"), ClassicPipes.REQUEST_ITEM_TRIGGER));
            event.register(Registries.CUSTOM_STAT, helper -> helper.register(ClassicPipes.ITEMS_REQUESTED_STAT, ClassicPipes.ITEMS_REQUESTED_STAT));

            event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper ->
                ClassicPipes.BlOCK_ENTITIES.forEach(helper::register)
            );

            event.register(ForgeRegistries.Keys.MENU_TYPES, helper ->
                ClassicPipes.MENUS.forEach(helper::register)
            );

        }

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                ForgeServerPacketHandler.registerServerPayload(ServerBoundMatchComponentsPayload.class, ServerBoundMatchComponentsPayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundDefaultRoutePayload.class, ServerBoundDefaultRoutePayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundLeaveOnePayload.class, ServerBoundLeaveOnePayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundSortingModePayload.class, ServerBoundSortingModePayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundRequestPayload.class, ServerBoundRequestPayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundActiveStockingPayload.class, ServerBoundActiveStockingPayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundSlotDirectionPayload.class, ServerBoundSlotDirectionPayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundTransferRecipePayload.class, ServerBoundTransferRecipePayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundSetFilterPayload.class, ServerBoundSetFilterPayload.STREAM_CODEC);
                ForgeServerPacketHandler.registerServerPayload(ServerBoundBlockingModePayload.class, ServerBoundBlockingModePayload.STREAM_CODEC);
                ClassicPipes.createStats();
            });
        }

    }

    @Mod.EventBusSubscriber(modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEventHandler {

        @SubscribeEvent
        public static void onRegisterCapabilities(AttachCapabilitiesEvent event) {
            if (event.getObject() instanceof ItemPipeEntity pipe) {
                Map<Direction, LazyOptional<IItemHandler>> wrapperForSide = new HashMap<>();
                for (Direction side : Direction.values()) {
                    wrapperForSide.put(side, LazyOptional.of(() -> new ForgeItemPipeWrapper(pipe, side)));
                }
                event.addCapability(MiscUtil.identifier("item_pipe"), new ICapabilityProvider() {
                    @Override
                    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
                            return wrapperForSide.get(side).cast();
                        }
                        return LazyOptional.empty();
                    }
                });
                event.addListener(() -> wrapperForSide.forEach((direction, lazyOptional) -> lazyOptional.invalidate()));
            } else if (event.getObject() instanceof FluidPipeEntity pipe) {
                Map<Direction, LazyOptional<IFluidHandler>> wrapperForSide = new HashMap<>();
                for (Direction side : Direction.values()) {
                    wrapperForSide.put(side, LazyOptional.of(() -> new ForgeFluidPipeWrapper(pipe, side)));
                }
                event.addCapability(MiscUtil.identifier("fluid_pipe"), new ICapabilityProvider() {
                    @Override
                    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
                            return wrapperForSide.get(side).cast();
                        }
                        return LazyOptional.empty();
                    }
                });
                event.addListener(() -> wrapperForSide.forEach((direction, lazyOptional) -> lazyOptional.invalidate()));
            }
        }

    }

    @Mod.EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ServerModEventHandler {

        @SubscribeEvent
        public static void onServerSetup(FMLDedicatedServerSetupEvent event) {
            event.enqueueWork(() -> ForgeServerPacketHandler.registerClientPayload(ClientBoundItemListPayload.class, ClientBoundItemListPayload.STREAM_CODEC));
        }

    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEventHandler {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ForgeClientPacketHandler.registerClientPayload(ClientBoundItemListPayload.class, ClientBoundItemListPayload.STREAM_CODEC);
                MenuScreens.register(ClassicPipes.DIAMOND_PIPE_MENU, DiamondPipeScreen::new);
                MenuScreens.register(ClassicPipes.ROUTING_PIPE_MENU, RoutingPipeScreen::new);
                MenuScreens.register(ClassicPipes.PROVIDER_PIPE_MENU, ProviderPipeScreen::new);
                MenuScreens.register(ClassicPipes.REQUEST_MENU, RequestScreen::new);
                MenuScreens.register(ClassicPipes.STOCKING_PIPE_MENU, StockingPipeScreen::new);
                MenuScreens.register(ClassicPipes.MATCHING_PIPE_MENU, MatchingPipeScreen::new);
                MenuScreens.register(ClassicPipes.STORAGE_PIPE_MENU, StoragePipeScreen::new);
                MenuScreens.register(ClassicPipes.RECIPE_PIPE_MENU, RecipePipeScreen::new);
                MenuScreens.register(ClassicPipes.DIAMOND_FLUID_PIPE_MENU, DiamondFluidPipeScreen::new);
                MenuScreens.register(ClassicPipes.ADVANCED_COPPER_PIPE_MENU, AdvancedCopperPipeScreen::new);
                MenuScreens.register(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU, AdvancedCopperFluidPipeScreen::new);
            });
        }

    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientForgeEventHandler {

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            ClassicPipes.ITEM_PIPE_ENTITIES.forEach((name, entityType) ->
                    event.registerBlockEntityRenderer(entityType, PipeRenderer::new)
            );
            ClassicPipes.FLUID_PIPE_ENTITIES.forEach((name, entityType) ->
                    event.registerBlockEntityRenderer(entityType, FluidPipeRenderer::new)
            );
            // Uses a special renderer
            event.registerBlockEntityRenderer(ClassicPipes.RECIPE_PIPE_ENTITY, RecipePipeRenderer::new);
        }

        @SubscribeEvent
        public static void onFillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == ClassicPipes.PIPES_TAB_KEY) {
                ClassicPipes.ITEMS.forEach((name, item) -> event.accept(item));
            }
        }

    }

}
