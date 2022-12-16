package schmoller.mods.rockgen;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import schmoller.mods.rockgen.recipes.FluidFallRecipe;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipe;

import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("rockgen")
public class ExampleMod
{
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(RecipeSerializer.class, this::onRegisterRecipes);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent)
        {
            // Register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }

    public void onRegisterRecipes(RegistryEvent.Register<RecipeSerializer<?>> event) {
        Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(FluidSpreadRecipe.TypeId), FluidSpreadRecipe.Type);
        event.getRegistry().register(FluidSpreadRecipe.SerializerInstance);

        Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(FluidFallRecipe.TypeId), FluidFallRecipe.Type);
        event.getRegistry().register(FluidFallRecipe.SerializerInstance);
    }
}
