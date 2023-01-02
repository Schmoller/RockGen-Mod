package schmoller.mods.rockgen.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import schmoller.mods.rockgen.RockGenerationMod;
import schmoller.mods.rockgen.recipes.DripstoneDrainRecipe;
import schmoller.mods.rockgen.util.GuiTextures;

import java.util.List;

@MethodsReturnNonnullByDefault
class DripstoneDrainRecipeCategory implements IRecipeCategory<DripstoneDrainRecipe> {
    public static final RecipeType<DripstoneDrainRecipe> Type = RecipeType.create(RockGenerationMod.Id,
        DripstoneDrainRecipe.TypeId,
        DripstoneDrainRecipe.class
    );
    private static final DrawableGuiTexture SlotBackground = new DrawableGuiTexture(GuiTextures.JeiSlot);

    private static final int SlotSize = 18;
    private static final int PanelWidth = SlotSize * 3 + 80;

    private final IDrawable background;
    private final IDrawable icon;

    DripstoneDrainRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(PanelWidth, SlotSize * 3 + 24);
        icon = guiHelper.createDrawableItemStack(new ItemStack(Items.POINTED_DRIPSTONE));
    }

    @Override
    public RecipeType<DripstoneDrainRecipe> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.dripstoneDrain.title");
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
        IRecipeLayoutBuilder builder, DripstoneDrainRecipe recipe, IFocusGroup focuses
    ) {
        var drainBlockSlot = builder
            .addSlot(RecipeIngredientRole.CATALYST, 10, 10)
            .setBackground(SlotBackground, -1, -1);

        var drainBlock = recipe.getBlock();
        drainBlock.forEachMatchingRegisteredBlock(block -> drainBlockSlot.addItemStack(new ItemStack(block.asItem())));

        // Just a filler to show the structure needed
        builder
            .addSlot(RecipeIngredientRole.RENDER_ONLY, 10, 10 + SlotSize + 2)
            .addItemStack(new ItemStack(Blocks.STONE))
            .addTooltipCallback((recipeSlotView, tooltip) -> {
                tooltip.add(Component.translatable("jei.dripstoneDrain.root").withStyle(ChatFormatting.YELLOW));
            });
        builder
            .addSlot(RecipeIngredientRole.RENDER_ONLY, 10, 10 + (SlotSize + 2) * 2)
            .addItemStack(new ItemStack(Blocks.POINTED_DRIPSTONE));

        addOutputSlots(builder, recipe);
    }

    private void addOutputSlots(IRecipeLayoutBuilder builder, DripstoneDrainRecipe recipe) {
        final int startX = PanelWidth - 10 - (SlotSize * 3);

        int x = startX;
        int y = 10;

        var outputs = recipe.getOutputs();

        for (var output : outputs.getOutputs()) {
            var chance = output.weight() / (float) outputs.getMaxOutputWeight() * 100;
            builder
                .addSlot(RecipeIngredientRole.OUTPUT, x, y)
                .setBackground(SlotBackground, -1, -1)
                .addItemStack(new ItemStack(output.block().asItem()))
                .addTooltipCallback((recipeSlotView, tooltip) -> {
                    if (chance < 100) {
                        tooltip.add(Component
                            .translatable("jei.fluidSpread.chance", String.format("%.0f%%", chance))
                            .withStyle(ChatFormatting.YELLOW));
                    }
                });

            x += SlotSize;
            if (x > PanelWidth - SlotSize - 10) {
                x = startX;
                y += SlotSize;
            }
        }
    }

    @Override
    public List<Component> getTooltipStrings(
        DripstoneDrainRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY
    ) {
        if (mouseX >= PanelWidth - GuiTextures.JeiColdIcon.width() && mouseX <= PanelWidth && mouseY >= 0 && mouseY <= GuiTextures.JeiColdIcon.height()) {
            return List.of(Component.translatable("jei.dripstoneDrain.ultraWarm"));
        }

        if (mouseX >= 34 && mouseX <= 66 && mouseY >= 15 && mouseY <= 26) {
            return List.of(Component.translatable("jei.dripstoneDrain.probability.description",
                String.format("%01.2f", recipe.getProbability() * 100)
            ));
        }

        return List.of();
    }

    @Override
    public void draw(
        DripstoneDrainRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY
    ) {
        GuiTextures.JeiLargeArrowRight.render(stack, 20 + SlotSize, 30);

        if (!recipe.getAllowUltraWarm()) {
            GuiTextures.JeiColdIcon.render(stack, PanelWidth - GuiTextures.JeiColdIcon.width(), 0);
        }

        Minecraft.getInstance().font.draw(stack,
            Component.translatable("jei.dripstoneDrain.probability",
                String.format("%01.2f", recipe.getProbability() * 100)
            ),
            34,
            15,
            0xFF808080
        );
    }
}
