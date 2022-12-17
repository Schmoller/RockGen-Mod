package schmoller.mods.rockgen.recipes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.List;

public class FluidSpreadRecipeCache {
    private List<GroupedRecipes> groupedRecipes;

    public void prepare(Collection<FluidSpreadRecipe> recipes) {
        ListMultimap<TagKey<Fluid>, FluidSpreadRecipe> recipesByFluid = ArrayListMultimap.create();

        for (var recipe : recipes) {
            recipesByFluid.put(recipe.fluidToMatch(), recipe);
        }

        groupedRecipes = recipesByFluid.keys()
            .stream()
            .map(key -> new GroupedRecipes(key, recipesByFluid.get(key)))
            .toList();
    }

    public Iterable<FluidSpreadRecipe> get(FlowingFluid flowingType) {
        for (var group : groupedRecipes) {
            if (flowingType.is(group.fluid)) {
                return group.recipes;
            }
        }

        return List.of();
    }

    private record GroupedRecipes(TagKey<Fluid> fluid, List<FluidSpreadRecipe> recipes) {}
}
