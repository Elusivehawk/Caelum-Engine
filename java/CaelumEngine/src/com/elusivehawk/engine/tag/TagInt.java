
package com.elusivehawk.engine.tag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * 
 * 
 * @author Elusivehawk
 */
public class TagInt implements ITag<Integer>
{
	protected final String name;
	protected final int i;
	
	@SuppressWarnings("unqualified-field-access")
	public TagInt(String title, int in)
	{
		name = title;
		i = in;
		
	}
	
	@Override
	public byte getType()
	{
		return TagReaderRegistry.INT_ID;
	}
	
	@Override
	public Integer getData()
	{
		return this.i;
	}

	@Override
	public String getName()
	{
		return this.name;
	}
	
	@Override
	public void save(DataOutputStream out) throws IOException
	{
		out.writeInt(this.i);
		
	}
	
	public static class IntReader implements ITagReader<Integer>
	{
		@Override
		public ITag<Integer> readTag(String name, DataInputStream in) throws IOException
		{
			return new TagInt(name, in.readInt());
		}
		
	}
	
}
