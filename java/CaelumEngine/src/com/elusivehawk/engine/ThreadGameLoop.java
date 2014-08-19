
package com.elusivehawk.engine;

import java.util.List;
import com.elusivehawk.util.Internal;
import com.google.common.collect.Lists;

/**
 * 
 * 
 * 
 * @author Elusivehawk
 */
@Internal
public final class ThreadGameLoop extends ThreadCaelum
{
	private final List<Input> input = Lists.newArrayList();
	private final Game game;
	
	@SuppressWarnings("unqualified-field-access")
	public ThreadGameLoop(List<Input> inputMap, Game g)
	{
		game = g;
		
		if (inputMap != null)
		{
			input.addAll(inputMap);
			
		}
		
	}
	
	@Override
	public void update(double delta) throws Throwable
	{
		if (!this.input.isEmpty())
		{
			this.input.forEach((input) ->
			{
				input.update(delta);
				
			});
			
		}
		
		this.game.update(delta);
		
	}
	
	@Override
	public int getTargetUpdateCount()
	{
		return this.game.getUpdateCount();
	}
	
	@Override
	public synchronized void setPaused(boolean pause)
	{
		super.setPaused(pause);
		
		this.game.setPaused(pause);
		
	}
	
	@Override
	public void onThreadStopped(boolean failure)
	{
		this.input.clear();
		
	}
	
}
