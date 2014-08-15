
package com.elusivehawk.engine.lwjgl;

import java.util.List;
import com.elusivehawk.engine.CaelumEngine;
import com.elusivehawk.engine.IGameEnvironment;
import com.elusivehawk.engine.Input;
import com.elusivehawk.engine.render.IRenderEnvironment;
import com.elusivehawk.util.EnumOS;
import com.elusivehawk.util.FileHelper;
import com.elusivehawk.util.json.EnumJsonType;
import com.elusivehawk.util.json.JsonData;
import com.elusivehawk.util.json.JsonObject;

/**
 * 
 * 
 * 
 * @author Elusivehawk
 */
public class LWJGLEnvironment implements IGameEnvironment
{
	private final IRenderEnvironment renderEnviro = new OpenGLEnvironment();
	
	@Override
	public boolean isCompatible(EnumOS os)
	{
		return os != EnumOS.ANDROID;
	}
	
	@Override
	public void initiate(JsonObject json, String... args)
	{
		System.setProperty("org.lwjgl.opengl.Display.noinput", "true");
		
		String lib = null;
		
		if (CaelumEngine.DEBUG && json != null)
		{
			JsonData val = json.getValue("debugNativeLocation");
			
			if (val.type == EnumJsonType.STRING)
			{
				lib = val.value;
				
			}
			
		}
		
		if (lib == null)
		{
			lib = determineLWJGLPath();
			
		}
		
		System.setProperty("org.lwjgl.librarypath", lib);
		
	}
	
	@Override
	public String getName()
	{
		return "CaelumLWJGL";
	}
	
	@Override
	public IRenderEnvironment getRenderEnv()
	{
		return this.renderEnviro;
	}
	
	@Override
	public List<Input> loadInputs()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public static String determineLWJGLPath()
	{
		//TODO: this only works on Debian... but we'll try it for now.
		
		return (EnumOS.getCurrentOS() == EnumOS.LINUX && FileHelper.createFile("/usr/lib/jni/liblwjgl.so").exists()) ? "/usr/lib/jni" : FileHelper.createFile(CaelumEngine.DEBUG ? "lib" : ".", String.format("/lwjgl/native/%s", EnumOS.getCurrentOS().toString())).getAbsolutePath();
	}
	
}
