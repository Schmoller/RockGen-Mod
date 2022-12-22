package schmoller.mods.rockgen;

import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipe;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipeCache;

@Mod("rockgen")
public class RockGenerationMod {
    public static final FluidSpreadRecipeCache FluidSpreadRecipeCache = new FluidSpreadRecipeCache();

    private static final DeferredRegister<RecipeType<?>> RecipeTypes = DeferredRegister.create(
        Registry.RECIPE_TYPE_REGISTRY,
        "rockgen"
    );
    private static final RegistryObject<RecipeType<FluidSpreadRecipe>> FluidSpreadRecipeType = RecipeTypes.register(
        FluidSpreadRecipe.TypeId,
        () -> FluidSpreadRecipe.Type
    );

    private static final DeferredRegister<RecipeSerializer<?>> RecipeSerializers = DeferredRegister.create(
        Registry.RECIPE_SERIALIZER_REGISTRY,
        "rockgen"
    );
    private static final RegistryObject<RecipeSerializer<FluidSpreadRecipe>> FluidSpreadRecipeSerializer = RecipeSerializers.register(
        FluidSpreadRecipe.TypeId,
        () -> FluidSpreadRecipe.SerializerInstance
    );

    public RockGenerationMod() {
        MinecraftForge.EVENT_BUS.register(this);
        RecipeTypes.register(FMLJavaModLoadingContext.get().getModEventBus());
        RecipeSerializers.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    // NOTE: This is client side only. Does not fire on servers
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRecipesUpdated(RecipesUpdatedEvent event) {
        var spreadRecipes = event.getRecipeManager().getAllRecipesFor(FluidSpreadRecipe.Type);
        FluidSpreadRecipeCache.prepare(spreadRecipes);
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }

        FluidSpreadRecipeCache.invalidate();
    }
}
