package schmoller.mods.rockgen.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import schmoller.mods.rockgen.RockGenerationMod;
import schmoller.mods.rockgen.recipes.DripstoneDrainRecipe;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipe;

@MethodsReturnNonnullByDefault
public class JeiCompatibilityPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(RockGenerationMod.Id, "jei_compat");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new FluidSpreadRecipeCategory(guiHelper));
        registration.addRecipeCategories(new DripstoneDrainRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        assert Minecraft.getInstance().level != null;
        var recipeManager = Minecraft.getInstance().level.getRecipeManager();
        registration.addRecipes(FluidSpreadRecipeCategory.Type, recipeManager.getAllRecipesFor(FluidSpreadRecipe.Type));
        registration.addRecipes(DripstoneDrainRecipeCategory.Type,
            recipeManager.getAllRecipesFor(DripstoneDrainRecipe.Type)
        );
    }
}
