package dk.itu.mario.level;

import java.util.Random;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import dk.itu.mario.MarioInterface.Constraints;
import dk.itu.mario.MarioInterface.GamePlay;
import dk.itu.mario.MarioInterface.LevelInterface;
import dk.itu.mario.engine.sprites.SpriteTemplate;
import dk.itu.mario.engine.sprites.Enemy;


public class MyLevel extends Level{
	//Store information about the level
	public   int ENEMIES = 0; //the number of enemies the level contains
	public   int BLOCKS_EMPTY = 0; // the number of empty blocks
	public   int BLOCKS_COINS = 0; // the number of coin blocks
	public   int BLOCKS_POWER = 0; // the number of power blocks
	public   int COINS = 0; //These are the coins in boxes that Mario collect
	public 	int WASTE = 0;

 
	//private static Random levelSeedRandom = new Random();
	public static long lastSeed;
	Random random;
	private int[] odds = new int[5];
	public int totalOdds = 0;
	private static final int ODDS_STRAIGHT = 0;
	private static final int ODDS_HILL_STRAIGHT = 1;
	private static final int ODDS_TUBES = 2;
	private static final int ODDS_JUMP = 3;
	private static final int ODDS_CANNONS = 4;
	private static final int JumpingThreshold = 3;

  
	private int difficulty;
	private int type;
	private int gaps;		// Jumper
	private int turtles;	// Hunter
	private int coins;		// Collector
	private boolean debug = false;
	
	private final int N = 100000;	// the Annealing sampling size to cut the level builder off at
	private int globalLevelRatingPeak = 0; // the Annealing current highest level rating
	private int localLevelRatingPeak = 0;
	private int globalPeakLevel; // The highest rated Level holder
	private int localPeakLevel; // highest rated Level in the sample holder
	private byte[][] globalPeakLevelMap;
	private byte[][] localPeakLevelMap;
	private SpriteTemplate[][] globalPeakSpriteTemplates;
	private SpriteTemplate[][] localPeakSpriteTemplates;
	private int[] levelLibrary = new int[width]; // library of points in the level where a pattern was created
	private int[] localPeakLibrary = new int[width]; // library of points in the level where a pattern was created in the local Max Level

	
	private GamePlay playerData;
	private boolean[] playerCode = new boolean[3]; // playerCode[0] = 0 or 1 if killer, playercode[1] = 0 or 1 if collector, playerCode[2] = 0 or 1 if jumper
	
	/* Tools -
	 * 		o Coins - (spawns on ground)= Coins++
	 * 		o Power Up Blocks - (Jump) = Power Up
	 * 		o Coin Blocks - (Jump && Power Up) = Coins++
	 * 		o Empty Blocks - (Jump && Power Up) = Nothing
	 * 		o Hills - (Jump) = (Kill || Coins || Nothing)
	 * 		o Pits - (Jump) = Nothing
	 * 		o Goombas - (Jump) = Kill
	 * 		o Koopas - (Jump || Jump x2) = (Kill || Kill x2)
	 * 		o Cannons - (Jump) = (Kill || Nothing)
	 */
	
	/*
	 * Pecking Order: Killer > Collector > Jumper
	 * 
	 * Assumed Scoring based on type of player
	 * Killer -
	 * 		Pros: 
	 * 			o Koopas + Goombas
	 * 			o Goombas
	 * 			o Goomba
	 * 			o Cannon
	 * 		
	 * 		Cons:
	 * 			o Hills
	 * 			o Koopas
	 * 
	 * Collector -
	 * 		Pros:
	 * 			o Power Up
	 * 			o Coins
	 * 			o CoinBlocks
	 * 			o Empty Blocks
	 * 
	 * 		Cons:
	 * 			o Hills 
	 * 			o Enemies
	 *			o Long rows of blocks
	 * 
	 * Jumper -
	 * 		Pros:
	 * 			o Hills
	 * 			o Blocks
	 * 			o Block patterns
	 * 			o Koopas
	 * 			o Higher Coins
	 * 			o Pits After Blocks
	 * 	
	 * 		Cons:
	 * 			o Pits
	 * 
	 * 
	 * Determining Type of Player:  
	 * Killer: 
         * enemyKillByFire; //number of enemies killed by shooting them
         * enemyKillByKickingShell; //number of enemies killed by kicking a shell on them
         * totalEnemies; //total number of enemies
         * GoombasKilled; //number of Goombas Mario killed
         * RedTurtlesKilled; //number of Red Turtle Mario killed
         * GreenTurtlesKilled;//number of Green Turtle Mario killed
         * ArmoredTurtlesKilled; //number of Armored Turtle Mario killed
         * CannonBallKilled; //number of Cannon Ball Mario killed
         * JumpFlowersKilled; //number of Jump Flower Mario killed
         * ChompFlowersKilled; //number of Chomp Flower Mario killed
		 *
     * Collector:
         * emptyBlocksDestroyed; //number of empty blocks destroyed
         * coinBlocksDestroyed; //number of coin block destroyed
         * percentageBlocksDestroyed; //percentage of all blocks destroyed
         * percentageCoinBlocksDestroyed; //percentage of coin blocks destroyed
         * percentageEmptyBlockesDestroyed; //percentage of empty blocks destroyed
         * percentagePowerBlockDestroyed; //percentage of power blocks destroyed	         
         * coinsCollected; //number of coins collected
         * totalEmptyBlocks; //total number of empty blocks
         * totalCoinBlocks; //total number of coin blocks
         * totalpowerBlocks; //total number of power blocks
         * totalCoins; //total number of coins
         * 
     * Jumper:
         * aimlessJumps; //total jumps - (stomp kills + coinblocks destroyed + power up destroyed + empty blocks destroyed)
         * jumpsNumber; // total number of jumps
         * 
     * Too Specific:
         * completionTime; //counts only the current run on the level, excluding death games
         * duckNumber; //total number of ducks
         * timeSpentDucking; // time spent in ducking mode
         * timesPressedRun;//number of times the run key pressed
         * timeSpentRunning; //total time spent running
         * timeRunningRight; //total time spent running to the right
         * timeRunningLeft;//total time spent running to the left
         * powerBlocksDestroyed; //number of power block destroyed
         * kickedShells; //number of shells Mario kicked
         * totalTimeLittleMode; //total time spent in little mode
         * totalTimeLargeMode; //total time spent in large mode
         * totalTimeFireMode; //total time spent in fire mode
         * 
     * Useless:
         * totalTime;//sums all the time, including from previous games if player died
         * timesSwichingPower; //number of Times Switched Between Little, Large or Fire Mario
         * timesOfDeathByFallingIntoGap; //number of death by falling into a gap
         * timesOfDeathByRedTurtle; //number of times Mario died by red turtle
         * timesOfDeathByArmoredTurtle; //number of times Mario died by Armored turtle
         * timesOfDeathByGoomba; //number of times Mario died by Goomba
         * timesOfDeathByGreenTurtle; //number of times Mario died by green turtle
         * timesOfDeathByJumpFlower; //number of times Mario died by Jump Flower
         * timesOfDeathByCannonBall; //number of time Mario died by Cannon Ball
         * timesOfDeathByChompFlower; //number of times Mario died by Chomp Flower
	 */
	
	
		public MyLevel(int width, int height)
	    {
			super(width, height);
	    }


		public MyLevel(int width, int height, long seed, int difficulty, int type, GamePlay playerMetrics)
	    {
	        this(width, height);
	        this.playerData = playerMetrics;
	        creat(seed, difficulty, type);
	    }

	    public void creat(long seed, int difficulty, int type)
	    {
	        this.type = type;
	        this.difficulty = difficulty;
	        
	        difficulty = 10;

	        lastSeed = seed;
	        random = new Random(seed);
	        
	        // Default Odds:
	        
	        odds[ODDS_STRAIGHT] = 40;
	        odds[ODDS_HILL_STRAIGHT] = 30;
	        odds[ODDS_TUBES] = 14;
	        odds[ODDS_JUMP] = 12;
	        odds[ODDS_CANNONS] = 1;
	        
	        
	        // Determine Player's play style
	        
	        // Is the player a killer?
	        
	        // If the player kills a majority of enemies with fire
	        // If the player kills .8 of all enemies

	        int totalEnemiesKilled = playerData.GoombasKilled + playerData.RedTurtlesKilled + playerData.GreenTurtlesKilled + playerData.ArmoredTurtlesKilled + playerData.CannonBallKilled + playerData.JumpFlowersKilled + playerData.ChompFlowersKilled;
	        float percentEnemiesKilled = totalEnemiesKilled/(float)playerData.totalEnemies;
	        float percentKilledByFire = playerData.enemyKillByFire/(float)totalEnemiesKilled;
	        
	        if(percentKilledByFire >= 0.2 || percentEnemiesKilled >= 0.7)
	        {
		         /*	Killer Smoothing:
			         *	if the player killed more goombas put in more goombas
			         *	if the player killed more x enemy put more x enemy
			         *	if the player killed mostly with shells, then group up koopas in the waves of enemies
			         *	if the player killed mostly with fire, make sure power ups are more common
			         *	if the player didn't kill with fire or shells, then make groups bigger to bounce on them
		         */
	        	
		        odds[ODDS_STRAIGHT] += 5;
		        odds[ODDS_HILL_STRAIGHT] -= 15;
		        odds[ODDS_TUBES] +=  0;
		        odds[ODDS_JUMP] += 0;
		        odds[ODDS_CANNONS] += 1;
		        playerCode[0] = true;
	        }
	        System.out.println("killed with fire: "+percentKilledByFire + " killed in general: "+percentEnemiesKilled + " total enemies killes: "+ totalEnemiesKilled);
	              
	        // Is the player a collector?
	        
	        // If the percentage of blocks destroyed is high
	        // If the percentage of coins collected is over .8
	        
	        float percentageCoinsCollected = playerData.coinsCollected/(float)playerData.totalCoins;
	        if(playerData.percentageBlocksDestroyed >= 0.8 || percentageCoinsCollected >= 0.8)
	        {
		         /*	Collector Smoothing:
			         *	if the player didn't break many blocks, but collected a lot of coins
			      */ 
		        odds[ODDS_STRAIGHT] += 5;
		        odds[ODDS_HILL_STRAIGHT] += 0;
		        odds[ODDS_TUBES] += 0;
		        odds[ODDS_JUMP] += 0;
		        odds[ODDS_CANNONS] += 0;
		        playerCode[1] = true;
	        }
	        System.out.println("blocks destroyed: "+ playerData.percentageBlocksDestroyed+ " percent coins collected: "+percentageCoinsCollected);
	        System.out.println("coinsCollected: "+ playerData.coinsCollected + " coinblocks destroyed: "+playerData.coinBlocksDestroyed);
	        
	        // Is the player a jumper?
	        
	        // If the percentage of aimless jumps is greater that .3
	        float percentageAimlessJumps = (float)(playerData.aimlessJumps) /((float)playerData.jumpsNumber);
	        if(percentageAimlessJumps >= 0.8 && playerData.jumpsNumber > 80)
	        {
	        	/*	Jumper Smoothing:
	        	 * 
	        	 */
		        odds[ODDS_CANNONS] += 1;
		        odds[ODDS_STRAIGHT] += 0;
		        odds[ODDS_HILL_STRAIGHT] += 5;
		        odds[ODDS_TUBES] += 5;
		        odds[ODDS_JUMP] =+ 5;
		        odds[ODDS_CANNONS] -=3;
		        playerCode[2] = true;
	        }
	        System.out.println("percent aimless jumps: "+ percentageAimlessJumps + " jumps total: "+playerData.jumpsNumber);
	        
	        /* Create Customized odds here:
	         *
	         *
	         */
	        if (type != LevelInterface.TYPE_OVERGROUND) {
	            odds[ODDS_HILL_STRAIGHT] = 0;
	        }
	        
	        // Set odds
	        for (int i = 0; i < odds.length; i++) 
	        {
	            //failsafe (no negative odds)
	            if (odds[i] < 0) 
	            {
	                odds[i] = 0;
	            }

	            // add up all numbers from inside the chance array
	            totalOdds += odds[i];
	        }
	        
	        //create the start location
	        int length = 0;
	        
	        // Annealing
	        for(int i = 0; i<N; i++)
	        {
	        	// Reset back to empty level
	        	for(int k=0;k<width;k++)
	        	{
	        		for(int j=0; j<height;j++)
	        		{
	        			setBlock(k,j,(byte)0);
	        			if(getSpriteTemplate(k,j) != null)
	        				getSpriteTemplate(k,j).isDead = true;
	        		}
	        	}
	        	length = buildStraight(0, width, true, true);            		// Create beginning of level    
	        	
	        	
	        	/*
	        	for(int x=0;x<width;x++)
	        	{
		        	setBlock(x,9,BLOCK_EMPTY);
	        	}
	        	*/
	        	setBlock(5,5,BLOCK_EMPTY);
	        	setBlock(4,5-1,BLOCK_EMPTY);
	        	setBlock(3,5-2,BLOCK_EMPTY);


	        	
	        	levelLibrary = new int[width];							// Create empty library of patterns in level
	        	int[] localPeakLibraryCoins = new int[width];
	        	int[] localPeakLibraryEnemies = new int[width];
	        	int[] localPeakLibraryEBlocks = new int[width];
	        	int[] localPeakLibraryPBlocks = new int[width];
	        	int[] localPeakLibraryWaste = new int[width];

	        	int librarySize = 0;
	        	int currentRating = 0;									// Create current Rating of level made
	        	
	        	// With the empty level look for a local peak level
	        	// If the level is a global peak it will replace the global peak level as the best level possible
	        	int savedX = 0;
	        	int savedY = 0;
	        	do
	        	{
	        		//System.out.println("yea");
	        		ENEMIES = 0; //the number of enemies the level contains
	        		BLOCKS_EMPTY = 0; // the number of empty blocks
	        		BLOCKS_COINS = 0; // the number of coin blocks
	        		BLOCKS_POWER = 0; // the number of power blocks
	        		COINS = 0;
	        		WASTE = 0;
	        		// set the new rating to beat
	        		localLevelRatingPeak = currentRating;
	        		
	        		
		        	// Set new localPeak and library
		        	localPeakLevel = length;
		        	
		        	//localPeakLevelMap = getMap().clone();
		        	localPeakLevelMap = new byte[width][height];
		        	localPeakSpriteTemplates = new SpriteTemplate[width][height];
		        	for(int x = 0;x<width;x++)
		        	{
		        		for(int y=0;y<height;y++)
		        		{
		        			localPeakLevelMap[x][y] = this.getBlock(x, y);
		        			localPeakSpriteTemplates[x][y] = this.getSpriteTemplate(x,y);
		        			
		        			if(getSpriteTemplate(x,y) != null)
		        			{
		        				//System.out.print(localPeakSpriteTemplates[x][y]);
		        				savedX = x;
		        				savedY = y;
		        			}
		        			
		        		}
		        	}


		        	//localPeakSpriteTemplates = getSpriteTemplates().clone();
		        	localPeakLibrary = new int[width];
		        	for(int x = 0; x<width; x++)
		        	{
		        		localPeakLibrary[x]= levelLibrary[x];
		        	}
		        	//localPeakLibrary = levelLibrary.clone();
		        	
		        	currentRating = 0;
		        	int mutatedGene = -1;
		        	length = 0;
		        	
		        	//System.out.println("lib size: " +librarySize);
		        	if(librarySize > 0)
		        	{
			        	mutatedGene = random.nextInt(librarySize);
			        	//System.out.println("mut: "+mutatedGene);
		        	}
        			
		        	// Create middle of level
        			int k = 0;
			        while (length < width - 64)
			        {

			        	/*
			            //If the level keeps this piece placement
			        	if(k >= localPeakLibrary.length)
			        	{
			        		System.out.println("1 failed ");
			        	}
			        	
			        	if(localPeakLibrary[k]==0)
			        	{
			        		System.out.println("2 failed: " +k);
			        	}
			        	
			        	if(mutatedGene<=-1)
			        	{
			        		System.out.println("3 failed: "+mutatedGene);
			        	}
			        	
			        	if(k == mutatedGene)
			        	{
			        		
			        		System.out.println("4 failed ");

			        	}
			        	*/
			        	if(k < localPeakLibrary.length && localPeakLibrary[k]!=0 && (mutatedGene>-1 && k != mutatedGene))
			        	{
			        		if(localPeakLibraryEnemies[k]>0)
			        		{
			        			//System.out.println("new enemy row: "+ localPeakLibraryEnemies[k]);
			        			for(int y = 0; y<height; y++)
			        			{
			        				if(getSpriteTemplate(k,y) != null && !getSpriteTemplate(k,y).isDead)
			        				{
			        					getSpriteTemplate(k,y).isDead = false;
			        				}
			        			}
			        		}
			        		for(int x = length; x<localPeakLibrary[k]+length; x++)
			        		{
			        			for(int y =0;y<height;y++)
			        			{
			        				setBlock(x,y,localPeakLevelMap[x][y]);
			        				

			        				setSpriteTemplate(x,y,localPeakSpriteTemplates[x][y]);
			        				
			        				

			        				//System.out.print(localPeakSpriteTemplates[x][y].isDead);

				        			if(getSpriteTemplate(x,y) != null)
				        			{
				        				//System.out.print(localPeakSpriteTemplates[x][y]);
				        			}
			        			}
			        		}
			        		length += localPeakLibrary[k];
			        		
			        		levelLibrary[k] = localPeakLibrary[k];
			        		
				        	COINS += localPeakLibraryCoins[k];
				        	ENEMIES += localPeakLibraryEnemies[k];
				        	BLOCKS_EMPTY += localPeakLibraryEBlocks[k];
				        	BLOCKS_POWER += localPeakLibraryPBlocks[k];
				        	WASTE += localPeakLibraryWaste[k];
			        	}else 
			        	{
			        		
			        		int prevEnemies = ENEMIES; 
			        		int prevEmpty = BLOCKS_EMPTY;
			        		int prevBlockCoins = BLOCKS_COINS;
			        		int prevBlockPower = BLOCKS_POWER;
			        		int prevCoins = COINS;
			        		int prevWaste = WASTE;
				        	//If the level throws out previous piece for a random one
			        		int pieceOfLevel = buildZone(length, width - length, true);
			        		
			        		//int pieceOfLevel = buildStraight(length,width-length, false);
				            length += pieceOfLevel;
				            
				            if(k < localPeakLibrary.length)
				            {
				            	levelLibrary[k] = pieceOfLevel;
					        	localPeakLibraryCoins[k] = COINS-prevCoins;
					        	localPeakLibraryEnemies[k] = ENEMIES - prevEnemies;
					        	localPeakLibraryEBlocks[k] = BLOCKS_EMPTY - prevEmpty;
					        	localPeakLibraryPBlocks[k] = BLOCKS_POWER - prevBlockPower;
					        	localPeakLibraryWaste[k] = WASTE - prevWaste;
				            }
				            
			        	}
			            k++;
			        }
			        ///System.out.println("wat");
			        librarySize = k;
			        //System.out.print("k: "+k+" librarysize: "+librarySize);
			        // Evaluate middle of level (give a level rating)
			        int j = 0;
			        while(levelLibrary[j]>0)
			        {
			        	// piece of the constructed level
			        	j++;
			        }

			        currentRating = 0;
			        //if add a point for enemies
			        if(playerCode[0])
			        {
			        	currentRating+= 10*ENEMIES;
			        }
			        
			        //playerCode[1]= true;
			        if(playerCode[1])
			        {
			        	currentRating+= 3*COINS;
			        	currentRating+= 2*BLOCKS_COINS;
			        	currentRating+= 1*BLOCKS_POWER;
			        	currentRating-= 1*BLOCKS_EMPTY;
			        	currentRating-= 5*ENEMIES;
			        }
			        
			        if(playerCode[2])
			        {
			        	if(BLOCKS_EMPTY<(width/4))
			        	{
			        		currentRating+= 1*BLOCKS_EMPTY;
			        	}else 
			        	{
			        		currentRating-= 1*BLOCKS_EMPTY;
			        	}
			        }
			        //System.out.print(currentRating);
			        //currentRating -= 1*WASTE;
			        //System.out.println(currentRating);
			        
			        /*
			        if(currentRating>0)
			        	currentRating = random.nextInt(currentRating)+ 5;
			        else if(currentRating == 0) currentRating = random.nextInt(5)+5;
		        	*/
			        
			        //System.out.println(currentRating+ " > "+ localLevelRatingPeak);

			        /*
			        if(currentRating>localLevelRatingPeak)
			        	System.out.println(ENEMIES +" rating: "+ currentRating);
			        */
			        if(currentRating <= localLevelRatingPeak && currentRating>0)
			        {
			        	//System.out.println(ENEMIES +" current rating: "+ currentRating + " peak: "+localLevelRatingPeak);
			        }
			        
	        	}while(currentRating > localLevelRatingPeak);
	        	
	        	//System.out.println("Exit the loope: "+ENEMIES +" current rating: "+ currentRating + " peak: "+localLevelRatingPeak);
	        	// adjust global best level
	        	if(localLevelRatingPeak > globalLevelRatingPeak)
	        	{
	        		System.out.println("savedX: "+savedX+" savedY: "+savedY);
	        		
    				if(localPeakSpriteTemplates[savedX][savedY] != null)
    				{
        				System.out.println(localPeakSpriteTemplates[savedX][savedY]);
    				}
    				
    				System.out.println("globalChange: "+ENEMIES + " rating: "+localLevelRatingPeak);
		        	globalLevelRatingPeak = localLevelRatingPeak;
		        	globalPeakLevel = localPeakLevel;
		        	
		        	//globalPeakLevelMap = localPeakLevelMap.clone();
		        	globalPeakLevelMap = new byte[width][height];
		        	globalPeakSpriteTemplates = new SpriteTemplate[width][height];
		        	for(int x = 0;x<width;x++)
		        	{
		        		for(int y=0;y<height;y++)
		        		{
		        			globalPeakLevelMap[x][y] = localPeakLevelMap[x][y];
		        			globalPeakSpriteTemplates[x][y] = localPeakSpriteTemplates[x][y];
		        			if(localPeakSpriteTemplates[x][y] !=null && !localPeakSpriteTemplates[x][y].isDead)
		        			{
		        				System.out.println(localPeakSpriteTemplates[x][y].isDead);
		        				//setBlock(9)
		        				globalPeakSpriteTemplates[x][y] = new SpriteTemplate(localPeakSpriteTemplates[x][y].type,false);
		        			}
		        			
		        			
		        			/*
		        			if(localPeakSpriteTemplates[x][y] != null)
		        			{
		        				System.out.print(localPeakSpriteTemplates[x][y]);
		        			}
		        			
		    				if(localPeakSpriteTemplates[savedX][savedY] != null)
		    				{
		        				System.out.print(localPeakSpriteTemplates[savedX][savedY]);
		    				}
		        			*/
		        		}
		        	}
		        	//globalPeakSpriteTemplates = localPeakSpriteTemplates.clone();
		        	System.out.println("before "+globalPeakLevelMap[13][height-1]);
	        	}    	
	        }
	        // set best scored level
        	//System.out.println("final: "+globalLevelRatingPeak);
	        
        	//System.out.println("before "+globalPeakLevelMap[13][height-1]);

        	for(int k=0;k<width;k++)
        	{
        		for(int j=0; j<height;j++)
        		{
        			setBlock(k,j,(byte)0);
        			
        			/*
        			if(getSpriteTemplate(k,j) != null)
        				getSpriteTemplate(k,j).isDead = true;
        			*/
        		}
        	}

        	//length = buildStraight(0, width, true, true);
	        length = globalPeakLevel;

	        for(int x =0; x <width;x++)
	        {
	        	for(int y = 0; y<height; y++)
	        	{
	        		/*
	        		if(x==13 && y==height-1)
	        			System.out.println("after "+ globalPeakLevelMap[x][y]);
	        		*/
	        		try
	        		{
	        			setBlock(x,y,globalPeakLevelMap[x][y]);
	        		}
	        		catch(Exception e)
	        		{
	        			
	        		}
	        		
	        		try
	        		{
		        		if(globalPeakSpriteTemplates[x][y] != null)
		        		{
		        			setSpriteTemplate(x,y,globalPeakSpriteTemplates[x][y]);
		        		}
	        		}
	        		catch(Exception e)
	        		{
	        			
	        		}
	        	}
	        }
	        
	        //set the end piece
	        int floor = height - 1 - random.nextInt(4);
	        
	        //creat the exit
	        xExit = length + 8;
	        yExit = floor;

	        // fills the end piece
	        for (int x = length; x < width; x++)
	        {
	            for (int y = 0; y < height; y++)
	            {
	                if (y >= floor)
	                {
	                    setBlock(x, y, GROUND);
	                }
	            }
	        }

	        if (type == LevelInterface.TYPE_CASTLE || type == LevelInterface.TYPE_UNDERGROUND)
	        {
	            int ceiling = 0;
	            int run = 0;
	            for (int x = 0; x < width; x++)
	            {
	                if (run-- <= 0 && x > 4)
	                {
	                    ceiling = random.nextInt(4);
	                    run = random.nextInt(4) + 4;
	                }
	                for (int y = 0; y < height; y++)
	                {
	                    if ((x > 4 && y <= ceiling) || x < 1)
	                    {
	                        setBlock(x, y, GROUND);
	                    }
	                }
	            }
	        }

	        fixWalls();

	    }
	    
	    private int buildZone(int x, int maxLength, Boolean cleanse) {
	        int t = random.nextInt(totalOdds);
	        int type = 0;

	        for (int i = 0; i < odds.length; i++) 
	        {
	        	// if the odds of jump are good enough then grab it first
	            if(odds[ODDS_JUMP] > t)
	            {
	            	type = ODDS_JUMP;
	            	break;
	            }
	            
	            //System.out.println("i: "+i+" odds[i]: "+odds[i]+ " t: "+t);
	            
	            // if the odds of jump are not good enough then grab whatever you can
	        	if (odds[i] > t) 
	        	{
	                type = i;
	                
	                break;
	            }
	        	
	        	int nTotal = 0;
                for(int j = i;j>=0;j--)
                {
                	nTotal += odds[j];
                }
                //System.out.println("nTotal: "+ nTotal +" totalodds: "+totalOdds);
                if(nTotal<totalOdds)
                	t = random.nextInt(totalOdds-nTotal);
                else if(i+1<odds.length)
                	t = random.nextInt(2*odds[i+1]);
	        }
	        
	        //System.out.println(type);

	        cleanse = false;
	        switch (type) {
	        case ODDS_STRAIGHT:
	            return buildStraight(x, maxLength, false, cleanse);
	        case ODDS_HILL_STRAIGHT:
	            return buildHillStraight(x,maxLength, cleanse);
	        case ODDS_TUBES:
	            return buildTubes(x, maxLength, cleanse);
	        case ODDS_JUMP:
	            if (gaps < Constraints.gaps)
	                return buildStraight(x, maxLength,false, cleanse);
	            else
	                return buildJump(x, maxLength, cleanse);
	        case ODDS_CANNONS:
	            return buildCannons(x, maxLength, cleanse);
	        }
	        return 0;
	    }


	    private int buildJump(int xo, int maxLength, Boolean cleanse)
	    {	gaps++;
	    	//jl: jump length
	    	//js: the number of blocks that are available at either side for free
	        int js = random.nextInt(4) + 2;
	        int jl = random.nextInt(2) + 2;
	        int length = js * 2 + jl;
	        
	        int floor = height - 1 - random.nextInt(4);

	        
	        if(cleanse)
	        {
	        	for(int x = xo; x<xo+length; x++)
	        	{
	        		for(int y=0; y<height; y++)
	        		{
	        			if(y>=floor)
	        			{
	        				setBlock(x,y,(byte)0);
	        			}
	        		}
	        	}
	        }

	        boolean hasStairs = random.nextInt(3) == 0;

	      //run from the start x position, for the whole length
	        for (int x = xo; x < xo + length; x++)
	        {
	            if (x < xo + js || x > xo + length - js - 1)
	            {
	            	//run for all y's since we need to paint blocks upward
	                for (int y = 0; y < height; y++)
	                {	//paint ground up until the floor
	                    if (y >= floor)
	                    {
	                        setBlock(x, y, GROUND);
	                    }
	                  //if it is above ground, start making stairs of rocks
	                    else if (hasStairs)
	                    {	//LEFT SIDE
	                        if (x < xo + js)
	                        { //we need to max it out and level because it wont
	                          //paint ground correctly unless two bricks are side by side
	                            if (y >= floor - (x - xo) + 1)
	                            {
	                                setBlock(x, y, ROCK);
	                            }
	                        }
	                        else
	                        { //RIGHT SIDE
	                            if (y >= floor - ((xo + length) - x) + 2)
	                            {
	                                setBlock(x, y, ROCK);
	                            }
	                        }
	                    }
	                }
	            }
	        }

	        return length;
	    }

	    private int buildCannons(int xo, int maxLength, Boolean cleanse)
	    {
	        int[] xcoordinates = new int[maxLength*height];
	        int[] ycoordinates = new int[maxLength*height];
	        int a = 0;
	        
	    	ENEMIES++;
	        int length = random.nextInt(10) + 2;
	        if (length > maxLength) length = maxLength;

	        int floor = height - 1 - random.nextInt(4);
	        
	        if(cleanse)
	        {
	        	for(int x = xo; x<xo+length; x++)
	        	{
	        		for(int y=0; y < floor; y++)
	        		{
	        			setBlock(x,y,(byte)0);
	        		}
	        	}
	        }
	        int xCannon = xo + 1 + random.nextInt(4);
	        for (int x = xo; x < xo + length; x++)
	        {
	            if (x > xCannon)
	            {
	                xCannon += 2 + random.nextInt(4);
	            }
	            if (xCannon == xo + length - 1) xCannon += 10;
	            int cannonHeight = floor - random.nextInt(4) - 1;

	            for (int y = 0; y < height; y++)
	            {
	                if (y >= floor)
	                {
                    	if(getBlock(x,y)!=0)
                    	{
                    		return 0;
                    	}
	                    setBlock(x, y, GROUND);
	                }
	                else
	                {
	                    if (x == xCannon && y >= cannonHeight)
	                    {
	                        if (y == cannonHeight)
	                        {
	                        	if(getBlock(x,y)!=0)
	                        	{
	                        		return 0;
	                        	}
	                            setBlock(x, y, (byte) (14 + 0 * 16));
	                        }
	                        else if (y == cannonHeight + 1)
	                        {
	                        	if(getBlock(x,y)!=0)
	                        	{
	                        		return 0;
	                        	}
	                            setBlock(x, y, (byte) (14 + 1 * 16));
	                        }
	                        else
	                        {
	                        	if(getBlock(x,y)!=0)
	                        	{
	                        		return 0;
	                        	}
	                            setBlock(x, y, (byte) (14 + 2 * 16));
	                        }
	                    }
	                }
	            }
	        }

	        return length;
	    }

	    private int buildHillStraight(int xo, int maxLength, Boolean cleanse)
	    {
	        int length = random.nextInt(10) + 10;
	        if (length > maxLength) length = maxLength;
	        int[] xcoordinates = new int[maxLength*height];
	        int[] ycoordinates = new int[maxLength*height];
	        int a = 0;

	        int floor = height - 1 - random.nextInt(4);
	        
	        if(cleanse)
	        {
	        	for(int x = xo; x<xo+length; x++)
	        	{
	        		for(int y=0; y < floor; y++)
	        		{
	        			setBlock(x,y,(byte)0);
	        		}
	        	}
	        }
	        for (int x = xo; x < xo + length; x++)
	        {
	            for (int y = 0; y < height; y++)
	            {
	                if (y >= floor)
	                {
	                    setBlock(x, y, GROUND);
	                    xcoordinates[a] = x;
	                    ycoordinates[a] = y;
	                    a++;
	                }
	            }
	        }

	        addEnemyLine(xo + 1, xo + length - 1, floor - 1);

	        int h = floor;

	        boolean keepGoing = true;

	        boolean[] occupied = new boolean[length];
	        while (keepGoing)
	        {
	            h = h - 2 - random.nextInt(3);

	            if (h <= 2)
	            {
	                keepGoing = false;
	            }
	            else
	            {
	                int l = random.nextInt(5) + 3;
	                int xxo = random.nextInt(length - l - 2) + xo + 1;

	                if (occupied[xxo - xo] || occupied[xxo - xo + l] || occupied[xxo - xo - 1] || occupied[xxo - xo + l + 1])
	                {
	                    keepGoing = false;
	                }
	                else
	                {
	                    occupied[xxo - xo] = true;
	                    occupied[xxo - xo + l] = true;
	                    addEnemyLine(xxo, xxo + l, h - 1);
	                    if (random.nextInt(4) == 0)
	                    {
	                        decorate(xxo - 1, xxo + l + 1, h);
	                        keepGoing = false;
	                    }
	                    for (int x = xxo; x < xxo + l; x++)
	                    {
	                        for (int y = h; y < floor; y++)
	                        {
	                            int xx = 5;
	                            if (x == xxo) xx = 4;
	                            if (x == xxo + l - 1) xx = 6;
	                            int yy = 9;
	                            if (y == h) yy = 8;

	                            if (getBlock(x, y) == 0)
	                            {
	                                setBlock(x, y, (byte) (xx + yy * 16));
	                                xcoordinates[a] = x; 
	                                ycoordinates[a]=y; 
	                                a++;
	                            }
	                            else
	                            {
	                                if (getBlock(x, y) == HILL_TOP_LEFT) {setBlock(x, y, HILL_TOP_LEFT_IN); xcoordinates[a] = x; ycoordinates[a]=y; a++;}
	                                if (getBlock(x, y) == HILL_TOP_RIGHT){setBlock(x, y, HILL_TOP_RIGHT_IN); xcoordinates[a] = x; ycoordinates[a]=y; a++;}
	                            }
	                        }
	                    }
	                }
	            }
	        }

	        return length;
	    }

	    private void addEnemyLine(int x0, int x1, int y)
	    {
	        for (int x = x0; x < x1; x++)
	        {
	        	int monsterChance = 1000;
	        	if (playerCode[0]) monsterChance = 400;
	            if (random.nextInt(monsterChance) < difficulty + 1)
	            {
	                int type = random.nextInt(4);

	                if (difficulty < 1)
	                {
	                    type = Enemy.ENEMY_GOOMBA;
	                }
	                else if (difficulty < 3)
	                {
	                    type = random.nextInt(3);
	                }
	                if(getBlock(x,y) == 0)
	                {
		                setSpriteTemplate(x, y, new SpriteTemplate(type, random.nextInt(35) < difficulty));
		                ENEMIES++;
	                }
	            }
	        }
	    }

	    private int buildTubes(int xo, int maxLength, boolean cleanse)
	    {
	        int[] xcoordinates = new int[maxLength*height];
	        int[] ycoordinates = new int[maxLength*height];
	        int a = 0;
	        int length = random.nextInt(10) + 5;
	        if (length > maxLength) length = maxLength;

	        int floor = height - 1 - random.nextInt(4);
	        
	        if(cleanse)
	        {
	        	for(int x = xo; x < xo+length; x++)
	        	{
	        		for(int y = 0; y< height; y++)
	        		{
	        			setBlock(x,y,(byte)0);
	        		}
	        	}
	        }
	        int xTube = xo + 1 + random.nextInt(4);
	        int tubeHeight = floor - random.nextInt(2) - 2;
	        for (int x = xo; x < xo + length; x++)
	        {
	            if (x > xTube + 1)
	            {
	                xTube += 3 + random.nextInt(4);
	                tubeHeight = floor - random.nextInt(2) - 2;
	            }
	            if (xTube >= xo + length - 2) xTube += 10;

	            if (x == xTube && random.nextInt(11) < difficulty + 1)
	            {
	            	
	                setSpriteTemplate(x, tubeHeight, new SpriteTemplate(Enemy.ENEMY_FLOWER, false));
	                ENEMIES++;
	            }
	            
	            
	            for(int y = 0; y < height; y++)
	            {
	            	if(y>= floor)
	            	{
	            		if(getBlock(x,y)!=0)
	            		{
	            			for(int i= 0; i<xcoordinates.length; i++)
	            			{
	            				setBlock(xcoordinates[i],ycoordinates[i],(byte)0);
	            			}
	            			return 0;
	            		}
	                    setBlock(x, y,GROUND);
	                    xcoordinates[a] = x;
	                    ycoordinates[a] = y;
	                    a++;
	            	}else
	            	{
	            		if((x== xTube || x== xTube +1) && y>=tubeHeight)
	            		{
	            			int xPic = 10 + x -xTube;
	            			if(y== tubeHeight)
	            			{
	            				if(getBlock(x,y)!=0)
	            				{
	    	            			for(int i= 0; i<xcoordinates.length; i++)
	    	            			{
	    	            				setBlock(xcoordinates[i],ycoordinates[i],(byte)0);
	    	            			}
	            					return 0;
	            				}
	            			}else 
	            			{
	            				if(getBlock(x,y)!=0)
	            				{
	    	            			for(int i= 0; i<xcoordinates.length; i++)
	    	            			{
	    	            				setBlock(xcoordinates[i],ycoordinates[i],(byte)0);
	    	            			}
	            					return 0;
	            				}
	            			}
	            		}
	            	}
	            }

	            for (int y = 0; y < height; y++)
	            {
	                if (y >= floor)
	                {
	                    setBlock(x, y,GROUND);
	                    xcoordinates[a] = x;
	                    ycoordinates[a] = y;
	                    a++;
	                }
	                else
	                {
	                    if ((x == xTube || x == xTube + 1) && y >= tubeHeight)
	                    {
	                        int xPic = 10 + x - xTube;

	                        if (y == tubeHeight)
	                        {
	                        	//tube top
	                        	if(getBlock(x,y)==0)
	                        	{
	                        		setBlock(x, y, (byte) (xPic + 0 * 16));
	        	                    xcoordinates[a] = x;
	        	                    ycoordinates[a] = y;
	        	                    a++;
	                        	}else
	                        	{
	    	            			for(int i= 0; i<xcoordinates.length; i++)
	    	            			{
	    	            				setBlock(xcoordinates[i],ycoordinates[i],(byte)0);
	    	            			}
	                        		return 0;
	                        	}
	                        		
	                        }
	                        else
	                        {
	                        	//tube side
	                        	if(getBlock(x,y)==0)
	                        	{
	                        		setBlock(x, y, (byte) (xPic + 1 * 16));
	        	                    xcoordinates[a] = x;
	        	                    ycoordinates[a] = y;
	        	                    a++;
	                        	}else
	                        	{
	    	            			for(int i= 0; i<xcoordinates.length; i++)
	    	            			{
	    	            				setBlock(xcoordinates[i],ycoordinates[i],(byte)0);
	    	            			}
	                        		return 0;
	                        	}
	                        }
	                    }
	                }
	            }
	        }

	        return length;
	    }

	    private int buildStraight(int xo, int maxLength, boolean safe, boolean cleanse)
	    {
	        int length = random.nextInt(10) + 2;

	        if (safe)
	        	length = 10 + random.nextInt(5);
	        	
	        

	        if (length > maxLength)
	        	length = maxLength;

	        int floor = height - 1 - random.nextInt(4);

	        /*
	        for(int x=xo; x<xo+length; x++)
	        {
	        	for(int y = 0; y<floor;y++)
	        	{
	        		setBlock(x,y,(byte)0);
	        	}
	        }
	        */
	        //runs from the specified x position to the length of the segment
	        for (int x = xo; x < xo + length; x++)
	        {
	            for (int y = 0; y < height; y++)
	            {
	                if (y >= floor)
	                {
	                    setBlock(x, y, GROUND);
	                }
	            }
	        }

	        if (!safe)
	        {
	            if (length > 5)
	            {
	                decorate(xo, xo + length, floor);
	            }
	        }

	        return length;
	    }

	    private void decorate(int xStart, int xLength, int floor)
	    {
	    	//if its at the very top, just return
	        if (floor < 1)
	        	return;

	        //        boolean coins = random.nextInt(3) == 0;
	        boolean rocks = true;

	        //add an enemy line above the box
	        addEnemyLine(xStart + 1, xLength - 1, floor - 1);

	        int s = random.nextInt(4);
	        int e = random.nextInt(4);
	        
	        //Coins placed under blocks
	        if (floor - 2 > 0){
	            if ((xLength - 1 - e) - (xStart + 1 + s) > 1){
	                for(int x = xStart + 1 + s; x < xLength - 1 - e; x++){
	                	if(getBlock(x-1,floor-2)!=0)
	                		WASTE++;
	                	
	                	Boolean tooHigh = true;
	                	for(int i = 0; i < 3; i++)
	                	{
	                		if(getBlock(x, floor-3-i)!=COIN && getBlock(x,floor-3-i)!=0)
	                		{
	                			tooHigh = false;
	                			break;
	                		}
	                	}
	                	
	                	/*
	                	if(!tooHigh)
	                	{
	                	*/
	                		setBlock(x, floor - 2, COIN);
	                		//System.out.print("shot");
	                		COINS++;
	                	//}
	                }
	            }
	        }

	        s = random.nextInt(4);
	        e = random.nextInt(4);
	        
	        //this fills the set of blocks and the hidden objects inside them
	        if (floor - 4 > 4)
	        {
	            if ((xLength - 1 - e) - (xStart + 1 + s) > 2)
	            {
	                for (int x = xStart + 1 + s; x < xLength - 1 - e; x++)
	                {
	                    if (rocks)
	                    {
	                    	// if s is greater than 0 && x+xStart+1 < xLenghth - 2 && 1/3 chance
                        	int chance1 = 2187; // too low
                        	for(int i = floor-3; i< height; i++)
                        	{
                        		if(getBlock(x,i)== 0 || getBlock(x,i) == COIN)
                        		{
                        			chance1 = chance1/3;
                        		}
                        	}
                        	if(chance1<=0)
                        	{
                        		chance1 = 1;
                        	}
                        	int chance2 = 128;
                        	for(int i = floor-3; i< height; i++)
                        	{
                        		if(getBlock(x,i)== 0|| getBlock(x,i) == COIN)
                        		{
                        			chance2 = chance2/2;
                        		}
                        	}
                        	if (chance2<=0)
                        		chance2 = 1;
                        	//System.out.println("chance1: "+chance1);
	                        if (x != xStart + 1 && x != xLength - 2 && random.nextInt(chance1) == 0)
	                        {
	                        	// 1/4 chance (1/12 overall)
	                        	int chance = 128;
	                        	for(int i = floor-3; i< height; i++)
	                        	{
	                        		if(getBlock(x,i)== 0|| getBlock(x,i) == COIN)
	                        		{
	                        			chance = chance/2;
	                        		}
	                        	}
	                        	if(chance<=0)
	                        	{
	                        		chance =1;
	                        	}
	                            if (random.nextInt(4) == 0)
	                            {

	                            	if(floor < height-1)
	                            	{
		                                setBlock(x, floor - 4, BLOCK_POWERUP);
		                                
		                                // if the space below is empty then it counts
		                                if(getBlock(x,floor-3) == 0 && (floor-5>0 &&  getBlock(x,floor-5)==0))
		                                {
		                                	BLOCKS_POWER++;
		                                }else WASTE++;
		                                
		                                //don't box up coins
		                                if(getBlock(x-1,floor-4) == COIN || getBlock(x,floor-5) == COIN|| getBlock(x,floor-3)== COIN)
		                                {
		                                	WASTE++;
		                                }
		                                
		                                //don't make unjumpable blocks
		                                int c = -2;
		                                while(floor+c < height)
		                                {
		                                	if(getBlock(x-1,floor+c)==0)
		                                	{
		                                		WASTE++;
		                                	}else break;
		                                	c++;
		                                }
		                                
		                                // put spaces in between barrier blocks
		                                if(getBlock(x,floor-2) !=0 || getBlock(x,floor-3) != 0)
		                                {
		                                	WASTE++;
		                                }
		                                
		                                //Don't build the blocks too high
		                                if(floor <9)
		                                {
		                                	int wa=9 - floor;
		                                	if (wa>0)
		                                		WASTE+= wa;
		                                }
	                               
	                            	}
	                            	
	                            }
	                            else// 3/4 chance (1/4 overall)
	                            {	//the fills a block with a hidden coin
	        	                	Boolean tooHigh = true;
	        	                	for(int i = 0; i < 3; i++)
	        	                	{
	        	                		if(getBlock(x-1, floor-3-i)!=COIN && getBlock(x-1,floor-3-i)!=0 && getBlock(x-2, floor-3-i)!=COIN && getBlock(x-2,floor-3-i)!=0)
	        	                		{
	        	                			tooHigh = false;
	        	                			break;
	        	                		}
	        	                	}
	                                if(floor < height-1 && !tooHigh)
	                                {
		                                setBlock(x, floor - 4, BLOCK_COIN);
		                               
		                                // if the space below is empty then it counts
		                                if(getBlock(x,floor-3) == 0)
		                                {
		                                	BLOCKS_COINS++;
		                                }else WASTE++;
		                                
		                                //don't box up coins
		                                if(getBlock(x-1,floor-4) == COIN || getBlock(x,floor-5) == COIN|| getBlock(x,floor-3)== COIN)
		                                {
		                                	WASTE++;
		                                }
		                                
		                                // don't make unjumpable block jumps
		                                int c = -2;
		                                while(floor+c < height)
		                                {
		                                	if(getBlock(x-1,floor+c)==0)
		                                	{
		                                		WASTE++;
		                                	}else break;
		                                	c++;
		                                }
		                                
		                                // put spaces in between barrier blocks
		                                if(getBlock(x,floor-2) !=0 || getBlock(x,floor-3) != 0)
		                                {
		                                	WASTE++;
		                                }
		                                
		                                //Don't build the blocks too high
		                                if(floor <9)
		                                {
		                                	int wa=9 - floor;
		                                	if (wa>0)
		                                		WASTE+= wa;
		                                }
	                                }else 
	                                {
	                                }
	                            }
	                        }
	                        else if (random.nextInt(chance2) == 0)// (1/6 chance) // too small
	                        {
	                            if (random.nextInt(chance2) == 0)// (1/24 chance)
	                            {
	        	                	Boolean tooHigh = true;
	        	                	for(int i = 0; i < 3; i++)
	        	                	{
	        	                		if(getBlock(x-1, floor-3-i)!=COIN && getBlock(x-1,floor-3-i)!=0 && getBlock(x-2, floor-3-i)!=COIN && getBlock(x-2,floor-3-i)!=0)
	        	                		{
	        	                			tooHigh = false;
	        	                			break;
	        	                		}
	        	                	}
	                            	// Random AF block, could be anything
	                            	if(floor < height-1 && !tooHigh)
	                            		setBlock(x, floor - 4, (byte) (2 + 1 * 16));
	                            	
	                            	// don't box up coins
	                                if(getBlock(x-1,floor-4) == COIN || getBlock(x,floor-5) == COIN|| getBlock(x,floor-3)== COIN)
	                                {
	                                	WASTE++;
	                                }
	                                
	                                //don't make unjumpable blocks 
	                                int c = -2;
	                                while(floor+c < height)
	                                {
	                                	if(getBlock(x-1,floor+c)==0)
	                                	{
	                                		WASTE++;
	                                	}else break;
	                                	c++;
	                                }
	                                
	                                // put spaces in between barrier blocks
	                                if(getBlock(x,floor-2) !=0 || getBlock(x,floor-3) != 0)
	                                {
	                                	WASTE++;
	                                }
	                                
	                                //Don't build the blocks too high
	                                if(floor <9)
	                                {
	                                	int wa=9 - floor;
	                                	if (wa>0)
	                                		WASTE+= wa;
	                                }
	                            }
	                            else // (1/8 chance)
	                            {
	        	                	Boolean tooHigh = true;
	        	                	for(int i = 0; i < 3; i++)
	        	                	{
	        	                		if(getBlock(x-1, floor-3-i)!=COIN && getBlock(x-1,floor-3-i)!=0 && getBlock(x-2, floor-3-i)!=COIN && getBlock(x-2,floor-3-i)!=0)
	        	                		{
	        	                			tooHigh = false;
	        	                			break;
	        	                		}
	        	                	}
	                            	// Random AF block, could be anything
	                            	if(floor < height -1 || !tooHigh)
	                            		setBlock(x, floor - 4, (byte) (1 + 1 * 16));
	                            	
	                            	// don't box up coins
	                                if(getBlock(x-1,floor-4) == COIN || getBlock(x,floor-5) == COIN|| getBlock(x,floor-3)== COIN)
	                                {
	                                	WASTE++;
	                                }
	                                
	                                //don't make unjumpable blocks
	                                int c = -2;
	                                while(floor+c < height)
	                                {
	                                	if(getBlock(x-1,floor+c)==0)
	                                	{
	                                		WASTE++;
	                                	}else break;
	                                	c++;
	                                }
	                                
	                                // put spaces in between barrier blocks
	                                if(getBlock(x,floor-2) !=0 || getBlock(x,floor-3) != 0)
	                                {
	                                	WASTE++;
	                                }
	                                
	                                //Don't build the blocks too high
	                                if(floor <9)
	                                {
	                                	int wa=9 - floor;
	                                	if (wa>0)
	                                		WASTE+= wa;
	                                }
	                            }
	                        }
	                        else if(chance2 ==1)
	                        {
        	                	Boolean tooHigh = true;
        	                	for(int i = 0; i < 3; i++)
        	                	{
        	                		if(getBlock(x-1, floor-3-i)!=COIN && getBlock(x-1,floor-3-i)!=0 && getBlock(x-2, floor-3-i)!=COIN && getBlock(x-2,floor-3-i)!=0)
        	                		{
        	                			tooHigh = false;
        	                			break;
        	                		}
        	                	}
        	                	if(!tooHigh)
        	                	{
        	                		setBlock(x, floor - 4, BLOCK_EMPTY);
        	                	}
                                
	                            // if the space below is empty then it counts
                                if(getBlock(x,floor-3) == 0)
                                {
                                	BLOCKS_EMPTY++;
                                }else WASTE++;
                                
                                //don't box up coins
                                if(getBlock(x-1,floor-4) == COIN || getBlock(x,floor-5) == COIN || getBlock(x,floor-3)== COIN)
                                {
                                	WASTE++;
                                }
                                
                                //don't make unjumpable blocks
                                int c = -2;
                                while(floor+c < height)
                                {
                                	if(getBlock(x-1,floor+c)==0)
                                	{
                                		WASTE++;
                                	}else break;
                                	c++;
                                }
                                
                                // put spaces in between barrier blocks
                                if(getBlock(x,floor-2) !=0 || getBlock(x,floor-3) != 0)
                                {
                                	WASTE++;
                                }
                                
                                //Don't build the blocks too high
                                if(floor <9)
                                {
                                	int wa=9 - floor;
                                	if (wa>0)
                                		WASTE+= wa;
                                }
	                        }
	                    }
	                }
	            }
	        }
	    }

	    private void fixWalls()
	    {
	        boolean[][] blockMap = new boolean[width + 1][height + 1];

	        for (int x = 0; x < width + 1; x++)
	        {
	            for (int y = 0; y < height + 1; y++)
	            {
	                int blocks = 0;
	                for (int xx = x - 1; xx < x + 1; xx++)
	                {
	                    for (int yy = y - 1; yy < y + 1; yy++)
	                    {
	                        if (getBlockCapped(xx, yy) == GROUND){
	                        	blocks++;
	                        }
	                    }
	                }
	                blockMap[x][y] = blocks == 4;
	            }
	        }
	        blockify(this, blockMap, width + 1, height + 1);
	    }

	    private void blockify(Level level, boolean[][] blocks, int width, int height){
	        int to = 0;
	        if (type == LevelInterface.TYPE_CASTLE)
	        {
	            to = 4 * 2;
	        }
	        else if (type == LevelInterface.TYPE_UNDERGROUND)
	        {
	            to = 4 * 3;
	        }

	        boolean[][] b = new boolean[2][2];

	        for (int x = 0; x < width; x++)
	        {
	            for (int y = 0; y < height; y++)
	            {
	                for (int xx = x; xx <= x + 1; xx++)
	                {
	                    for (int yy = y; yy <= y + 1; yy++)
	                    {
	                        int _xx = xx;
	                        int _yy = yy;
	                        if (_xx < 0) _xx = 0;
	                        if (_yy < 0) _yy = 0;
	                        if (_xx > width - 1) _xx = width - 1;
	                        if (_yy > height - 1) _yy = height - 1;
	                        b[xx - x][yy - y] = blocks[_xx][_yy];
	                    }
	                }

	                if (b[0][0] == b[1][0] && b[0][1] == b[1][1])
	                {
	                    if (b[0][0] == b[0][1])
	                    {
	                        if (b[0][0])
	                        {
	                            level.setBlock(x, y, (byte) (1 + 9 * 16 + to));
	                        }
	                        else
	                        {
	                            // KEEP OLD BLOCK!
	                        }
	                    }
	                    else
	                    {
	                        if (b[0][0])
	                        {
	                        	//down grass top?
	                            level.setBlock(x, y, (byte) (1 + 10 * 16 + to));
	                        }
	                        else
	                        {
	                        	//up grass top
	                            level.setBlock(x, y, (byte) (1 + 8 * 16 + to));
	                        }
	                    }
	                }
	                else if (b[0][0] == b[0][1] && b[1][0] == b[1][1])
	                {
	                    if (b[0][0])
	                    {
	                    	//right grass top
	                        level.setBlock(x, y, (byte) (2 + 9 * 16 + to));
	                    }
	                    else
	                    {
	                    	//left grass top
	                        level.setBlock(x, y, (byte) (0 + 9 * 16 + to));
	                    }
	                }
	                else if (b[0][0] == b[1][1] && b[0][1] == b[1][0])
	                {
	                    level.setBlock(x, y, (byte) (1 + 9 * 16 + to));
	                }
	                else if (b[0][0] == b[1][0])
	                {
	                    if (b[0][0])
	                    {
	                        if (b[0][1])
	                        {
	                            level.setBlock(x, y, (byte) (3 + 10 * 16 + to));
	                        }
	                        else
	                        {
	                            level.setBlock(x, y, (byte) (3 + 11 * 16 + to));
	                        }
	                    }
	                    else
	                    {
	                        if (b[0][1])
	                        {
	                        	//right up grass top
	                            level.setBlock(x, y, (byte) (2 + 8 * 16 + to));
	                        }
	                        else
	                        {
	                        	//left up grass top
	                            level.setBlock(x, y, (byte) (0 + 8 * 16 + to));
	                        }
	                    }
	                }
	                else if (b[0][1] == b[1][1])
	                {
	                    if (b[0][1])
	                    {
	                        if (b[0][0])
	                        {
	                        	//left pocket grass
	                            level.setBlock(x, y, (byte) (3 + 9 * 16 + to));
	                        }
	                        else
	                        {
	                        	//right pocket grass
	                            level.setBlock(x, y, (byte) (3 + 8 * 16 + to));
	                        }
	                    }
	                    else
	                    {
	                        if (b[0][0])
	                        {
	                            level.setBlock(x, y, (byte) (2 + 10 * 16 + to));
	                        }
	                        else
	                        {
	                            level.setBlock(x, y, (byte) (0 + 10 * 16 + to));
	                        }
	                    }
	                }
	                else
	                {
	                    level.setBlock(x, y, (byte) (0 + 1 * 16 + to));
	                }
	            }
	        }
	    }
	    
	    public RandomLevel clone() throws CloneNotSupportedException {

	    	RandomLevel clone=new RandomLevel(width, height);

	    	clone.xExit = xExit;
	    	clone.yExit = yExit;
	    	byte[][] map = getMap();
	    	SpriteTemplate[][] st = getSpriteTemplate();
	    	
	    	for (int i = 0; i < map.length; i++)
	    		for (int j = 0; j < map[i].length; j++) {
	    			clone.setBlock(i, j, map[i][j]);
	    			clone.setSpriteTemplate(i, j, st[i][j]);
	    	}
	    	clone.BLOCKS_COINS = BLOCKS_COINS;
	    	clone.BLOCKS_EMPTY = BLOCKS_EMPTY;
	    	clone.BLOCKS_POWER = BLOCKS_POWER;
	    	clone.ENEMIES = ENEMIES;
	    	clone.COINS = COINS;
	    	
	        return clone;

	      }


}
