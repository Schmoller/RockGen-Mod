package schmoller.mods.rockgen.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import schmoller.mods.rockgen.RockGenerationMod;
import schmoller.mods.rockgen.recipes.BlockMatcher;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipe;
import schmoller.mods.rockgen.util.GuiTextures;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class FluidSpreadRecipeCategory implements IRecipeCategory<FluidSpreadRecipe> {
    public static final RecipeType<FluidSpreadRecipe> Type = RecipeType.create(RockGenerationMod.Id,
        FluidSpreadRecipe.TypeId,
        FluidSpreadRecipe.class
    );
    private static final DrawableGuiTexture SlotBackground = new DrawableGuiTexture(GuiTextures.JeiSlot);

    private static final int SlotSize = 18;
    private static final int PanelWidth = 180;

    private final IDrawable background;
    private final IDrawable icon;

    FluidSpreadRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(PanelWidth, 70);
        icon = guiHelper.createDrawableItemStack(new ItemStack(Items.LAVA_BUCKET));
    }

    @Override
    public RecipeType<FluidSpreadRecipe> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return new TranslatableComponent("jei.fluidSpread.title");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, FluidSpreadRecipe recipe, IFocusGroup focuses
    ) {
        switch (getShapeOf(recipe)) {
            case Fall -> setupRecipeFalling(builder, recipe);
            case FluidToBlock -> setupRecipeFluidToBlock(builder, recipe);
            case FluidToFluid -> setupRecipeFluidToFluid(builder, recipe);
        }

        addOutputSlots(builder, recipe);
    }

    private void setupRecipeFalling(
        IRecipeLayoutBuilder builder, FluidSpreadRecipe recipe
    ) {
        var sourceFluidSlot = builder
            .addSlot(RecipeIngredientRole.CATALYST, 34, 10)
            .setBackground(SlotBackground, -1, -1);

        addMatchingFluids(recipe.getFluidToMatch(), sourceFluidSlot);

        var matchSlot = builder.addSlot(RecipeIngredientRole.CATALYST, 34, 44).setBackground(SlotBackground, -1, -1);

        if (recipe.getTargetFluid().isPresent()) {
            var targetFluid = recipe.getTargetFluid().get();
            addMatchingFluids(targetFluid, matchSlot);
        } else if (recipe.getTargetBlock().isPresent()) {
            var targetBlock = recipe.getTargetBlock().get();
            addMatchingBlocks(targetBlock, matchSlot);
        }
    }

    private void setupRecipeFluidToBlock(
        IRecipeLayoutBuilder builder, FluidSpreadRecipe recipe
    ) {
        var sourceFluidSlot = builder
            .addSlot(RecipeIngredientRole.CATALYST, 17, 10)
            .setBackground(SlotBackground, -1, -1);
        addMatchingFluids(recipe.getFluidToMatch(), sourceFluidSlot);

        var matchSlot = builder.addSlot(RecipeIngredientRole.CATALYST, 51, 10).setBackground(SlotBackground, -1, -1);

        var blockUnderSlot = builder
            .addSlot(RecipeIngredientRole.CATALYST, 34, 36)
            .setBackground(SlotBackground, -1, -1);

        blockUnderSlot.addTooltipCallback((recipeSlotView, tooltip) -> {
            tooltip.add(new TranslatableComponent("jei.fluidSpread.match.blockBelow"));
        });

        if (recipe.getTargetFluid().isPresent()) {
            var targetFluid = recipe.getTargetFluid().get();
            addMatchingFluids(targetFluid, matchSlot);
        } else if (recipe.getTargetBlock().isPresent()) {
            matchSlot.addTooltipCallback((recipeSlotView, tooltip) -> {
                tooltip.add(new TranslatableComponent("jei.fluidSpread.match.block"));
            });

            var targetBlock = recipe.getTargetBlock().get();
            addMatchingBlocks(targetBlock, matchSlot);
        }

        if (recipe.getBlockBelowRequirement().isPresent()) {
            var targetBlock = recipe.getBlockBelowRequirement().get();
            addMatchingBlocks(targetBlock, blockUnderSlot);
        }
    }

    private void setupRecipeFluidToFluid(
        IRecipeLayoutBuilder builder, FluidSpreadRecipe recipe
    ) {
        var sourceFluidSlot = builder.addSlot(RecipeIngredientRole.CATALYST, 10, 10);
        var matchSlot = builder.addSlot(RecipeIngredientRole.CATALYST, 58, 10);
        var blockUnderSlot = builder.addSlot(RecipeIngredientRole.CATALYST, 34, 36);

        sourceFluidSlot.setBackground(SlotBackground, -1, -1);
        matchSlot.setBackground(SlotBackground, -1, -1);
        blockUnderSlot.setBackground(SlotBackground, -1, -1);

        blockUnderSlot.addTooltipCallback((recipeSlotView, tooltip) -> {
            tooltip.add(new TranslatableComponent("jei.fluidSpread.match.blockBelow"));
        });

        addMatchingFluids(recipe.getFluidToMatch(), sourceFluidSlot);

        if (recipe.getFluidState() != FluidSpreadRecipe.FluidSourceState.DontCare) {
            sourceFluidSlot.addTooltipCallback((recipeSlotView, tooltip) -> {
                if (recipe.getFluidState() == FluidSpreadRecipe.FluidSourceState.RequireSource) {
                    tooltip.add(new TranslatableComponent("jei.fluidSpread.match.sourceBlocks").withStyle(ChatFormatting.YELLOW));
                    tooltip.add(new TranslatableComponent("jei.fluidSpread.consumed").withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(new TranslatableComponent("jei.fluidSpread.match.flowingBlocks").withStyle(
                        ChatFormatting.YELLOW));
                }
            });
        }

        if (recipe.getTargetFluid().isPresent()) {
            var targetFluid = recipe.getTargetFluid().get();
            addMatchingFluids(targetFluid, matchSlot);
        } else if (recipe.getTargetBlock().isPresent()) {
            matchSlot.addTooltipCallback((recipeSlotView, tooltip) -> {
                tooltip.add(new TranslatableComponent("jei.fluidSpread.match.block"));
            });

            var targetBlock = recipe.getTargetBlock().get();
            addMatchingBlocks(targetBlock, matchSlot);
        }

        if (recipe.getBlockBelowRequirement().isPresent()) {
            var targetBlock = recipe.getBlockBelowRequirement().get();
            addMatchingBlocks(targetBlock, blockUnderSlot);
        }
    }

    private void addMatchingFluids(TagKey<Fluid> tag, IIngredientAcceptor<?> slot) {
        ForgeRegistries.FLUIDS.forEach(fluid -> {
            if (fluid.is(tag)) {
                slot.addFluidStack(fluid, 1000);
            }
        });
    }

    private void addMatchingBlocks(BlockMatcher matcher, IIngredientAcceptor<?> slot) {
        ForgeRegistries.BLOCKS.forEach(block -> {
            if (matcher.matches(block.defaultBlockState())) {
                slot.addItemStack(new ItemStack(block.asItem()));
            }
        });
    }

    private void addOutputSlots(IRecipeLayoutBuilder builder, FluidSpreadRecipe recipe) {
        final int startX = PanelWidth - 10 - (SlotSize * 3);

        int x = startX;
        int y = 10;

        var totalWeight = 0;
        for (var resultBlock : recipe.getOutputs()) {
            totalWeight = totalWeight + resultBlock.weight();
        }

        for (var output : recipe.getOutputs()) {
            var chance = output.weight() / (float) totalWeight * 100;
            builder
                .addSlot(RecipeIngredientRole.OUTPUT, x, y)
                .setBackground(SlotBackground, -1, -1)
                .addItemStack(new ItemStack(output.block().asItem()))
                .addTooltipCallback((recipeSlotView, tooltip) -> {
                    if (chance < 100) {
                        tooltip.add(new TranslatableComponent("jei.fluidSpread.chance",
                            String.format("%.0f%%", chance)
                        ).withStyle(ChatFormatting.YELLOW));
                    }
                });

            x += SlotSize;
            if (x > PanelWidth - SlotSize - 10) {
                x = startX;
                y += SlotSize;
            }
        }
    }

    public void draw(
        FluidSpreadRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY
    ) {
        switch (getShapeOf(recipe)) {
            case Fall -> GuiTextures.JeiSmallArrowDown.render(stack, 38, 30);
            case FluidToBlock -> GuiTextures.JeiSmallArrowRight.render(stack, 37, 14);
            case FluidToFluid -> {
                GuiTextures.JeiSmallArrowRight.render(stack, 30, 14);
                GuiTextures.JeiSmallArrowLeft.render(stack, 44, 14);
            }
        }

        var arrowStart = (PanelWidth - GuiTextures.JeiLargeArrowRight.width()) / 2 + 6;
        GuiTextures.JeiLargeArrowRight.render(stack, arrowStart, 24);
    }

    @Override
    public ResourceLocation getUid() {
        return Type.getUid();
    }

    @Override
    public Class<? extends FluidSpreadRecipe> getRecipeClass() {
        return Type.getRecipeClass();
    }

    private RecipeShape getShapeOf(FluidSpreadRecipe recipe) {
        if (recipe.fluidSpreadDirection() == FluidSpreadRecipe.FluidSpreadDirection.Down) {
            return RecipeShape.Fall;
        }

        if (recipe.getTargetFluid().isPresent()) {
            return RecipeShape.FluidToFluid;
        }

        return RecipeShape.FluidToBlock;
    }

    private enum RecipeShape {
        FluidToFluid, FluidToBlock, Fall,
    }
}
