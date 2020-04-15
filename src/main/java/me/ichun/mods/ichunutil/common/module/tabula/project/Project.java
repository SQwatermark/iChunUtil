package me.ichun.mods.ichunutil.common.module.tabula.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.ichun.mods.ichunutil.client.model.ModelTabula;
import me.ichun.mods.ichunutil.client.render.BufferedImageTexture;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Project extends Identifiable<Project> //Model
{
    public static final int IDENTIFIER_LENGTH = 20;
    public static final int PROJ_VERSION = 5;

    public static final Gson SIMPLE_GSON = new GsonBuilder().disableHtmlEscaping().create();

    //Save file stuff
    @Nullable
    public transient File saveFile;
    public transient boolean isDirty;
    public transient boolean tampered; //file may be tampered?
    public transient boolean isOldTabula; //*GASP*

    //Project texture Stuffs
    private transient BufferedImage bufferedTexture;
    public transient BufferedImageTexture bufferedImageTexture;

    //Client Model
    @OnlyIn(Dist.CLIENT)
    private transient ModelTabula model;

    //defaults
    public String author = "";
    public int projVersion = PROJ_VERSION; //TODO detect if version is old (< 5). Support? Should we support Techne?
    public ArrayList<String> notes = new ArrayList<>();

    public int texWidth = 64;
    public int texHeight = 32;

    public ArrayList<Part> parts = new ArrayList<>();

    public int partCountProjectLife = 0;

    //TODO should we check for tampered files?

    @Override
    public Identifiable<?> getById(String id)
    {
        for(Part part : parts)
        {
            Identifiable<?> ident = part.getById(id);
            if(ident != null)
            {
                return ident;
            }
        }
        return identifier.equals(id) ? this : null;
    }

    @Override
    public String getJsonWithoutChildren()
    {
        ArrayList<Part> parts = this.parts;
        this.parts = null;
        String json = SIMPLE_GSON.toJson(this);
        this.parts = parts;
        return json;
    }

    @Override
    public void transferChildren(Project clone)
    {
        clone.parts = parts;
    }

    @Override
    public void adoptChildren()
    {
        for(Part part : parts)
        {
            part.parent = this;
            part.adoptChildren();
        }
    }

    public boolean save(@Nonnull File saveFile) //file to save as
    {
        return saveProject(this, saveFile);
    }

    @OnlyIn(Dist.CLIENT)
    public void markDirty()
    {
        isDirty = true;

        updateModel();
    }

    @OnlyIn(Dist.CLIENT)
    public void destroy()
    {
        //destroy the model
        setBufferedTexture(null);

    }

    @OnlyIn(Dist.CLIENT)
    public void updateModel()
    {
        if(model != null)
        {
            model.isDirty = true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public ModelTabula getModel()
    {
        if(model == null)
        {
            model = new ModelTabula(this);
        }
        return model;
    }

    public void importProject(@Nonnull Project project, boolean texture)
    {
        markDirty();

        if(texture && project.getBufferedTexture() != null)
        {
            setBufferedTexture(project.getBufferedTexture());
        }
        parts.addAll(project.parts);
    }


    @OnlyIn(Dist.CLIENT)
    public void setBufferedTexture(BufferedImage texture)
    {
        if(bufferedImageTexture != null)
        {
            Minecraft.getInstance().getTextureManager().deleteTexture(bufferedImageTexture.getResourceLocation());

            bufferedImageTexture = null;
        }
        this.bufferedTexture = texture;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getBufferedTextureResourceLocation()
    {
        if(bufferedTexture != null)
        {
            if(bufferedImageTexture == null)
            {
                bufferedImageTexture = new BufferedImageTexture(bufferedTexture);
                Minecraft.getInstance().getTextureManager().loadTexture(bufferedImageTexture.getResourceLocation(), bufferedImageTexture);
            }

            return bufferedImageTexture.getResourceLocation();
        }
        return null;
    }

    public BufferedImage getBufferedTexture()
    {
        return bufferedTexture;
    }

    public void load()
    {
        repair(); //repair first.

        adoptChildren();
    }

    public void repair()
    {
        while(projVersion < PROJ_VERSION)
        {
            if(projVersion <= 4) //TODO UHHH is this necessary?
            {

            }
            projVersion++;
        }
    }

    public static boolean saveProject(Project project, File file)
    {
        try
        {
            file.getParentFile().mkdirs();

            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
            out.setLevel(9);
            out.putNextEntry(new ZipEntry("model.json"));

            byte[] data = SIMPLE_GSON.toJson(project).getBytes();
            out.write(data, 0, data.length);
            out.closeEntry();

            if(project.bufferedTexture != null)
            {
                out.putNextEntry(new ZipEntry("texture.png"));
                ImageIO.write(project.bufferedTexture, "png", out);
                out.closeEntry();
            }

            out.close();

            //save and mark no longer dirty
            //set our save file as the saveFile.
            project.saveFile = file;
            project.isDirty = false;

            return true;
        }
        catch(Exception e)
        {
            iChunUtil.LOGGER.error("Failed to save model: {}", project.name + " - " + project.author);
            e.printStackTrace();
        }
        return false;
    }

    public static class Part extends Identifiable<Part> //ModelRenderer
    {
        public ArrayList<String> notes = new ArrayList<>();

        public int texWidth = 64;
        public int texHeight = 32;
        public boolean matchProject = true;

        public int texOffX;
        public int texOffY;

        //position
        public float rotPX;
        public float rotPY;
        public float rotPZ;

        //angles
        public float rotAX;
        public float rotAY;
        public float rotAZ;

        public boolean mirror;
        public boolean showModel = true;

        public ArrayList<Box> boxes = new ArrayList<>();
        public ArrayList<Part> children = new ArrayList<>();

        public Part(Identifiable<?> parent, int count)
        {
            this.parent = parent;
            this.boxes.add(new Box(this)); //there is code that rely on parts always having 1 box
            this.name = "part" + count;
        }

        @Override
        public Identifiable<?> getById(String id)
        {
            for(Box part : boxes)
            {
                Identifiable<?> ident = part.getById(id);
                if(ident != null)
                {
                    return ident;
                }
            }

            for(Part part : children)
            {
                Identifiable<?> ident = part.getById(id);
                if(ident != null)
                {
                    return ident;
                }
            }
            return identifier.equals(id) ? this : null;
        }

        @Override
        public String getJsonWithoutChildren()
        {
            ArrayList<Box> boxes = this.boxes;
            ArrayList<Part> children = this.children;
            this.boxes = null;
            this.children = null;
            String json = SIMPLE_GSON.toJson(this);
            this.boxes = boxes;
            this.children = children;
            return json;
        }

        @Override
        public void transferChildren(Part clone)
        {
            clone.boxes = boxes;
            clone.children = children;
        }

        @Override
        public void adoptChildren()
        {
            for(Box box : boxes)
            {
                box.parent = this;
                box.adoptChildren();
            }

            for(Part part : children)
            {
                part.parent = this;
                part.adoptChildren();
            }
        }

        public int[] getProjectTextureDims()
        {
            if(parent instanceof Part)
            {
                return ((Part)parent).getProjectTextureDims();
            }
            else if(parent instanceof Project)
            {
                return new int[] { ((Project)parent).texWidth, ((Project)parent).texHeight };
            }
            iChunUtil.LOGGER.error("We can't find out parent's texture dimensions, we have an orphaned Part. Uh oh! Their name is {} and their identifier is {}", name, identifier);
            return new int[] { texWidth, texHeight };
        }

        public static class Box extends Identifiable<Box> //ModelBox
        {
            //the old offsets.
            public float posX;
            public float posY;
            public float posZ;

            public float dimX = 1F;
            public float dimY = 1F;
            public float dimZ = 1F;

            public float expandX;
            public float expandY;
            public float expandZ;

            public Box(Identifiable<?> parent)
            {
                this.parent = parent;
                this.name = "Box";
            }

            @Override
            public Identifiable getById(String id)
            {
                return identifier.equals(id) ? this : null;
            }

            @Override
            public String getJsonWithoutChildren()
            {
                return SIMPLE_GSON.toJson(this);
            }

            @Override
            public void transferChildren(Box clone){}

            @Override
            public void adoptChildren(){} //boxes are infertile
        }
    }

    public Part addPart(Identifiable<?> parent)
    {
        markDirty();

        Part part = new Part(this, ++partCountProjectLife);
        if(parent instanceof Part) //Parts can have children
        {
            part.parent = parent;

            ((Part)parent).children.add(part);
        }
        else if(parent instanceof Part.Box)
        {
            return addPart(parent.parent);
        }
        else
        {
            parts.add(part);
        }

        return part;
    }

    public Part.Box addBox(Identifiable<?> parent)
    {
        markDirty();

        if(parent instanceof Part) //Parts can have children
        {
            Part.Box box = new Part.Box(parent);

            ((Part)parent).boxes.add(box);
            return box;
        }
        else if(parent instanceof Part.Box)
        {
            return addBox(parent.parent);
        }
        else
        {
            Part part = new Part(this, ++partCountProjectLife);
            parts.add(part);
            return part.boxes.get(0);
        }
    }

    public void delete(Identifiable<?> child)
    {
        Identifiable<?> parent = child.parent;
        if(parent instanceof Project) //lets orphan this mofo
        {
            ((Project)parent).parts.remove(child);
        }
        else if(parent instanceof Part)
        {
            ((Part)parent).boxes.remove(child);
            ((Part)parent).children.remove(child);
        }
        markDirty();
    }
}
