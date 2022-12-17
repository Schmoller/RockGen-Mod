package schmoller.mods.rockgen.recipes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.List;

public class FluidSpreadRecipeCache {
    private List<GroupedRecipes> groupedRegularRecipes;
    private List<GroupedRecipes> groupedFallingRecipes;

    public void prepare(Collection<FluidSpreadRecipe> recipes) {
        ListMultimap<TagKey<Fluid>, FluidSpreadRecipe> recipesByFluid = ArrayListMultimap.create();

        for (var recipe : recipes) {
            recipesByFluid.put(recipe.fluidToMatch(), recipe);
        }

        groupedRegularRecipes = recipesByFluid.keys().stream().distinct().map(key -> {
            var recipesOfSameFluidType = recipesByFluid.get(key)
                .stream()
                .filter(recipe -> recipe.fluidSpreadDirection() == FluidSpreadRecipe.FluidSpreadDirection.Regular)
                .toList();
            return new GroupedRecipes(key, recipesOfSameFluidType);
        }).filter(group -> !group.recipes.isEmpty()).toList();

        groupedFallingRecipes = recipesByFluid.keys().stream().distinct().map(key -> {
            var recipesOfSameFluidType = recipesByFluid.get(key)
                .stream()
                .filter(recipe -> recipe.fluidSpreadDirection() == FluidSpreadRecipe.FluidSpreadDirection.Down)
                .toList();
            return new GroupedRecipes(key, recipesOfSameFluidType);
        }).filter(group -> !group.recipes.isEmpty()).toList();
    }

    public Iterable<FluidSpreadRecipe> getRegular(FlowingFluid flowingType) {
        for (var group : groupedRegularRecipes) {
            if (flowingType.is(group.fluid)) {
                return group.recipes;
            }
        }

        return List.of();
    }

    public Iterable<FluidSpreadRecipe> getFalling(FlowingFluid flowingType) {
        for (var group : groupedFallingRecipes) {
            if (flowingType.is(group.fluid)) {
                return group.recipes;
            }
        }

        return List.of();
    }

    private record GroupedRecipes(TagKey<Fluid> fluid, List<FluidSpreadRecipe> recipes) {}
}
