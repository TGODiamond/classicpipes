package jagm.classicpipes;

import jagm.classicpipes.blockentity.FabricFluidPipeWrapper;
import jagm.classicpipes.blockentity.FabricItemPipeWrapper;
import jagm.classicpipes.client.renderer.FluidPipeRenderer;
import jagm.classicpipes.client.renderer.PipeRenderer;
import jagm.classicpipes.client.renderer.RecipePipeRenderer;
import jagm.classicpipes.client.screen.*;
import jagm.classicpipes.network.*;
import jagm.classicpipes.util.MiscUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@SuppressWarnings("unused")
public final class FabricEntrypoint implements ModInitializer, ClientModInitializer {

    public static final long FLUID_CONVERSION_RATE = FluidConstants.BUCKET / 1000;

    @Override
    public void onInitialize() {

        ClassicPipes.ITEMS.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, MiscUtil.identifier(name), item));
        ClassicPipes.BLOCKS.forEach((name, block) -> Registry.register(BuiltInRegistries.BLOCK, MiscUtil.identifier(name), block));
        ClassicPipes.SOUNDS.forEach((name, soundEvent) -> Registry.register(BuiltInRegistries.SOUND_EVENT, MiscUtil.identifier(name), soundEvent));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ClassicPipes.PIPES_TAB_KEY, ClassicPipes.PIPES_TAB);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ClassicPipes.LABEL_COMPONENT_KEY, ClassicPipes.LABEL_COMPONENT);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, MiscUtil.identifier("request_item"), ClassicPipes.REQUEST_ITEM_TRIGGER);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, ClassicPipes.ITEMS_REQUESTED_STAT, ClassicPipes.ITEMS_REQUESTED_STAT);
        ClassicPipes.createStats();

        ClassicPipes.BlOCK_ENTITIES.forEach(FabricEntrypoint::registerBlockEntity);

        ClassicPipes.ITEM_PIPE_ENTITIES.forEach((name, entityType) ->
                ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, entityType)
        );
        ClassicPipes.FLUID_PIPE_ENTITIES.forEach((name, entityType) ->
                FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, entityType)
        );
        // Pipe(s) with a unique renderer
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.RECIPE_PIPE_ENTITY);

        ClassicPipes.MENUS.forEach(FabricEntrypoint::registerMenu);

        ItemGroupEvents.modifyEntriesEvent(ClassicPipes.PIPES_TAB_KEY).register(tab -> ClassicPipes.ITEMS.forEach((name, item) -> tab.accept(item)));

        registerServerPayload(ServerBoundMatchComponentsPayload.TYPE, ServerBoundMatchComponentsPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundDefaultRoutePayload.TYPE, ServerBoundDefaultRoutePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundLeaveOnePayload.TYPE, ServerBoundLeaveOnePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundSortingModePayload.TYPE, ServerBoundSortingModePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundRequestPayload.TYPE, ServerBoundRequestPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundActiveStockingPayload.TYPE, ServerBoundActiveStockingPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundSlotDirectionPayload.TYPE, ServerBoundSlotDirectionPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundTransferRecipePayload.TYPE, ServerBoundTransferRecipePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundSetFilterPayload.TYPE, ServerBoundSetFilterPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundBlockingModePayload.TYPE, ServerBoundBlockingModePayload.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(ClientBoundItemListPayload.TYPE, ClientBoundItemListPayload.STREAM_CODEC);

    }

    @Override
    public void onInitializeClient() {

        ClassicPipes.TRANSPARENT_BLOCKS.forEach(block -> BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.CUTOUT));
        ClassicPipes.ITEM_PIPE_ENTITIES.forEach((name, entityType) ->
                BlockEntityRenderers.register(entityType, PipeRenderer::new)
        );
        ClassicPipes.FLUID_PIPE_ENTITIES.forEach((name, entityType) ->
                BlockEntityRenderers.register(entityType, FluidPipeRenderer::new)
        );
        // Uses a special renderer
        BlockEntityRenderers.register(ClassicPipes.RECIPE_PIPE_ENTITY, RecipePipeRenderer::new);

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

        ClientPlayNetworking.registerGlobalReceiver(ClientBoundItemListPayload.TYPE, (payload, context) -> payload.handle(context.player()));

    }

    private static <T extends BlockEntity> void registerBlockEntity(String name, BlockEntityType<T> blockEntityType) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MiscUtil.identifier(name), blockEntityType);
    }

    private static <T extends AbstractContainerMenu> void registerMenu(String name, MenuType<T> menuType) {
        Registry.register(BuiltInRegistries.MENU, MiscUtil.identifier(name), menuType);
    }

    private static <T extends SelfHandler> void registerServerPayload(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> payload.handle(context.player()));
    }

}
