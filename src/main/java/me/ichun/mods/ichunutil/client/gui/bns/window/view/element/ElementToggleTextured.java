package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class ElementToggleTextured<T extends ElementToggleTextured> extends ElementToggle<T>
{
    public ResourceLocation textureLocation;
    public boolean warping;

    public ElementToggleTextured(@Nonnull Fragment<?> parent, @Nonnull String tooltip, ResourceLocation rl, Consumer<T> callback)
    {
        super(parent, "", callback);
        this.tooltip = tooltip;
        this.textureLocation = rl;
    }

    public <T extends ElementToggleTextured<?>> T setWarping()
    {
        warping = true;
        return (T)this;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick)
    {
        super.render(mouseX, mouseY, partialTick);

        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        bindTexture(textureLocation);

        if(warping)
        {
            RenderHelper.draw(getLeft() + 2, getTop() + 2, width - 4, height - 4, 0);
        }
        else
        {
            int length = Math.min(width, height) - 4;
            int x = (int)(getLeft() + (width / 2D) - (length / 2D));
            int y = (int)(getTop() + (height / 2D) - (length / 2D));
            RenderHelper.draw(x , y, length, length, 0);
        }
    }
}