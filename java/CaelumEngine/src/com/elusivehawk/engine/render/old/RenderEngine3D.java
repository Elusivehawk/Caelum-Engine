
package com.elusivehawk.engine.render.old;

import java.util.Collection;
import java.util.List;
import com.elusivehawk.engine.render.EnumRenderMode;
import com.elusivehawk.engine.render.RenderHelper;
import com.elusivehawk.engine.render.RenderSystem;
import com.elusivehawk.engine.render.opengl.GLConst;
import com.elusivehawk.engine.render.opengl.GLProgram;
import com.elusivehawk.engine.render.opengl.IGL1;
import com.elusivehawk.engine.render.three.Model;
import com.elusivehawk.engine.render.three.RenderTicket;

/**
 * 
 * Renders entities in the game world.
 * 
 * @author Elusivehawk
 */
@Deprecated
public class RenderEngine3D implements IRenderEngine
{
	@Override
	public void render(RenderSystem sys, IRenderHUB hub)
	{
		if (!hub.getRenderMode().is3D())
		{
			return;
		}
		
		IScene scene = hub.getScene();
		
		if (scene == null)
		{
			return;
		}
		
		Collection<IModelGroup> models = scene.getModels();
		
		if (models == null || models.isEmpty())
		{
			return;
		}
		
		IGL1 gl1 = sys.getGL1();
		
		gl1.glEnable(GLConst.GL_DEPTH_TEST);
		gl1.glDepthFunc(GLConst.GL_LESS);
		
		gl1.glEnable(GLConst.GL_CULL_FACE);
		gl1.glCullFace(GLConst.GL_BACK);
		
		int currTex = 0, tex = 0;
		boolean zBuffer = true;
		
		RenderTicket tkt;
		Model m;
		GLProgram p;
		
		for (IModelGroup group : models)
		{
			List<RenderTicket> tickets = group.getTickets();
			
			if (tickets == null || tickets.isEmpty())
			{
				continue;
			}
			
			for (int c = 0; c < tickets.size(); c++)
			{
				tkt = tickets.get(c);
				
				if (!tkt.updateBeforeUse(sys))
				{
					continue;
				}
				
				m = tkt.getModel();
				p = tkt.getProgram();
				
				if (m == null)
				{
					continue;
				}
				
				if (!p.bind(sys))
				{
					continue;
				}
				
				sys.manipulateProgram(EnumRenderMode.MODE_3D, p);
				
				try
				{
					RenderHelper.checkForGLError(gl1);
					
				}
				catch (Exception e)
				{
					p.unbind(sys);
					
					throw e;
				}
				
				tex = tkt.getTexture() == null ? sys.getDefaultTexture() : tkt.getTexture().getTexId(tkt.getCurrentTexFrame());
				
				if (currTex != tex)
				{
					if (!gl1.glIsTexture(tex))
					{
						tex = sys.getDefaultTexture();
						
					}
					
					gl1.glBindTexture(GLConst.GL_TEXTURE0, tex);
					currTex = tex;
					
				}
				
				if (zBuffer != tkt.enableZBuffering())
				{
					zBuffer = tkt.enableZBuffering();
					
					if (zBuffer)
					{
						gl1.glEnable(GLConst.GL_DEPTH_TEST);
						gl1.glDepthFunc(GLConst.GL_LESS);
						
					}
					else
					{
						gl1.glDisable(GLConst.GL_DEPTH_TEST);
						
					}
					
				}
				
				gl1.glDrawElements(m.getDrawMode(), 0, GLConst.GL_UNSIGNED_INT, m.getPolyCount());
				
				RenderHelper.checkForGLError(gl1);
				
				p.unbind(sys);
				
			}
			
		}
		
		gl1.glDisable(GLConst.GL_CULL_FACE);
		
		if (zBuffer)
		{
			gl1.glDisable(GLConst.GL_DEPTH_TEST);
			
		}
		
	}
	
	@Override
	public int getPriority(IRenderHUB hub)
	{
		return hub.getRenderMode().is3D() ? 0 : -1;
	}
	
}
