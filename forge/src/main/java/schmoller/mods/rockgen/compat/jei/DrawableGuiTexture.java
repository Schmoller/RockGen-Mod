package schmoller.mods.rockgen.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import org.jetbrains.annotations.NotNull;
import schmoller.mods.rockgen.util.GuiTexture;

class DrawableGuiTexture implements IDrawable {
    private final GuiTexture texture;

    public DrawableGuiTexture(GuiTexture texture) {
        this.texture = texture;
    }

    @Override
    public int getWidth() {
        return texture.width();
    }

    @Override
    public int getHeight() {
        return texture.height();
    }

    @Override
    public void draw(@NotNull PoseStack poseStack, int xOffset, int yOffset) {
        texture.render(poseStack, xOffset, yOffset);
    }
}
