package schmoller.mods.rockgen.util;

import net.minecraft.resources.ResourceLocation;
import schmoller.mods.rockgen.RockGenerationMod;

public class GuiTextures {
    // JEI Widgets
    public static final GuiTexture JeiSlot = createTextureFrom("jei/widgets", 0, 0, 18, 18);
    public static final GuiTexture JeiSmallArrowRight = createTextureFrom("jei/widgets", 1, 19, 10, 8);
    public static final GuiTexture JeiSmallArrowLeft = createTextureFrom("jei/widgets", 12, 19, 10, 8);
    public static final GuiTexture JeiSmallArrowDown = createTextureFrom("jei/widgets", 23, 19, 8, 10);
    public static final GuiTexture JeiLargeArrowRight = createTextureFrom("jei/widgets", 1, 31, 22, 17);

    private static GuiTexture createTextureFrom(String id, int x, int y, int width, int height) {
        var location = new ResourceLocation(RockGenerationMod.Id, "textures/gui/" + id + ".png");
        return new GuiTexture(location, x, y, width, height);
    }
}
