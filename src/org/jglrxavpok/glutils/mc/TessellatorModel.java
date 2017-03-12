package org.jglrxavpok.glutils.mc;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import org.jglrxavpok.glutils.IndexedModel;
import org.jglrxavpok.glutils.Mesh;
import org.jglrxavpok.glutils.Model;
import org.jglrxavpok.glutils.OBJLoader;
import org.jglrxavpok.glutils.ObjEvent;
import org.jglrxavpok.glutils.ObjEvent.EventType;
import org.jglrxavpok.glutils.ObjModel;
import org.jglrxavpok.glutils.ObjObject;
import org.jglrxavpok.glutils.TessellatorModelEvent;
import org.jglrxavpok.glutils.Vertex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;

/**
 * @author jglrxavpok
 */
public class TessellatorModel extends ObjModel
{

    public static final EventBus MODEL_RENDERING_BUS = new EventBus();

    public TessellatorModel(String string)
    {
        super(string);
        try
        {
            String content = new String(read(Model.class.getResourceAsStream(string)), "UTF-8");
            String startPath = string.substring(0, string.lastIndexOf('/') + 1);
            HashMap<ObjObject, IndexedModel> map = new OBJLoader().loadModel(startPath, content);
            objObjects.clear();
            Set<ObjObject> keys = map.keySet();
            Iterator<ObjObject> it = keys.iterator();
            while(it.hasNext())
            {
                ObjObject object = it.next();
                Mesh mesh = new Mesh();
                object.mesh = mesh;
                objObjects.add(object);
                map.get(object).toMesh(mesh);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void renderImpl()
    {
        Collections.sort(objObjects, new Comparator<ObjObject>()
        {

            @Override
            public int compare(ObjObject a, ObjObject b)
            {
                Vec3d v = Minecraft.getMinecraft().getRenderViewEntity().getPositionVector();
                double aDist = v.distanceTo(new Vec3d(a.center.x, a.center.y, a.center.z));
                double bDist = v.distanceTo(new Vec3d(b.center.x, b.center.y, b.center.z));
                return Double.compare(aDist, bDist);
            }
        });
        for(ObjObject object : objObjects)
        {
            renderGroup(object);
        }
    }

    @Override
    public void renderGroupsImpl(String group)
    {
        for(ObjObject object : objObjects)
        {
            if(object.getName().equals(group))
            {
                renderGroup(object);
            }
        }
    }

    @Override
    public void renderGroupImpl(ObjObject obj)
    {
        Tessellator tess = Tessellator.getInstance();
       // WorldRenderer renderer = tess.getWorldRenderer();
        VertexBuffer renderer = tess.getBuffer();
        if(obj.mesh == null)
            return;
        Vector3f color = new Vector3f(1, 1, 1);
        float alpha = 1f;
        if(obj.material != null)
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, obj.material.diffuseTexture);
            // color = new Vector3f(obj.material.diffuseColor.x*obj.material.ambientColor.x,
            // obj.material.diffuseColor.y*obj.material.ambientColor.y,
            // obj.material.diffuseColor.z*obj.material.ambientColor.z);
            // alpha = obj.material.transparency;
        }
        int[] indices = obj.mesh.indices;
        Vertex[] vertices = obj.mesh.vertices;
        renderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
        for(int i = 0; i < indices.length; i += 3)
        {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            Vertex v0 = vertices[i0];
            Vertex v1 = vertices[i1];
            Vertex v2 = vertices[i2];
            
            renderer.pos(v0.getPos().x, v0.getPos().y, v0.getPos().z).tex(v0.getTexCoords().x, 1f - v0.getTexCoords().y).normal(v0.getNormal().x, v0.getNormal().y, v0.getNormal().z).endVertex();
            renderer.pos(v1.getPos().x, v1.getPos().y, v1.getPos().z).tex(v1.getTexCoords().x, 1f - v1.getTexCoords().y).normal(v1.getNormal().x, v1.getNormal().y, v1.getNormal().z).endVertex();
            renderer.pos(v2.getPos().x, v2.getPos().y, v2.getPos().z).tex(v2.getTexCoords().x, 1f - v2.getTexCoords().y).normal(v2.getNormal().x, v2.getNormal().y, v2.getNormal().z).endVertex();
        }
        tess.draw();
    }

    @Override
    public boolean fireEvent(ObjEvent event)
    {
        Event evt = null;
        if(event.type == EventType.PRE_RENDER_GROUP)
        {
            evt = new TessellatorModelEvent.RenderGroupEvent.Pre(((ObjObject) event.data[1]).getName(), this);
        }
        else if(event.type == EventType.POST_RENDER_GROUP)
        {
            evt = new TessellatorModelEvent.RenderGroupEvent.Post(((ObjObject) event.data[1]).getName(), this);
        }
        else if(event.type == EventType.PRE_RENDER_ALL)
        {
            evt = new TessellatorModelEvent.RenderPre(this);
        }
        else if(event.type == EventType.POST_RENDER_ALL)
        {
            evt = new TessellatorModelEvent.RenderPost(this);
        }
        if(evt != null)
            return !MODEL_RENDERING_BUS.post(evt);
        return true;
    }

    @Deprecated
    public void regenerateNormals()
    {

    }
}
