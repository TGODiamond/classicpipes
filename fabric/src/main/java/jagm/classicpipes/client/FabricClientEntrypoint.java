package jagm.classicpipes.client;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.client.renderer.FluidPipeRenderer;
import jagm.classicpipes.client.renderer.PipeRenderer;
import jagm.classicpipes.client.renderer.RecipePipeRenderer;
import jagm.classicpipes.client.screen.*;
import jagm.classicpipes.network.ClientBoundItemListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@SuppressWarnings("unused")
public class FabricClientEntrypoint implements ClientModInitializer {

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

}
