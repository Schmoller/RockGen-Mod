package schmoller.mods.rockgen;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipe;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipeCache;

@Mod(RockGenerationMod.Id)
public class RockGenerationMod {
    public static final FluidSpreadRecipeCache FluidSpreadRecipeCache = new FluidSpreadRecipeCache();
    public static final String Id = "rockgen";

    public RockGenerationMod() {
        FMLJavaModLoadingContext.get()
            .getModEventBus()
            .addGenericListener(RecipeSerializer.class, this::onRegisterRecipes);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onRegisterRecipes(RegistryEvent.Register<RecipeSerializer<?>> event) {
        Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(FluidSpreadRecipe.TypeId), FluidSpreadRecipe.Type);
        event.getRegistry().register(FluidSpreadRecipe.SerializerInstance);
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
