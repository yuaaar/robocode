package yuaaar;
import robocode.*;
import robocode.util.Utils;
import java.util.LinkedHashMap;
import java.awt.Color;
import java.awt.Graphics2D;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * DogeNose - a radar testbed robot by yuaaar
 */
public class DogeNose extends AdvancedRobot
{
    static final double BASE_MOVEMENT       = 180;
    static final double GUN_FACTOR          = 500;
    static final double BASE_TURN           = Math.PI/2;
    static final double BASE_CANNON_POWER   = 20;
        
    // Globals
    static double   movement;
    static String   lastTarget;
    static double   lastDistance;
	
    static LinkedHashMap<String, Double> ehm;
    static double scanDir;
	static double radTurn;
    static Object lastScanned;
    //for graphical debugging
    static int      eX;
    static int      eY;
    /**
     * run: DogeNose's default behavior
     */
    public void run() {
        // Initialization of the robot should be put here

        setColors(Color.black,Color.black,Color.white);

        scanDir = 1;
        radTurn = movement = lastDistance = Double.POSITIVE_INFINITY;
        ehm = new LinkedHashMap<String, Double>(5, 2, true);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);	

        do {
			setTurnRadarRightRadians(radTurn);
			scan();	
		} while(true);
    }

    /**
     * onScannedRobot: What to do when you see another robot
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        double distance = e.getDistance();
        double absoluteBearing = e.getBearingRadians() + getHeadingRadians();
        String name = e.getName();
		ehm.put(name, absoluteBearing);
        //box movement
        if (getDistanceRemaining() == 0) {
            setAhead(movement = -movement);
            setTurnRightRadians(BASE_TURN);
        }

        //lock on to new target if someone got closer
        if (lastDistance > distance + 36) {
            lastDistance = distance;
            lastTarget = name;
        }

		if ((name == lastScanned || lastScanned == null) && ehm.size() == getOthers()) {
            scanDir = Utils.normalRelativeAngle(ehm.values().iterator().next() - getRadarHeadingRadians());
            lastScanned = ehm.keySet().iterator().next();
		}        
        //Lock/Fire if gun is somewhat cool and we're pointed at selected target
        if (getGunHeat() < 1 && (lastTarget == name && distance < GUN_FACTOR)) {
            if(getGunHeat() == getGunTurnRemaining()) {
                setFireBullet(getOthers() * getEnergy() * BASE_CANNON_POWER / distance);
                lastDistance = Double.POSITIVE_INFINITY;
            }

            //radar section
            radTurn = 3.5 * Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
            
            //for some debugging
            eX = (int)(getX() + Math.sin(absoluteBearing) * e.getDistance());
            eY = (int)(getY() + Math.cos(absoluteBearing) * e.getDistance());
            //gun section
            setTurnGunRightRadians(Math.asin(Math.sin(absoluteBearing - getGunHeadingRadians() + 
                (1 - e.getDistance() / GUN_FACTOR) * 
                Math.asin(e.getVelocity() / 11) * Math.sin(e.getHeadingRadians() - absoluteBearing) )));
        } else {
			radTurn = scanDir * Double.POSITIVE_INFINITY;
		}
    }
    
    /**
     * onHitWall: What to do when you hit a wall
     */
    public void onHitWall(HitWallEvent e) {
        if(Math.abs(movement) > BASE_MOVEMENT) {
            movement = BASE_MOVEMENT;
        }
    }
    
    /**
    * onRobotDeath: aim at the next guy
    */
    public void onRobotDeath(RobotDeathEvent e) {
        ehm.remove(e.getName());
        lastDistance = Double.POSITIVE_INFINITY;
		lastScanned = null;
    }

    public void onPaint(Graphics2D g) {
        g.setColor(new Color(0x00, 0xff, 0xff, 0x80));
        //target location
        g.fillRect(eX - 20, eY - 20, 40, 40);
        //aim location
        int x = (int)getX();
        int y = (int)getY();
        g.drawLine(x, y, x + (int)(lastDistance * Math.sin(getGunTurnRemaining() + getGunHeadingRadians())),
            y + (int)(lastDistance * Math.cos(getGunTurnRemaining() + getGunHeadingRadians())));

    }
}
