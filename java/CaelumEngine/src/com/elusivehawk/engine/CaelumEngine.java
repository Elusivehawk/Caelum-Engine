
package com.elusivehawk.engine;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.elusivehawk.engine.assets.AssetManager;
import com.elusivehawk.engine.render.IRenderEnvironment;
import com.elusivehawk.engine.render.RenderContext;
import com.elusivehawk.engine.render.ThreadGameRender;
import com.elusivehawk.engine.render.old.IRenderHUB;
import com.elusivehawk.util.EnumOS;
import com.elusivehawk.util.FileHelper;
import com.elusivehawk.util.IPausable;
import com.elusivehawk.util.ReflectionHelper;
import com.elusivehawk.util.ShutdownHelper;
import com.elusivehawk.util.StringHelper;
import com.elusivehawk.util.Version;
import com.elusivehawk.util.concurrent.IThreadStoppable;
import com.elusivehawk.util.json.EnumJsonType;
import com.elusivehawk.util.json.JsonData;
import com.elusivehawk.util.json.JsonObject;
import com.elusivehawk.util.json.JsonParser;
import com.elusivehawk.util.storage.Pair;
import com.elusivehawk.util.storage.Tuple;
import com.elusivehawk.util.task.TaskManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * The core class for the Caelum Engine.
 * 
 * @author Elusivehawk
 */
public final class CaelumEngine
{
	private static final CaelumEngine INSTANCE = new CaelumEngine();
	
	public static final boolean DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
	public static final Version VERSION = new Version(Version.ALPHA, 1, 0, 0);
	
	private final Map<EnumEngineFeature, IThreadStoppable> threads = Maps.newEnumMap(EnumEngineFeature.class);
	private final Map<EnumInputType, Input> inputs = Maps.newEnumMap(EnumInputType.class);
	private final Map<String, String> startargs = Maps.newHashMap();
	private final TaskManager tasks = new TaskManager();
	private final List<String> startupPrefixes = Lists.newArrayList();
	
	private boolean singleThreadRender = false;
	
	private ILog log = new GameLog();
	private IGameEnvironment env = null;
	private IRenderEnvironment renv = null;
	private JsonObject envConfig = null;
	
	private GameFactory factory = null;
	private Game game = null;
	private GameArguments gameargs = null;
	private AssetManager assets = new AssetManager();
	private RenderContext rcon = null;
	
	private CaelumEngine()
	{
		this.startupPrefixes.add("env:");
		this.startupPrefixes.add("gamefac:");
		this.startupPrefixes.add("verbose:");
		this.startupPrefixes.add("st:");
		
		if (EnumOS.getCurrentOS() != EnumOS.ANDROID)
		{
			Runtime.getRuntime().addShutdownHook(new Thread(() ->
			{
				CaelumEngine.instance().shutDownGame();
				CaelumEngine.instance().clearGameEnv();
				
			}));
			
		}
		
		Iterator<String> itr = this.startupPrefixes.iterator();
		String prefix;
		
		while (itr.hasNext())
		{
			prefix = itr.next();
			
			if (!prefix.endsWith(":"))
			{
				this.log.log(EnumLogType.WTF, "Prefix is missing colon: %s", prefix);
				itr.remove();
				
			}
			
		}
		
	}
	
	public static CaelumEngine instance()
	{
		return INSTANCE;
	}
	
	public static Game game()
	{
		return instance().game;
	}
	
	public static AssetManager assetManager()
	{
		return instance().assets;
	}
	
	public static IGameEnvironment environment()
	{
		return instance().env;
	}
	
	public static ILog log()
	{
		return instance().log;
	}
	
	public static TaskManager tasks()
	{
		return instance().tasks;
	}
	
	public static GameArguments gameArgs()
	{
		return instance().gameargs;
	}
	
	public static IContext getContext(boolean safe)
	{
		Thread t = Thread.currentThread();
		
		if (safe && !(t instanceof IThreadContext))
		{
			return null;
		}
		
		return ((IThreadContext)t).getContext();
	}
	
	public static RenderContext renderContext()
	{
		return renderContext(true);
	}
	
	public static RenderContext renderContext(boolean safe)
	{
		return (RenderContext)getContext(safe);
	}
	
	public static boolean isPaused()
	{
		Thread t = Thread.currentThread();
		
		if (!(t instanceof IPausable))
		{
			return false;
		}
		
		return ((IPausable)t).isPaused();
	}
	
	public static void flipScreen(boolean flip)
	{
		if (instance().rcon != null)
		{
			instance().rcon.onScreenFlipped(flip);
			
		}
		
		if (game() != null)
		{
			game().onScreenFlipped(flip);
			
		}
		
	}
	
	public static void main(String... args)
	{
		instance().createGameEnv(args);
		instance().startGame();
		instance().pauseGame(false);
		
	}
	
	/**
	 * 
	 * ONLY FOR DEBUGGING! I MEAN IT PEOPLE, DON'T RELY ON THIS!
	 * 
	 * @param gamefac
	 * @param args
	 */
	public static void start(GameFactory gamefac, String... args)
	{
		if (instance().factory != null)
		{
			throw new CaelumException("Nuh uh uh, you didn't say the magic word...");
		}
		
		instance().factory = gamefac;
		
		main(args);
		
	}
	
	public void createGameEnv(String... args)
	{
		if (this.game != null)
		{
			return;
		}
		
		//XXX Parsing the starting arguments
		
		List<String> gargs = Lists.newArrayList();
		Map<String, String> strs = Maps.newHashMap();
		Pair<String> spl;
		
		for (String str : args)
		{
			boolean testForGameArg = true;
			
			for (String prefix : this.startupPrefixes)
			{
				if (str.startsWith(prefix))
				{
					spl = StringHelper.splitFirst(str, ":");
					strs.put(spl.one, spl.two);
					testForGameArg = false;
					
					break;
				}
				
			}
			
			if (testForGameArg)
			{
				gargs.add(str);
				
			}
			
		}
		
		this.startargs.putAll(strs);
		this.gameargs = new GameArguments(gargs);
		
		this.log.log(EnumLogType.INFO, "Starting Caelum Engine %s on %s.", VERSION, EnumOS.getCurrentOS());
		
		if (DEBUG)
		{
			for (Entry<String, String> entry : this.startargs.entrySet())
			{
				this.log.log(EnumLogType.INFO, "Argument: %s, %s", entry.getKey(), entry.getValue());
				
			}
			
		}
		
		boolean verbose = "true".equalsIgnoreCase(this.startargs.get("verbose"));
		
		this.log.setEnableVerbosity(verbose);
		
		if (DEBUG)
		{
			this.log.log(EnumLogType.WARN, "Debugging is turned on!");
			
		}
		
		//XXX Loading game environment
		
		if (this.env == null)
		{
			IGameEnvironment env = null;
			Class<?> clazz = null;
			String cl = this.startargs.get("env");
			
			if (cl == null)
			{
				clazz = this.loadEnvironmentFromJson();
				
			}
			else
			{
				try
				{
					clazz = Class.forName(cl);
					
				}
				catch (Exception e){}
				
			}
			
			if (clazz == null)
			{
				this.log.log(EnumLogType.VERBOSE, "Loading default game environment.");
				
				try
				{
					switch (EnumOS.getCurrentOS())
					{
						case WINDOWS:
						case MAC:
						case LINUX: clazz = Class.forName("com.elusivehawk.engine.lwjgl.LWJGLEnvironment"); break;
						case ANDROID: clazz = Class.forName("com.elusivehawk.engine.android.AndroidEnvironment"); break;
						default: this.log.log(EnumLogType.WTF, "Unsupported OS! Enum: %s; OS: %s", EnumOS.getCurrentOS(), System.getProperty("os.name"));
						
					}
					
				}
				catch (Exception e){}
				
			}
			else
			{
				this.log.log(EnumLogType.WARN, "Loading custom game environment, this is gonna suck...");
				
			}
			
			env = (IGameEnvironment)ReflectionHelper.newInstance(clazz, new Class[]{IGameEnvironment.class}, null);
			
			if (env == null)
			{
				this.log.log(EnumLogType.ERROR, "Unable to load environment: Instance couldn't be created. Class: %s", clazz == null ? "NULL" : clazz.getCanonicalName());
				ShutdownHelper.exit("NO-ENVIRONMENT-FOUND");
				
			}
			
			if (!env.isCompatible(EnumOS.getCurrentOS()))
			{
				this.log.log(EnumLogType.ERROR, "Unable to load environment: Current OS is incompatible. Class: %s; OS: %s", clazz == null ? "NULL" : clazz.getCanonicalName(), EnumOS.getCurrentOS());
				ShutdownHelper.exit("NO-ENVIRONMENT-FOUND");
				
			}
			
			env.initiate(this.envConfig, args);
			
			this.env = env;
			this.renv = env.getRenderEnv();
			
			ILog l = this.env.getLog();
			
			if (l != null)
			{
				this.log = l;
				
				l.setEnableVerbosity(verbose);
				
			}
			
			this.singleThreadRender = "true".equalsIgnoreCase(this.startargs.get("st")) || env.singleThreaded();
			
		}
		
		//XXX Loading input
		
		if (this.inputs.isEmpty())
		{
			List<Input> inputList = this.env.loadInputs();
			
			if (inputList == null || inputList.isEmpty())
			{
				this.log.log(EnumLogType.WARN, "Unable to load input");
				
			}
			else
			{
				for (Input input : inputList)
				{
					this.inputs.put(input.getType(), input);
					
					this.log.log(EnumLogType.VERBOSE, "Loaded input of type %s, with class %s", input.getType().name(), input.getClass().getCanonicalName());
					
				}
				
			}
			
		}
		
		if (DEBUG)
		{
			this.factory = new TestGameFactory();
			
		}
		else if (this.factory == null)
		{
			String gamefac = this.startargs.get("gamefac");
			
			if (gamefac == null)
			{
				this.log.log(EnumLogType.ERROR, "Game factory not found: Factory not provided");
				ShutdownHelper.exit("NO-FACTORY-FOUND");
				
			}
			else
			{
				this.factory = (GameFactory)ReflectionHelper.newInstance(gamefac, new Class<?>[]{GameFactory.class}, null);
				
			}
			
		}
		
	}
	
	public void startGame()
	{
		//XXX Loading the game itself
		
		if (this.factory == null)
		{
			this.log.log(EnumLogType.ERROR, "Game factory not found: Factory not provided");
			ShutdownHelper.exit("NO-FACTORY-FOUND");
			
		}
		
		Game g = this.factory.createGame();
		
		if (g == null)
		{
			this.log.log(EnumLogType.ERROR, "Could not load game");
			ShutdownHelper.exit("NO-GAME-FOUND");
			
		}
		
		this.tasks.start();
		
		g.preInit(this.gameargs);
		
		this.log.log(EnumLogType.INFO,"Loading %s", g);
		
		if (g.getGameVersion() == null)
		{
			this.log.log(EnumLogType.WARN, "The game is missing a Version object!");
			
		}
		
		this.game = g;
		
		try
		{
			g.initiateGame(this.gameargs);
			
		}
		catch (Throwable e)
		{
			this.log.err("Game failed to load!", e);
			
			ShutdownHelper.exit("GAME-LOAD-FAILURE");
			
		}
		
		//XXX Creating game threads
		
		if (this.assets == null)
		{
			this.assets = new AssetManager();
			
		}
		
		this.game.loadAssets(this.assets);
		this.assets.initiate();
		
		ThreadGameLoop gameloop = new ThreadGameLoop(this.inputs, this.game);
		
		this.threads.put(EnumEngineFeature.LOGIC, gameloop);
		
		IRenderHUB hub = this.game.getRenderHUB();
		
		if (hub == null)
		{
			this.rcon = new RenderContext(this.renv);
			
		}
		else
		{
			this.rcon = new RenderContext(this.renv, hub);
			this.log.log(EnumLogType.WARN, "Game %s is using the rendering HUB system!!", this.game);
			
		}
		
		IThreadStoppable rt;
		
		if (this.singleThreadRender)
		{
			rt = gameloop;
			gameloop.enableSingleThreadedRendering(this.rcon);
			
		}
		else
		{
			rt = this.renv.createRenderThread(this.rcon);
			
		}
		
		if (rt == null)
		{
			rt = new ThreadGameRender(this.rcon);
			
		}
		
		this.threads.put(EnumEngineFeature.RENDER, rt);
		
		/*this.threads.put(EnumEngineFeature.SOUND, new ThreadSoundPlayer());
		
		IPhysicsSimulator ph = this.game.getPhysicsSimulator();
		
		if (ph != null)
		{
			this.threads.put(EnumEngineFeature.PHYSICS, new ThreadPhysics(ph, this.game.getUpdateCount()));
			
		}*/
		
		//XXX Starting game threads
		
		IThreadStoppable t;
		
		for (EnumEngineFeature fe : EnumEngineFeature.values())
		{
			t = this.threads.get(fe);
			
			if (t instanceof Thread)
			{
				t.setPaused(true);
				((Thread)t).start();
				
			}
			else
			{
				this.threads.remove(fe);
				
			}
			
		}
		
	}
	
	public void pauseGame(boolean pause)
	{
		this.pauseGame(pause, EnumEngineFeature.values());
		
	}
	
	public void pauseGame(boolean pause, EnumEngineFeature... features)
	{
		for (EnumEngineFeature fe : features)
		{
			IThreadStoppable t = this.threads.get(fe);
			
			if (t != null)
			{
				t.setPaused(pause);
				
			}
			
		}
		
	}
	
	public void shutDownGame()
	{
		if (this.threads.isEmpty())
		{
			return;
		}
		
		this.rcon = null;
		
		if (this.game != null)
		{
			this.game.onShutdown();
			this.game = null;
			
		}
		
		for (EnumEngineFeature fe : EnumEngineFeature.values())
		{
			IThreadStoppable t = this.threads.get(fe);
			
			if (t != null)
			{
				t.stopThread();
				
			}
			
		}
		
		this.tasks.stop();
		
		this.threads.clear();
		
	}
	
	public void clearGameEnv()
	{
		if (this.game != null)
		{
			return;
		}
		
		this.inputs.clear();
		this.startargs.clear();
		
		this.env = null;
		this.renv = null;
		this.envConfig = null;
		this.log = new GameLog();
		
		this.gameargs = null;
		this.assets = null;
		
	}
	
	private Class<?> loadEnvironmentFromJson()
	{
		File jsonFile = FileHelper.createFile(".", "gameEnv.json");
		
		if (!FileHelper.isFileReal(jsonFile))
		{
			return null;
		}
		
		JsonData j = JsonParser.parse(jsonFile);
		
		if (j == null)
		{
			return null;
		}
		
		if (!(j instanceof JsonObject))
		{
			return null;
		}
		
		JsonObject json = (JsonObject)j;
		
		JsonData curEnv = json.getValue(EnumOS.getCurrentOS().toString());
		
		if (curEnv == null || curEnv.type != EnumJsonType.OBJECT)
		{
			return null;
		}
		
		this.envConfig = (JsonObject)curEnv;
		JsonData envLoc = this.envConfig.getValue("lib");
		
		if (envLoc == null || envLoc.type != EnumJsonType.STRING)
		{
			return null;
		}
		
		File envLibFile = FileHelper.createFile(envLoc.value);
		
		if (!FileHelper.canReadFile(envLibFile) || !envLibFile.getName().endsWith(".jar"))
		{
			return null;
		}
		
		Tuple<ClassLoader, Set<Class<?>>> tuple = ReflectionHelper.loadLibrary(envLibFile);
		Set<Class<?>> set = tuple.two;
		
		if (set == null || set.isEmpty())
		{
			return null;
		}
		
		for (Class<?> c : set)
		{
			if (IGameEnvironment.class.isAssignableFrom(c))
			{
				return c;
			}
			
		}
		
		return null;
	}
	
}
