
package com.elusivehawk.engine.core;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * 
 * 
 * 
 * @author Elusivehawk
 */
public class GameArguments
{
	protected final Map<String, String> args = Maps.newHashMap();
	
	@SuppressWarnings("unqualified-field-access")
	public GameArguments(Iterable<String> buf)
	{
		String[] spl;
		
		for (String str : buf)
		{
			if (str.contains("="))
			{
				spl = str.split("=");
				
				args.put(spl[0], spl[1]);
				
			}
			
		}
		
	}
	
	public String getString(String name, String d)
	{
		String ret = this.args.get(name);
		
		return ret == null ? d : name;
	}
	
	public byte getByte(String name, byte d)
	{
		byte ret = d;
		
		try
		{
			ret = Byte.parseByte(this.getString(name, "0"));
			
		}
		catch (Exception e){}
		
		return ret;
	}
	
	public short getShort(String name, short d)
	{
		short ret = d;
		
		try
		{
			ret = Short.parseShort(this.getString(name, "0"));
			
		}
		catch (Exception e){}
		
		return ret;
	}
	
	public int getInt(String name, int d)
	{
		int ret = d;
		
		try
		{
			ret = Integer.parseInt(this.getString(name, "0"));
			
		}
		catch (Exception e){}
		
		return ret;
	}
	
	public long getLong(String name, long d)
	{
		long ret = d;
		
		try
		{
			ret = Long.parseLong(this.getString(name, "0"));
			
		}
		catch (Exception e){}
		
		return ret;
	}
	
	public double getDouble(String name, double d)
	{
		double ret = d;
		
		try
		{
			ret = Double.parseDouble(this.getString(name, "0"));
			
		}
		catch (Exception e){}
		
		return ret;
	}
	
	public float getFloat(String name, float d)
	{
		float ret = d;
		
		try
		{
			ret = Float.parseFloat(this.getString(name, "0f"));
			
		}
		catch (Exception e){}
		
		return ret;
	}
	
}
